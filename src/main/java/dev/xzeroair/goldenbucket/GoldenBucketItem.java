package dev.xzeroair.goldenbucket;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GoldenBucketItem extends Item {
    public static final int CAPACITY = 3 * FluidType.BUCKET_VOLUME;

    public GoldenBucketItem() {
        super(new Properties().stacksTo(1));
    }

    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new GoldenBucketFluidHandler(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        GoldenBucketFluidHandler fluidHandler = new GoldenBucketFluidHandler(stack);
        if (GoldenBucketConfig.isMilk(fluidHandler.getFluid().getFluid()) && !player.isShiftKeyDown()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        HitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }

        BlockHitResult blockHitResult = (BlockHitResult) hitResult;
        BlockPos blockPos = blockHitResult.getBlockPos();
        Direction direction = blockHitResult.getDirection();
        BlockPos relativePos = blockPos.relative(direction);

        if (!level.mayInteract(player, blockPos) || !player.mayUseItemAt(relativePos, direction, stack)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!player.isShiftKeyDown() && tryPickupFluid(level, player, fluidHandler, blockPos)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        if (tryPlaceFluid(level, player, fluidHandler, blockHitResult)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        return InteractionResultHolder.fail(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Cow) || target.isBaby()) {
            return InteractionResult.PASS;
        }

        GoldenBucketFluidHandler fluidHandler = new GoldenBucketFluidHandler(stack);
        Fluid milk = GoldenBucketConfig.milkFluid();
        if (milk == null) {
            return InteractionResult.PASS;
        }

        FluidStack resource = new FluidStack(milk, FluidType.BUCKET_VOLUME);
        if (!GoldenBucketConfig.isFluidAllowed(milk) || fluidHandler.fill(resource, FluidAction.SIMULATE) != FluidType.BUCKET_VOLUME) {
            return InteractionResult.PASS;
        }

        if (!player.level().isClientSide()) {
            fluidHandler.fill(resource, FluidAction.EXECUTE);
        }

        playSound(player.level(), player, target.blockPosition(), SoundEvents.COW_MILK);
        return InteractionResult.sidedSuccess(player.level().isClientSide());
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return GoldenBucketConfig.isMilk(getFluid(stack).getFluid()) ? 32 : 0;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return GoldenBucketConfig.isMilk(getFluid(stack).getFluid()) ? UseAnim.DRINK : UseAnim.NONE;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (GoldenBucketConfig.isMilk(getFluid(stack).getFluid())) {
            entity.removeAllEffects();
            if (!level.isClientSide() && (!(entity instanceof Player player) || !player.getAbilities().instabuild)) {
                new GoldenBucketFluidHandler(stack).drain(FluidType.BUCKET_VOLUME, FluidAction.EXECUTE);
            }
        }

        return stack;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getFluid(stack).getAmount() > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getFluid(stack).getAmount() / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        Fluid fluid = getFluid(stack).getFluid();
        if (fluid == Fluids.LAVA) {
            return 0xFF6A00;
        }
        if (GoldenBucketConfig.isMilk(fluid)) {
            return 0xFFFFFF;
        }

        return 0x3F76E4;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FluidStack fluid = getFluid(stack);
        tooltip.add(Component.translatable("item.goldenbucket.golden_bucket.tooltip", getFluidName(fluid), fluid.getAmount(), CAPACITY)
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.goldenbucket.golden_bucket.sneak_tooltip")
                .withStyle(ChatFormatting.GRAY));
    }

    private boolean tryPickupFluid(Level level, Player player, GoldenBucketFluidHandler fluidHandler, BlockPos blockPos) {
        BlockState state = level.getBlockState(blockPos);
        Fluid fluid = state.getFluidState().getType();
        if (!(state.getBlock() instanceof BucketPickup bucketPickup)) {
            return false;
        }

        if (!state.getFluidState().isSource() || fluid == Fluids.EMPTY || !GoldenBucketConfig.isFluidAllowed(fluid)) {
            return false;
        }

        FluidStack resource = new FluidStack(fluid, FluidType.BUCKET_VOLUME);
        if (fluidHandler.fill(resource, FluidAction.SIMULATE) != FluidType.BUCKET_VOLUME) {
            return false;
        }

        ItemStack pickedUp = bucketPickup.pickupBlock(player, level, blockPos, state);
        if (pickedUp.isEmpty()) {
            return false;
        }

        fluidHandler.fill(resource, FluidAction.EXECUTE);
        playSound(level, player, blockPos, getPickupSound(resource));
        level.gameEvent(player, GameEvent.FLUID_PICKUP, blockPos);
        return true;
    }

    private boolean tryPlaceFluid(Level level, Player player, GoldenBucketFluidHandler fluidHandler, BlockHitResult hitResult) {
        BlockPos blockPos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(blockPos);
        BlockPos targetPos = state.canBeReplaced() ? blockPos : blockPos.relative(hitResult.getDirection());
        FluidStack drained = fluidHandler.drain(FluidType.BUCKET_VOLUME, FluidAction.SIMULATE);

        if (!player.mayUseItemAt(targetPos, hitResult.getDirection(), fluidHandler.getContainer())) {
            return false;
        }

        if (GoldenBucketConfig.isMilk(drained.getFluid())) {
            fluidHandler.drain(FluidType.BUCKET_VOLUME, FluidAction.EXECUTE);
            playSound(level, player, targetPos, getEmptySound(drained));
            level.gameEvent(player, GameEvent.FLUID_PLACE, targetPos);
            return true;
        }

        if (drained.getAmount() != FluidType.BUCKET_VOLUME || !placeFluid(level, player, targetPos, drained)) {
            return false;
        }

        fluidHandler.drain(FluidType.BUCKET_VOLUME, FluidAction.EXECUTE);
        return true;
    }

    private boolean placeFluid(Level level, @Nullable Player player, BlockPos blockPos, FluidStack fluidStack) {
        Fluid fluid = fluidStack.getFluid();
        if (fluid.getFluidType().isVaporizedOnPlacement(level, blockPos, fluidStack)) {
            fluid.getFluidType().onVaporize(player, level, blockPos, fluidStack);
            return true;
        }

        if (!fluid.getFluidType().canBePlacedInLevel(level, blockPos, fluidStack)) {
            return false;
        }

        BlockState state = level.getBlockState(blockPos);
        boolean canReplace = state.canBeReplaced(fluid);
        boolean canContain = state.getBlock() instanceof LiquidBlockContainer liquidContainer
                && liquidContainer.canPlaceLiquid(player, level, blockPos, state, fluid);

        if (!state.isAir() && !canReplace && !canContain) {
            return false;
        }

        if (!level.isClientSide()) {
            if (canContain) {
                ((LiquidBlockContainer) state.getBlock()).placeLiquid(level, blockPos, state, fluid.defaultFluidState());
            } else {
                if (canReplace && !state.liquid()) {
                    level.destroyBlock(blockPos, true);
                }

                level.setBlock(blockPos, fluid.defaultFluidState().createLegacyBlock(), 11);
            }
        }

        playSound(level, player, blockPos, getEmptySound(fluidStack));
        level.gameEvent(player, GameEvent.FLUID_PLACE, blockPos);
        return true;
    }

    public static FluidStack getFluid(ItemStack stack) {
        return new GoldenBucketFluidHandler(stack).getFluid();
    }

    private Component getFluidName(FluidStack fluid) {
        if (fluid.isEmpty()) {
            return Component.translatable("item.goldenbucket.empty");
        }

        return fluid.getDisplayName();
    }

    private SoundEvent getPickupSound(FluidStack fluid) {
        SoundEvent sound = fluid.getFluid().getFluidType().getSound(fluid, SoundActions.BUCKET_FILL);
        if (sound != null) {
            return sound;
        }

        if (fluid.getFluid() == Fluids.LAVA) {
            return SoundEvents.BUCKET_FILL_LAVA;
        }

        return SoundEvents.BUCKET_FILL;
    }

    private SoundEvent getEmptySound(FluidStack fluid) {
        SoundEvent sound = fluid.getFluid().getFluidType().getSound(fluid, SoundActions.BUCKET_EMPTY);
        if (sound != null) {
            return sound;
        }

        if (fluid.getFluid() == Fluids.LAVA) {
            return SoundEvents.BUCKET_EMPTY_LAVA;
        }

        return SoundEvents.BUCKET_EMPTY;
    }

    private void playSound(Level level, @Nullable Player player, BlockPos pos, SoundEvent sound) {
        level.playSound(player, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static class GoldenBucketFluidHandler implements IFluidHandlerItem, ICapabilityProvider {
        private static final String FLUID_NBT_KEY = "Fluid";

        private final LazyOptional<IFluidHandlerItem> holder = LazyOptional.of(() -> this);
        private final ItemStack container;

        private GoldenBucketFluidHandler(ItemStack container) {
            this.container = container;
        }

        @Override
        public @NotNull ItemStack getContainer() {
            return container;
        }

        public @NotNull FluidStack getFluid() {
            CustomData customData = container.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            if (!customData.contains(FLUID_NBT_KEY)) {
                return FluidStack.EMPTY;
            }
            return FluidStack.loadFluidStackFromNBT(customData.copyTag().getCompound(FLUID_NBT_KEY));
        }

        private void setFluid(FluidStack fluid) {
            CustomData.update(DataComponents.CUSTOM_DATA, container, tag -> {
                if (fluid.isEmpty()) {
                    tag.remove(FLUID_NBT_KEY);
                    return;
                }

                CompoundTag fluidTag = new CompoundTag();
                fluid.writeToNBT(fluidTag);
                tag.put(FLUID_NBT_KEY, fluidTag);
            });
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return canFillFluidType(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (container.getCount() != 1 || resource.isEmpty() || !canFillFluidType(resource)) {
                return 0;
            }

            FluidStack contained = getFluid();
            if (contained.isEmpty()) {
                int fillAmount = Math.min(CAPACITY, resource.getAmount());
                if (action.execute()) {
                    FluidStack filled = resource.copy();
                    filled.setAmount(fillAmount);
                    setFluid(filled);
                }
                return fillAmount;
            }

            if (!contained.isFluidEqual(resource)) {
                return 0;
            }

            int fillAmount = Math.min(CAPACITY - contained.getAmount(), resource.getAmount());
            if (action.execute() && fillAmount > 0) {
                contained.grow(fillAmount);
                setFluid(contained);
            }
            return fillAmount;
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            if (container.getCount() != 1 || resource.isEmpty() || !resource.isFluidEqual(getFluid())) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            if (container.getCount() != 1 || maxDrain <= 0) {
                return FluidStack.EMPTY;
            }

            FluidStack contained = getFluid();
            if (contained.isEmpty() || !canDrainFluidType(contained)) {
                return FluidStack.EMPTY;
            }

            int drainAmount = Math.min(contained.getAmount(), maxDrain);
            FluidStack drained = contained.copy();
            drained.setAmount(drainAmount);

            if (action.execute()) {
                contained.shrink(drainAmount);
                setFluid(contained);
            }

            return drained;
        }

        public boolean canFillFluidType(FluidStack fluid) {
            return !fluid.isEmpty() && GoldenBucketConfig.isFluidAllowed(fluid.getFluid());
        }

        public boolean canDrainFluidType(FluidStack fluid) {
            return !fluid.isEmpty();
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction facing) {
            return ForgeCapabilities.FLUID_HANDLER_ITEM.orEmpty(capability, holder);
        }
    }
}

