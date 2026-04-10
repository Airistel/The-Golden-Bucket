package dev.xzeroair.goldenbucket.client;

import dev.xzeroair.goldenbucket.GoldenBucketConfig;
import dev.xzeroair.goldenbucket.GoldenBucketMod;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.DefaultGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class GoldenBucketGuiFactory extends DefaultGuiFactory {
    public GoldenBucketGuiFactory() {
        super(GoldenBucketMod.MODID, GuiConfig.getAbridgedConfigPath(GoldenBucketConfig.getConfigPath()));
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parent) {
        return new GuiConfig(parent, getConfigElements(), GoldenBucketMod.MODID, false, false, GoldenBucketConfig.getConfigPath());
    }

    @Nonnull
    private static List<IConfigElement> getConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        if (GoldenBucketConfig.getConfig() != null) {
            list.addAll(new ConfigElement(GoldenBucketConfig.getConfig().getCategory(GoldenBucketConfig.CATEGORY_GENERAL)).getChildElements());
        }
        return list;
    }
}
