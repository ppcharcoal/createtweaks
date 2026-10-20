package com.ppcharcoal.arcticfox.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ppcharcoal.arcticfox.registry.ModWorldgen;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Wraps another structure (normally a jigsaw) and only lets it generate on dry, fairly flat land:
 * no water anywhere under its footprint (frozen lakes count as water) and limited height difference.
 */
public class DryLandStructure extends Structure {
    public static final MapCodec<DryLandStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            Structure.CODEC.fieldOf("inner").forGetter(s -> s.inner),
            Codec.intRange(1, 48).optionalFieldOf("radius", 10).forGetter(s -> s.radius),
            Codec.intRange(0, 64).optionalFieldOf("max_slope", 4).forGetter(s -> s.maxSlope)
    ).apply(instance, DryLandStructure::new));

    private final Holder<Structure> inner;
    private final int radius;
    private final int maxSlope;

    public DryLandStructure(StructureSettings settings, Holder<Structure> inner, int radius, int maxSlope) {
        super(settings);
        this.inner = inner;
        this.radius = radius;
        this.maxSlope = maxSlope;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int cx = context.chunkPos().getMiddleBlockX();
        int cz = context.chunkPos().getMiddleBlockZ();
        int sea = context.chunkGenerator().getSeaLevel();
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int dx = -radius; dx <= radius; dx += radius) {
            for (int dz = -radius; dz <= radius; dz += radius) {
                int x = cx + dx, z = cz + dz;
                int surface = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
                int floor = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
                if (surface > floor || surface <= sea) return Optional.empty(); // water here
                min = Math.min(min, surface);
                max = Math.max(max, surface);
            }
        }
        if (max - min > maxSlope) return Optional.empty();
        return inner.value().findValidGenerationPoint(context);
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgen.DRY_LAND.get();
    }
}
