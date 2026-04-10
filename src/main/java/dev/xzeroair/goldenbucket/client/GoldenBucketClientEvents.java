package dev.xzeroair.goldenbucket.client;

import dev.xzeroair.goldenbucket.GoldenBucketItem;
import dev.xzeroair.goldenbucket.GoldenBucketMod;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = GoldenBucketMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GoldenBucketClientEvents {
    private GoldenBucketClientEvents() {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                GoldenBucketMod.GOLDEN_BUCKET.get(),
                fluidPropertyId(),
                (stack, level, entity, seed) -> getFluidModelValue(stack)
        ));
    }

    private static ResourceLocation fluidPropertyId() {
        ResourceLocation id = ResourceLocation.tryParse(GoldenBucketMod.MODID + ":fluid");
        if (id == null) {
            throw new IllegalStateException("Invalid Golden Bucket fluid property id");
        }

        return id;
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

        ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(fluid);
        if (fluidId != null && "milk".equals(fluidId.getPath())) {
            return 0.3F;
        }

        return 0.0F;
    }
}
