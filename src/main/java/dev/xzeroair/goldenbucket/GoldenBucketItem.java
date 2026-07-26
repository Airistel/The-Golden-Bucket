package dev.xzeroair.goldenbucket;

import net.minecraft.block.BlockState;
import net.minecraft.block.IBucketPickupHandler;
import net.minecraft.block.ILiquidContainer;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;

import javax.annotation.Nullable;
import java.util.List;

public class GoldenBucketItem extends Item {
    public static final int CAPACITY = 3 * FluidAttributes.BUCKET_VOLUME;

    public GoldenBucketItem() {
        super(GoldenBucketMod.itemProperties());
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundNBT nbt) {
        return new GoldenBucketFluidHandler(stack);
    }

    @Override
    public ActionResult<ItemStack> use(World level, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        GoldenBucketFluidHandler fluidHandler = new GoldenBucketFluidHandler(stack);
        if (GoldenBucketConfig.isMilk(fluidHandler.getFluid().getFluid()) && !player.isShiftKeyDown()) {
            player.startUsingItem(hand);
            return ActionResult.sidedSuccess(stack, level.isClientSide);
        }

        RayTraceResult hitResult = getPlayerPOVHitResult(level, player, RayTraceContext.FluidMode.SOURCE_ONLY);

        if (hitResult.getType() != RayTraceResult.Type.BLOCK) {
            return ActionResult.pass(stack);
        }

        BlockRayTraceResult blockHitResult = (BlockRayTraceResult) hitResult;
        BlockPos blockPos = blockHitResult.getBlockPos();
        Direction direction = blockHitResult.getDirection();
        BlockPos relativePos = blockPos.relative(direction);

        if (!level.mayInteract(player, blockPos) || !player.mayUseItemAt(relativePos, direction, stack)) {
            return ActionResult.fail(stack);
        }

        if (!player.isShiftKeyDown() && tryPickupFluid(level, player, fluidHandler, blockPos)) {
            return ActionResult.sidedSuccess(stack, level.isClientSide);
        }

        if (tryPlaceFluid(level, player, fluidHandler, blockHitResult)) {
            return ActionResult.sidedSuccess(stack, level.isClientSide);
        }

        return ActionResult.fail(stack);
    }

    @Override
    public ActionResultType interactLivingEntity(ItemStack stack, PlayerEntity player, LivingEntity target, Hand hand) {
        if (!(target instanceof CowEntity) || target.isBaby()) {
            return ActionResultType.PASS;
        }

        GoldenBucketFluidHandler fluidHandler = new GoldenBucketFluidHandler(stack);
        Fluid milk = GoldenBucketConfig.milkFluid();
        if (milk == null) {
            return ActionResultType.PASS;
        }

        FluidStack resource = new FluidStack(milk, FluidAttributes.BUCKET_VOLUME);
        if (!GoldenBucketConfig.isFluidAllowed(milk) || fluidHandler.fill(resource, FluidAction.SIMULATE) != FluidAttributes.BUCKET_VOLUME) {
            return ActionResultType.PASS;
        }

        if (!player.level.isClientSide) {
            fluidHandler.fill(resource, FluidAction.EXECUTE);
        }

        playSound(player.level, player, target.blockPosition(), SoundEvents.COW_MILK);
        return ActionResultType.sidedSuccess(player.level.isClientSide);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return GoldenBucketConfig.isMilk(getFluid(stack).getFluid()) ? 32 : 0;
    }

