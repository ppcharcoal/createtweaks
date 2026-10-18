package com.ppcharcoal.arcticfox.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

import java.util.List;

/**
 * Same blob shape as vanilla ores, but an ore block is only placed where it touches air or a fluid
 * (cave walls, ravines, cliffs and mountain faces). Ore that would be sealed inside solid rock is
 * placed only with {@code buried_chance}, so strip mining finds very little.
 */
public class ExposedOreFeature extends Feature<ExposedOreFeature.Config> {
    public record Config(List<OreConfiguration.TargetBlockState> targets, int size, float buriedChance)
            implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.list(OreConfiguration.TargetBlockState.CODEC).fieldOf("targets").forGetter(Config::targets),
                Codec.intRange(0, 64).fieldOf("size").forGetter(Config::size),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("buried_chance", 0.08F).forGetter(Config::buriedChance)
        ).apply(instance, Config::new));
    }

    public ExposedOreFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        Config config = context.config();
        int size = config.size();
        if (size <= 0) {
            return false;
        }

        float angle = random.nextFloat() * (float) Math.PI;
        float spread = size / 8.0F;
        double x0 = origin.getX() + Mth.sin(angle) * spread;
        double x1 = origin.getX() - Mth.sin(angle) * spread;
        double z0 = origin.getZ() + Mth.cos(angle) * spread;
        double z1 = origin.getZ() - Mth.cos(angle) * spread;
        double y0 = origin.getY() + random.nextInt(3) - 2;
        double y1 = origin.getY() + random.nextInt(3) - 2;

        LongOpenHashSet visited = new LongOpenHashSet();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int placed = 0;

        for (int i = 0; i < size; i++) {
            float t = (float) i / (float) size;
            double cx = Mth.lerp(t, x0, x1);
            double cy = Mth.lerp(t, y0, y1);
            double cz = Mth.lerp(t, z0, z1);
            double radius = ((Mth.sin((float) Math.PI * t) + 1.0F) * random.nextDouble() * size / 16.0 + 1.0) / 2.0;

            int minX = Mth.floor(cx - radius), maxX = Mth.floor(cx + radius);
            int minY = Mth.floor(cy - radius), maxY = Mth.floor(cy + radius);
            int minZ = Mth.floor(cz - radius), maxZ = Mth.floor(cz + radius);
            for (int x = minX; x <= maxX; x++) {
                double dx = (x + 0.5 - cx) / radius;
                if (dx * dx >= 1.0) continue;
                for (int y = minY; y <= maxY; y++) {
                    double dy = (y + 0.5 - cy) / radius;
                    if (dx * dx + dy * dy >= 1.0 || level.isOutsideBuildHeight(y)) continue;
                    for (int z = minZ; z <= maxZ; z++) {
                        double dz = (z + 0.5 - cz) / radius;
                        if (dx * dx + dy * dy + dz * dz >= 1.0) continue;
                        pos.set(x, y, z);
                        if (!visited.add(pos.asLong())) continue;
                        if (tryPlace(level, random, config, pos)) placed++;
                    }
                }
            }
        }
        return placed > 0;
    }

    private static boolean tryPlace(WorldGenLevel level, RandomSource random, Config config, BlockPos.MutableBlockPos pos) {
        BlockState current = level.getBlockState(pos);
        for (OreConfiguration.TargetBlockState target : config.targets()) {
            if (!target.target.test(current, random)) continue;
            if (!isExposed(level, pos) && random.nextFloat() >= config.buriedChance()) {
                return false;
            }
            level.setBlock(pos, target.state, 2);
            return true;
        }
        return false;
    }

    private static boolean isExposed(WorldGenLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.values()) {
            neighbor.setWithOffset(pos, direction);
            BlockState state = level.getBlockState(neighbor);
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
