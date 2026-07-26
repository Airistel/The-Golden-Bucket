package dev.xzeroair.goldenbucket;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public final class GoldenBucketConfig {
    public static final String CATEGORY_GENERAL = "general";
    private static final String[] DEFAULT_ALLOWED_FLUIDS = new String[]{"minecraft:water", "minecraft:lava", "minecraft:milk"};
    private static final ResourceLocation MILK_STILL = new ResourceLocation("minecraft", "blocks/water_still");
    private static final ResourceLocation MILK_FLOWING = new ResourceLocation("minecraft", "blocks/water_flow");
    private static Configuration config;

    private GoldenBucketConfig() {
    }

    public static void load(File file) {
        registerMilkFluid();
        config = new Configuration(file);

        try {
            config.load();
            readAllowedFluids();
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }

    private static void registerMilkFluid() {
        if (FluidRegistry.getFluid("milk") == null) {
            FluidRegistry.registerFluid(new Fluid("milk", MILK_STILL, MILK_FLOWING).setUnlocalizedName("milk"));
        }
    }

    public static Configuration getConfig() {
        return config;
    }

    public static String getConfigPath() {
        return config == null ? GoldenBucketMod.MODID : config.toString();
    }

    public static boolean isFluidAllowed(Fluid fluid) {
        String fluidName = FluidRegistry.getFluidName(fluid);
        if (fluidName == null) {
            return false;
        }
        boolean listed = readAllowedFluids().contains(normalizeFluidId(fluidName));
        boolean blacklist = config != null && config.getBoolean("useFluidBlacklist", CATEGORY_GENERAL, false,
                "When true, allowedFluids becomes a blacklist instead of a whitelist.");
        return (blacklist ? !listed : listed) && (!isMilk(fluid) || isMilkAllowed());
    }

    public static FluidStack milkStack() {
        Fluid milk = FluidRegistry.getFluid("milk");
        return milk == null ? null : new FluidStack(milk, Fluid.BUCKET_VOLUME);
    }

    public static boolean isMilk(Fluid fluid) {
        String fluidName = FluidRegistry.getFluidName(fluid);
        return "milk".equals(normalizeFluidId(fluidName));
    }

    private static boolean isMilkAllowed() {
        return config == null || config.getBoolean(
                "allowMilk",
                CATEGORY_GENERAL,
                true,
                "Allows the Golden Bucket to collect milk from cows and drink it like a normal milk bucket."
        );
    }

    private static Set<String> readAllowedFluids() {
        if (config == null) {
            return new LinkedHashSet<>(Arrays.asList("water", "lava", "milk"));
        }

        String[] configuredValues = config.getStringList(
                "allowedFluids",
                CATEGORY_GENERAL,
                DEFAULT_ALLOWED_FLUIDS,
                "Fluids the Golden Bucket can store. Use fluid registry ids like minecraft:water."
        );

        LinkedHashSet<String> configuredFluids = new LinkedHashSet<>();
        for (String configuredValue : configuredValues) {
            String fluidId = normalizeFluidId(configuredValue);
            if (FluidRegistry.getFluid(fluidId) != null) {
                configuredFluids.add(fluidId);
            }
        }

        return configuredFluids;
    }

    private static String normalizeFluidId(String value) {
        String trimmed = value == null ? "" : value.trim();
        int namespaceSeparator = trimmed.indexOf(':');
        if (namespaceSeparator >= 0) {
            return trimmed.substring(namespaceSeparator + 1);
        }

        return trimmed;
    }
}
