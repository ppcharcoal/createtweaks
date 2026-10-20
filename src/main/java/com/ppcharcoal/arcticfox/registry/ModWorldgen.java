package com.ppcharcoal.arcticfox.registry;

import com.mojang.serialization.MapCodec;
import com.ppcharcoal.arcticfox.ArcticFox;
import com.ppcharcoal.arcticfox.world.ArcticBiomeSource;
import com.ppcharcoal.arcticfox.world.ExposedOreFeature;
import com.ppcharcoal.arcticfox.world.DryLandStructure;
import com.ppcharcoal.arcticfox.world.OptionalSwapProcessor;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModWorldgen {
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, ArcticFox.MOD_ID);

    public static final Supplier<MapCodec<ArcticBiomeSource>> ARCTIC =
            BIOME_SOURCES.register("arctic", () -> ArcticBiomeSource.CODEC);

    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, ArcticFox.MOD_ID);

    /** Ore blobs that only keep the ore blocks touching air or water (caves, cliffs), plus a small buried chance. */
    public static final Supplier<ExposedOreFeature> EXPOSED_ORE = FEATURES.register("exposed_ore", ExposedOreFeature::new);

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, ArcticFox.MOD_ID);
    public static final Supplier<StructureType<DryLandStructure>> DRY_LAND = STRUCTURE_TYPES.register("dry_land", () -> () -> DryLandStructure.CODEC);

    public static final DeferredRegister<StructureProcessorType<?>> PROCESSORS = DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, ArcticFox.MOD_ID);
    public static final Supplier<StructureProcessorType<OptionalSwapProcessor>> OPTIONAL_SWAP = PROCESSORS.register("optional_swap", () -> () -> OptionalSwapProcessor.CODEC);

    private ModWorldgen() {
    }
}
