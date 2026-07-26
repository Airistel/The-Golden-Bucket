package dev.xzeroair.goldenbucket;

import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = GoldenBucketMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GoldenBucketClientEvents {
    private GoldenBucketClientEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemModelsProperties.register(
                GoldenBucketMod.GOLDEN_BUCKET.get(),
                new ResourceLocation(GoldenBucketMod.MODID, "fluid"),
                (stack, level, entity) -> getFluidModelValue(stack)
        ));
    }

    private static float getFluidModelValue(ItemStack stack) {
        FluidStack fluidStack = GoldenBucketItem.getFluid(stack);
        if (fluidStack.isEmpty()) {
            return 0.0F;
        }

        Fluid fluid = fluidStack.getFluid();
        if (fluid == Fluids.WATER) {
            return 0.1F;
        }
        if (fluid == Fluids.LAVA) {
            return 0.2F;
        }
        if (GoldenBucketConfig.isMilk(fluid)) {
            return 0.3F;
        }
        return 0.1F;
    }
}
