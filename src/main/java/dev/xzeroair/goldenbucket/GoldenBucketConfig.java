package dev.xzeroair.goldenbucket;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = GoldenBucketMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GoldenBucketConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ALLOWED_FLUIDS = BUILDER
            .comment("Fluids the Golden Bucket can store. Use fluid registry ids like minecraft:water.")
            .defineListAllowEmpty("allowedFluids", List.of("minecraft:water", "minecraft:lava", "minecraft:milk"), GoldenBucketConfig::isValidFluidId);

    private static final ForgeConfigSpec.BooleanValue ALLOW_MILK = BUILDER
            .comment("Allows the Golden Bucket to collect milk from cows and drink it like a normal milk bucket.")
            .define("allowMilk", true);

    private static final ForgeConfigSpec.BooleanValue USE_FLUID_BLACKLIST = BUILDER
            .comment("When true, allowedFluids becomes a blacklist instead of a whitelist.")
            .define("useFluidBlacklist", false);

    private static final ResourceLocation MILK_ID = fluidId("minecraft:milk");

    static final ForgeConfigSpec SPEC = BUILDER.build();

    private static Set<ResourceLocation> allowedFluids = Set.of(
            fluidId("minecraft:water"),
            fluidId("minecraft:lava"),
            fluidId("minecraft:milk")
    );

    private GoldenBucketConfig() {
    }

    public static boolean isFluidAllowed(Fluid fluid) {
        ResourceLocation key = ForgeRegistries.FLUIDS.getKey(fluid);
        if (key == null) {
            return false;
        }
        boolean listed = allowedFluids.contains(key);
        boolean permittedByFilter = USE_FLUID_BLACKLIST.get() ? !listed : listed;
        return permittedByFilter && (!isMilk(fluid) || ALLOW_MILK.get());
    }

    public static Fluid milkFluid() {
        return ForgeRegistries.FLUIDS.getValue(MILK_ID);
    }

    public static boolean isMilk(Fluid fluid) {
        ResourceLocation key = ForgeRegistries.FLUIDS.getKey(fluid);
        return MILK_ID.equals(key);
    }

    private static ResourceLocation fluidId(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            throw new IllegalArgumentException("Invalid built-in fluid id: " + value);
        }

        return id;
    }

    private static boolean isValidFluidId(final Object value) {
        if (!(value instanceof String fluidId)) {
            return false;
        }

        ResourceLocation id = ResourceLocation.tryParse(fluidId);
        return id != null && ForgeRegistries.FLUIDS.containsKey(id);
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        LinkedHashSet<ResourceLocation> configuredFluids = new LinkedHashSet<>();
        for (String fluidId : ALLOWED_FLUIDS.get()) {
            ResourceLocation id = ResourceLocation.tryParse(fluidId);
            if (id != null) {
                configuredFluids.add(id);
            }
        }

        allowedFluids = Set.copyOf(configuredFluids);
    }
}
