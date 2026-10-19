package com.ppcharcoal.arcticfox.world;

import com.mojang.datafixers.util.Pair;
import com.ppcharcoal.arcticfox.ArcticFox;
import com.ppcharcoal.arcticfox.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.MiscOverworldFeatures;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * New worlds start the player in a biome with a few scattered trees (snowy plains), not in a forest
 * and not in a treeless waste, so the first wood is within reach.
 */
@EventBusSubscriber(modid = ArcticFox.MOD_ID)
public final class SpawnSelector {
    public static final TagKey<Biome> SPAWN_BIOMES = TagKey.create(Registries.BIOME, ArcticFox.id("spawn_biomes"));

    private SpawnSelector() {
    }

    @SubscribeEvent
    public static void onCreateSpawn(LevelEvent.CreateSpawnPosition event) {
        if (!Config.SPAWN_NEAR_TREES.get() || !(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(b -> b.is(SPAWN_BIOMES), new BlockPos(0, 80, 0), 6400, 32, 64);
        if (found == null) {
            ArcticFox.LOGGER.warn("No spawn biome with trees found nearby, using the default spawn");
            return;
        }
        BlockPos center = found.getFirst();
        BlockPos best = null;
        int bestTrees = -1;
        // look around the biome hit for dry land with the most trees in view
        for (int dx = -64; dx <= 64 && bestTrees < 6; dx += 16) {
            for (int dz = -64; dz <= 64; dz += 16) {
                BlockPos column = center.offset(dx, 0, dz);
                if (!level.getBiome(column.atY(80)).is(SPAWN_BIOMES)) continue;
                BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
                BlockState ground = level.getBlockState(top.below());
                if (!ground.getFluidState().isEmpty() || ground.is(BlockTags.ICE)) continue;
                int trees = countLogs(level, top, 24);
                if (trees > bestTrees) {
                    bestTrees = trees;
                    best = top;
                }
            }
        }
        if (best == null) return;
        final BlockPos spawn = best;
        level.setDefaultSpawnPos(spawn, 0.0F);
        ArcticFox.LOGGER.info("World spawn set to {} ({} trees nearby)", spawn.toShortString(), bestTrees);
        if (level.getServer().getWorldData().worldGenOptions().generateBonusChest()) {
            level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getHolder(MiscOverworldFeatures.BONUS_CHEST)
                    .ifPresent(feature -> feature.value().place(level, level.getChunkSource().getGenerator(), level.random, spawn));
        }
        event.setCanceled(true);
    }

    /** Number of distinct tree trunks (log columns touching the ground) around a position. */
    public static int countLogs(ServerLevel level, BlockPos center, int radius) {
        int trunks = 0;
        for (int x = -radius; x <= radius; x += 2) {
            for (int z = -radius; z <= radius; z += 2) {
                BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.offset(x, 0, z));
                if (level.getBlockState(top.below()).is(BlockTags.LOGS)) trunks++;
            }
        }
        return trunks;
    }
}
