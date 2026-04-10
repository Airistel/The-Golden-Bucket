package dev.xzeroair.goldenbucket;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(GoldenBucketMod.MODID)
public class GoldenBucketMod {
    public static final String MODID = "goldenbucket";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final RegistryObject<Item> GOLDEN_BUCKET = ITEMS.register("golden_bucket", GoldenBucketItem::new);

    public GoldenBucketMod() {
        ForgeMod.enableMilkFluid();
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modEventBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, GoldenBucketConfig.SPEC);
    }

    static Item.Properties itemProperties() {
        return new Item.Properties().stacksTo(1).tab(ItemGroup.TAB_TOOLS);
    }
}