    @Override
    public UseAction getUseAnimation(ItemStack stack) {
        return GoldenBucketConfig.isMilk(getFluid(stack).getFluid()) ? UseAction.DRINK : UseAction.NONE;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, World level, LivingEntity entity) {
        if (GoldenBucketConfig.isMilk(getFluid(stack).getFluid())) {
            entity.removeAllEffects();
            if (!level.isClientSide && (!(entity instanceof PlayerEntity) || !((PlayerEntity) entity).abilities.instabuild)) {
                new GoldenBucketFluidHandler(stack).drain(FluidAttributes.BUCKET_VOLUME, FluidAction.EXECUTE);
            }
        }

        return stack;
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return getFluid(stack).getAmount() > 0;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1.0D - (double) getFluid(stack).getAmount() / CAPACITY;
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
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
    public void appendHoverText(ItemStack stack, @Nullable World level, List<ITextComponent> tooltip, ITooltipFlag flag) {
        FluidStack fluid = getFluid(stack);
        tooltip.add(new TranslationTextComponent("item.goldenbucket.golden_bucket.tooltip", getFluidName(fluid), fluid.getAmount(), CAPACITY).withStyle(TextFormatting.AQUA));
        tooltip.add(new TranslationTextComponent("item.goldenbucket.golden_bucket.sneak_tooltip").withStyle(TextFormatting.GRAY));
    }

    private boolean tryPickupFluid(World level, PlayerEntity player, GoldenBucketFluidHandler fluidHandler, BlockPos blockPos) {
        BlockState state = level.getBlockState(blockPos);
        Fluid fluid = state.getFluidState().getType();
        if (!(state.getBlock() instanceof IBucketPickupHandler) || !state.getFluidState().isSource() || !isAllowedFluid(fluid)) {
            return false;
        }

        FluidStack resource = new FluidStack(fluid, FluidAttributes.BUCKET_VOLUME);
        if (fluidHandler.fill(resource, FluidAction.SIMULATE) != FluidAttributes.BUCKET_VOLUME) {
            return false;
        }

        if (((IBucketPickupHandler) state.getBlock()).takeLiquid(level, blockPos, state) == Fluids.EMPTY) {
            return false;
        }

        fluidHandler.fill(resource, FluidAction.EXECUTE);
        playSound(level, player, blockPos, fluid == Fluids.LAVA ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL);
        return true;
    }

    private boolean tryPlaceFluid(World level, PlayerEntity player, GoldenBucketFluidHandler fluidHandler, BlockRayTraceResult hitResult) {
        BlockPos blockPos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(blockPos);
        BlockPos targetPos = state.getMaterial().isReplaceable() ? blockPos : blockPos.relative(hitResult.getDirection());
        FluidStack drained = fluidHandler.drain(FluidAttributes.BUCKET_VOLUME, FluidAction.SIMULATE);

        if (drained.getAmount() != FluidAttributes.BUCKET_VOLUME || !player.mayUseItemAt(targetPos, hitResult.getDirection(), fluidHandler.getContainer())) {
            return false;
        }

        if (GoldenBucketConfig.isMilk(drained.getFluid())) {
            if (!level.isClientSide) {
                fluidHandler.drain(FluidAttributes.BUCKET_VOLUME, FluidAction.EXECUTE);
            }
            playSound(level, player, targetPos, SoundEvents.BUCKET_EMPTY);
            return true;
        }

        Fluid fluid = drained.getFluid();
        BlockState targetState = level.getBlockState(targetPos);
        boolean canContain = targetState.getBlock() instanceof ILiquidContainer
                && ((ILiquidContainer) targetState.getBlock()).canPlaceLiquid(level, targetPos, targetState, fluid);

        if (!level.isEmptyBlock(targetPos) && !targetState.getMaterial().isReplaceable() && !canContain) {
            return false;
        }

        if (!level.isClientSide) {
            if (canContain) {
                ((ILiquidContainer) targetState.getBlock()).placeLiquid(level, targetPos, targetState, fluid.defaultFluidState());
            } else {
                if (!level.isEmptyBlock(targetPos) && !targetState.getMaterial().isLiquid()) {
                    level.destroyBlock(targetPos, true);
                }
                level.setBlock(targetPos, fluid.defaultFluidState().createLegacyBlock(), 11);
            }
            fluidHandler.drain(FluidAttributes.BUCKET_VOLUME, FluidAction.EXECUTE);
        }

        playSound(level, player, targetPos, fluid == Fluids.LAVA ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY);
        return true;
    }

    public static FluidStack getFluid(ItemStack stack) {
        return new GoldenBucketFluidHandler(stack).getFluid();
    }

    private ITextComponent getFluidName(FluidStack fluid) {
        return fluid.isEmpty() ? new TranslationTextComponent("item.goldenbucket.empty") : fluid.getDisplayName();
    }

    private boolean isAllowedFluid(Fluid fluid) {
        return GoldenBucketConfig.isFluidAllowed(fluid);
    }

    private void playSound(World level, @Nullable PlayerEntity player, BlockPos pos, SoundEvent sound) {
        level.playSound(player, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }

    private static class GoldenBucketFluidHandler extends FluidHandlerItemStack {
        private GoldenBucketFluidHandler(ItemStack container) {
            super(container, CAPACITY);
        }

        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            return !fluid.isEmpty() && GoldenBucketConfig.isFluidAllowed(fluid.getFluid());
        }

        @Override
        public boolean canDrainFluidType(FluidStack fluid) {
            return !fluid.isEmpty();
        }
    }
}
