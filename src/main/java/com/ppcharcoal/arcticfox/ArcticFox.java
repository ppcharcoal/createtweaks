package com.ppcharcoal.arcticfox;

import com.mojang.logging.LogUtils;
import com.ppcharcoal.arcticfox.compat.CreateCompat;
import com.ppcharcoal.arcticfox.registry.ModBlocks;
import com.ppcharcoal.arcticfox.registry.ModConditions;
import com.ppcharcoal.arcticfox.registry.ModItems;
import com.ppcharcoal.arcticfox.registry.ModWorldgen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(ArcticFox.MOD_ID)
public final class ArcticFox {
    public static final String MOD_ID = "arcticfox";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ArcticFox(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModWorldgen.BIOME_SOURCES.register(modBus);
        ModConditions.CONDITIONS.register(modBus);

        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        if (ModList.get().isLoaded("create")) {
            CreateCompat.init();
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
