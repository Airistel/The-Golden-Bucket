package dev.xzeroair.goldenbucket;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;

import javax.annotation.Nullable;
import java.util.List;

public class GoldenBucketItem extends Item {
    public static final int BUCKET_VOLUME = Fluid.BUCKET_VOLUME;
    public static final int CAPACITY = 3 * BUCKET_VOLUME;

    public GoldenBucketItem() {
        setMaxStackSize(1);
        addPropertyOverride(new ResourceLocation(GoldenBucketMod.MODID, "fluid"), (stack, world, entity) -> getFluidModelValue(stack));
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new GoldenBucketFluidHandler(stack);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        GoldenBucketFluidHandler fluidHandler = new GoldenBucketFluidHandler(stack);
        FluidStack storedFluid = fluidHandler.getFluid();
        if (storedFluid != null && GoldenBucketConfig.isMilk(storedFluid.getFluid())) {
            player.setActiveHand(hand);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        RayTraceResult hit = rayTrace(world, player, true);

        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }

        BlockPos blockPos = hit.getBlockPos();
        EnumFacing side = hit.sideHit;
        BlockPos targetPos = blockPos.offset(side);

        if (!world.isBlockModifiable(player, blockPos) || !player.canPlayerEdit(targetPos, side, stack)) {
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }

        if (!player.isSneaking() && tryPickupFluid(world, player, fluidHandler, blockPos)) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        if (tryPlaceFluid(world, player, fluidHandler, blockPos, side)) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        return new ActionResult<>(EnumActionResult.FAIL, stack);
    }

    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase target, EnumHand hand) {
        if (!(target instanceof EntityCow) || target.isChild()) {
            return false;
        }

        GoldenBucketFluidHandler fluidHandler = new GoldenBucketFluidHandler(stack);
        FluidStack milk = GoldenBucketConfig.milkStack();
        if (milk == null || !GoldenBucketConfig.isFluidAllowed(milk.getFluid()) || fluidHandler.fill(milk, false) != BUCKET_VOLUME) {
            return false;
        }

        if (!player.world.isRemote) {
            fluidHandler.fill(milk, true);
        }

        player.world.playSound(player, target.getPosition(), SoundEvents.ENTITY_COW_MILK, SoundCategory.NEUTRAL, 1.0F, 1.0F);
        return true;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        FluidStack fluid = getFluid(stack);
        return fluid != null && GoldenBucketConfig.isMilk(fluid.getFluid()) ? 32 : 0;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        FluidStack fluid = getFluid(stack);
        return fluid != null && GoldenBucketConfig.isMilk(fluid.getFluid()) ? EnumAction.DRINK : EnumAction.NONE;
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) {
            return stack;
        }

        EntityPlayer player = (EntityPlayer) entity;
        FluidStack fluid = getFluid(stack);
        if (fluid == null || !GoldenBucketConfig.isMilk(fluid.getFluid())) {
            return stack;
        }

        player.curePotionEffects(new ItemStack(Items.MILK_BUCKET));
        if (!player.capabilities.isCreativeMode) {
            new GoldenBucketFluidHandler(stack).drain(BUCKET_VOLUME, true);
        }

        return stack;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        FluidStack fluid = getFluid(stack);
        tooltip.add(TextFormatting.AQUA + I18n.translateToLocalFormatted(
                "item.goldenbucket.golden_bucket.tooltip",
                getFluidName(fluid),
                fluid == null ? 0 : fluid.amount,
                CAPACITY
        ));
        tooltip.add(TextFormatting.GRAY + I18n.translateToLocal("item.goldenbucket.golden_bucket.sneak_tooltip"));
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        FluidStack fluid = getFluid(stack);
        return fluid != null && fluid.amount > 0;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        FluidStack fluid = getFluid(stack);
        int amount = fluid == null ? 0 : fluid.amount;
        return 1.0D - (double) amount / CAPACITY;
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        FluidStack fluid = getFluid(stack);
        if (fluid != null && fluid.getFluid() == FluidRegistry.LAVA) {
            return 0xFF6A00;
        }
        return 0x3F76E4;
    }

    private boolean tryPickupFluid(World world, EntityPlayer player, GoldenBucketFluidHandler fluidHandler, BlockPos blockPos) {
        IBlockState state = world.getBlockState(blockPos);
        Fluid fluid = getFluidFromState(state);
        if (fluid == null || !GoldenBucketConfig.isFluidAllowed(fluid) || !isSourceBlock(state)) {
            return false;
        }

        FluidStack resource = new FluidStack(fluid, BUCKET_VOLUME);
        if (fluidHandler.fill(resource, false) != BUCKET_VOLUME) {
            return false;
        }

        if (!world.isRemote) {
            world.setBlockToAir(blockPos);
            fluidHandler.fill(resource, true);
        }

        world.playSound(player, blockPos, fluid == FluidRegistry.LAVA ? SoundEvents.ITEM_BUCKET_FILL_LAVA : SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    private boolean tryPlaceFluid(World world, EntityPlayer player, GoldenBucketFluidHandler fluidHandler, BlockPos blockPos, EnumFacing side) {
        IBlockState state = world.getBlockState(blockPos);
        BlockPos targetPos = state.getBlock().isReplaceable(world, blockPos) ? blockPos : blockPos.offset(side);
        FluidStack drained = fluidHandler.drain(BUCKET_VOLUME, false);

        if (drained == null || drained.amount != BUCKET_VOLUME || !player.canPlayerEdit(targetPos, side, fluidHandler.getContainer())) {
            return false;
        }

        Block block = getBlockForFluid(drained.getFluid());
        if (block == null) {
            return false;
        }

        IBlockState targetState = world.getBlockState(targetPos);
        Material material = targetState.getMaterial();
        if (!world.isAirBlock(targetPos) && !targetState.getBlock().isReplaceable(world, targetPos) && material != Material.WATER && material != Material.LAVA) {
            return false;
        }

        if (!world.isRemote) {
            if (!world.isAirBlock(targetPos) && material != Material.WATER && material != Material.LAVA) {
                world.destroyBlock(targetPos, true);
            }
            world.setBlockState(targetPos, block.getDefaultState(), 11);
            fluidHandler.drain(BUCKET_VOLUME, true);
        }

        world.playSound(player, targetPos, drained.getFluid() == FluidRegistry.LAVA ? SoundEvents.ITEM_BUCKET_EMPTY_LAVA : SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    @Nullable
    private Fluid getFluidFromState(IBlockState state) {
        Material material = state.getMaterial();
        if (material == Material.WATER) {
            return FluidRegistry.WATER;
        }
        if (material == Material.LAVA) {
            return FluidRegistry.LAVA;
        }
        return null;
    }

    private boolean isSourceBlock(IBlockState state) {
        return state.getBlock() instanceof BlockLiquid && state.getValue(BlockLiquid.LEVEL) == 0;
    }

    @Nullable
    private Block getBlockForFluid(Fluid fluid) {
        if (fluid == FluidRegistry.WATER) {
            return Blocks.FLOWING_WATER;
        }
        if (fluid == FluidRegistry.LAVA) {
            return Blocks.FLOWING_LAVA;
        }
        return null;
    }

    @Nullable
    public static FluidStack getFluid(ItemStack stack) {
        return new GoldenBucketFluidHandler(stack).getFluid();
    }

    private static float getFluidModelValue(ItemStack stack) {
        FluidStack fluidStack = getFluid(stack);
        if (fluidStack == null) {
            return 0.0F;
        }

        Fluid fluid = fluidStack.getFluid();
        if (fluid == FluidRegistry.WATER) {
            return 0.1F;
        }
        if (fluid == FluidRegistry.LAVA) {
            return 0.2F;
        }
        if (GoldenBucketConfig.isMilk(fluid)) {
            return 0.3F;
        }
        return 0.0F;
    }

    private String getFluidName(@Nullable FluidStack fluid) {
        if (fluid == null || fluid.getFluid() == null) {
            return I18n.translateToLocal("item.goldenbucket.empty");
        }

        ResourceLocation name = FluidRegistry.getFluidName(fluid.getFluid()) == null ? null : new ResourceLocation(FluidRegistry.getFluidName(fluid.getFluid()));
        return name == null ? fluid.getLocalizedName() : name.toString();
    }

    private static class GoldenBucketFluidHandler extends FluidHandlerItemStack {
        private GoldenBucketFluidHandler(ItemStack container) {
            super(container, CAPACITY);
        }

        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            return fluid != null && GoldenBucketConfig.isFluidAllowed(fluid.getFluid());
        }

        @Override
        public boolean canDrainFluidType(FluidStack fluid) {
            return fluid != null && GoldenBucketConfig.isFluidAllowed(fluid.getFluid());
        }
    }
}
