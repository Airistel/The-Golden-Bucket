package dev.xzeroair.goldenbucket;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(GoldenBucketMod.MODID)
public class GoldenBucketMod {
    public static final String MODID = "goldenbucket";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final RegistryObject<Item> GOLDEN_BUCKET = ITEMS.register("golden_bucket", GoldenBucketItem::new);

    public GoldenBucketMod(FMLJavaModLoadingContext context) {
        ForgeMod.enableMilkFluid();
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::addCreative);
        ITEMS.register(modEventBus);
        context.registerConfig(ModConfig.Type.COMMON, GoldenBucketConfig.SPEC);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(GOLDEN_BUCKET);
        }
    }
}
