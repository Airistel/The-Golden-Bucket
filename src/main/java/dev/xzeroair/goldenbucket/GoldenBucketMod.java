package dev.xzeroair.goldenbucket;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod(modid = GoldenBucketMod.MODID, name = GoldenBucketMod.NAME, version = GoldenBucketMod.VERSION, guiFactory = "dev.xzeroair.goldenbucket.client.GoldenBucketGuiFactory")
@Mod.EventBusSubscriber
public final class GoldenBucketMod {
    public static final String MODID = "goldenbucket";
    public static final String NAME = "Golden Bucket";
    public static final String VERSION = "1.0.1";

    @GameRegistry.ObjectHolder(MODID + ":golden_bucket")
    public static final Item GOLDEN_BUCKET = null;

    public GoldenBucketMod() {
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        GoldenBucketConfig.load(event.getSuggestedConfigurationFile());
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new GoldenBucketItem()
                .setRegistryName(MODID, "golden_bucket")
                .setTranslationKey(MODID + ".golden_bucket")
                .setCreativeTab(CreativeTabs.TOOLS));
    }
}
