package dev.xzeroair.goldenbucket;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = GoldenBucketMod.MODID, value = Side.CLIENT)
public final class GoldenBucketClientEvents {
    private GoldenBucketClientEvents() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(
                GoldenBucketMod.GOLDEN_BUCKET,
                0,
                new ModelResourceLocation(GoldenBucketMod.MODID + ":golden_bucket", "inventory")
        );
    }
}
