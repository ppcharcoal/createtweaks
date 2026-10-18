package com.ppcharcoal.arcticfox.registry;

import com.mojang.serialization.MapCodec;
import com.ppcharcoal.arcticfox.ArcticFox;
import com.ppcharcoal.arcticfox.world.ArcticBiomeSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModWorldgen {
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, ArcticFox.MOD_ID);

    public static final Supplier<MapCodec<ArcticBiomeSource>> ARCTIC =
            BIOME_SOURCES.register("arctic", () -> ArcticBiomeSource.CODEC);

    private ModWorldgen() {
    }
}
