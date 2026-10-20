package com.ppcharcoal.arcticfox.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Wraps another biome source (normally the vanilla multi-noise overworld) and swaps every biome
 * it produces through a remap table. Terrain shape and climate layout stay vanilla, but every
 * temperate/warm biome is replaced by a frozen counterpart, so the whole surface is snowy while
 * mountains, rivers, coasts and caves still land where they would naturally.
 */
public final class ArcticBiomeSource extends BiomeSource {
    public static final MapCodec<ArcticBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("delegate").forGetter(source -> source.delegate),
            Codec.unboundedMap(ResourceLocation.CODEC, Biome.CODEC).fieldOf("remap").forGetter(source -> source.remap)
    ).apply(instance, ArcticBiomeSource::new));

    private final BiomeSource delegate;
    private final Map<ResourceLocation, Holder<Biome>> remap;

    public ArcticBiomeSource(BiomeSource delegate, Map<ResourceLocation, Holder<Biome>> remap) {
        this.delegate = delegate;
        this.remap = Map.copyOf(remap);
    }

    private Holder<Biome> map(Holder<Biome> biome) {
        Optional<ResourceKey<Biome>> key = biome.unwrapKey();
        if (key.isEmpty()) {
            return biome;
        }
        Holder<Biome> replacement = this.remap.get(key.get().location());
        return replacement != null ? replacement : biome;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return this.delegate.possibleBiomes().stream().map(this::map).distinct();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        return this.map(this.delegate.getNoiseBiome(x, y, z, sampler));
    }
}
