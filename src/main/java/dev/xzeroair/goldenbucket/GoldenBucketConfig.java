package dev.xzeroair.goldenbucket;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class GoldenBucketConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ALLOWED_FLUIDS = BUILDER
            .comment("Fluids the Golden Bucket can store. Use fluid registry ids like minecraft:water.")
            .defineList("allowedFluids", Arrays.asList("minecraft:water", "minecraft:lava", "minecraft:milk"), GoldenBucketConfig::isValidFluidId);

    private static final ForgeConfigSpec.BooleanValue ALLOW_MILK = BUILDER
            .comment("Allows the Golden Bucket to collect milk from cows and drink it like a normal milk bucket.")
            .define("allowMilk", true);

    private static final ForgeConfigSpec.BooleanValue USE_FLUID_BLACKLIST = BUILDER
            .comment("When true, allowedFluids becomes a blacklist instead of a whitelist.")
            .define("useFluidBlacklist", false);

    private static final ResourceLocation MILK_ID = new ResourceLocation("minecraft", "milk");

    static final ForgeConfigSpec SPEC = BUILDER.build();

    private GoldenBucketConfig() {
    }

    public static boolean isFluidAllowed(Fluid fluid) {
        ResourceLocation key = ForgeRegistries.FLUIDS.getKey(fluid);
        if (key == null) {
            return false;
        }
        boolean listed = configuredFluidIds().contains(key);
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

    private static Set<ResourceLocation> configuredFluidIds() {
        LinkedHashSet<ResourceLocation> configuredFluids = new LinkedHashSet<>();
        for (String fluidId : ALLOWED_FLUIDS.get()) {
            ResourceLocation id = parseFluidId(fluidId);
            if (id != null) {
                configuredFluids.add(id);
            }
        }

        return configuredFluids;
    }

    private static boolean isValidFluidId(Object value) {
        return value instanceof String && parseFluidId((String) value) != null;
    }

    private static ResourceLocation parseFluidId(String value) {
        try {
            return new ResourceLocation(value);
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
