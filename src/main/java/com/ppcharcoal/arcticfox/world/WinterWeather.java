package com.ppcharcoal.arcticfox.world;

import com.ppcharcoal.arcticfox.ArcticFox;
import com.ppcharcoal.arcticfox.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Night-time snowstorms and deep snow.
 * <ul>
 *     <li>Each day is either sunny or snowy (rolled per day from the world seed); every night is a blizzard
 *     (rain + thunder, which falls as snow). Natural lightning is suppressed. Clients get blizzard fog.</li>
 *     <li>While it snows, snow keeps piling up: layers grow to a full block, then turn into snow blocks
 *     with new layers on top, up to {@code snow.maxDepthBlocks} (3) blocks. Bright block light (torches)
 *     and glass/ice/heat blocks keep spots clear. Works with or without Snow! Real Magic.</li>
 * </ul>
 */
@EventBusSubscriber(modid = ArcticFox.MOD_ID)
public final class WinterWeather {
    public static final TagKey<Block> NO_SETTLE = TagKey.create(Registries.BLOCK, ArcticFox.id("snow_does_not_settle_on"));
    private static final int NIGHT_START = 12600;
    private static final int NIGHT_END = 23400;

    private WinterWeather() {
    }

    public static boolean isNight(Level level) {
        long t = level.getDayTime() % 24000L;
        return t >= NIGHT_START && t < NIGHT_END;
    }

    public enum Sky { CLEAR, SNOW, BLIZZARD }

    /** Which day (dawn to dawn) a given day time belongs to. */
    public static long dayIndex(long dayTime) {
        return Math.floorDiv(dayTime - NIGHT_END + 24000L, 24000L);
    }

    /** Daytime weather for a day: deterministic per world seed, so it survives restarts. */
    public static boolean isSnowyDay(ServerLevel level, long day) {
        long h = level.getSeed() ^ (day * 0x9E3779B97F4A7C15L);
        h ^= (h >>> 33); h *= 0xff51afd7ed558ccdL; h ^= (h >>> 33);
        double roll = (h >>> 11) * 0x1.0p-53;
        return roll < Config.SNOWY_DAY_CHANCE.get();
    }

    public static Sky desired(ServerLevel level) {
        if (isNight(level)) return Sky.BLIZZARD;
        return isSnowyDay(level, dayIndex(level.getDayTime())) ? Sky.SNOW : Sky.CLEAR;
    }

    /** Ticks until the next weather change (dawn or dusk). */
    private static int ticksUntilChange(ServerLevel level) {
        long t = level.getDayTime() % 24000L;
        long next = isNight(level) ? (t < NIGHT_END ? NIGHT_END : NIGHT_END + 24000) : (t < NIGHT_START ? NIGHT_START : NIGHT_START + 24000);
        return (int) (next - t) + 40;
    }

    /** Called every 20 ticks for the overworld: sunny or snowy days, a blizzard every night. */
    public static void updateWeather(ServerLevel level) {
        if (!Config.NIGHT_SNOWSTORMS.get() || level.dimension() != Level.OVERWORLD) {
            return;
        }
        Sky sky = desired(level);
        boolean raining = level.getLevelData().isRaining();
        boolean thundering = level.getLevelData().isThundering();
        int duration = ticksUntilChange(level);
        switch (sky) {
            case BLIZZARD -> { if (!raining || !thundering) level.setWeatherParameters(0, duration, true, true); }
            case SNOW -> { if (!raining || thundering) level.setWeatherParameters(0, duration, true, false); }
            case CLEAR -> { if (raining || thundering) level.setWeatherParameters(duration, 0, false, false); }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        if (level.getGameTime() % 20 == 0) {
            updateWeather(level);
        }
        if (Config.SNOW_STACKING.get() && level.isRaining()) {
            int attempts = Config.SNOW_ATTEMPTS_PER_TICK.get() * (level.isThundering() ? 2 : 1);
            RandomSource random = level.getRandom();
            for (ServerPlayer player : level.players()) {
                if (player.isSpectator()) continue;
                for (int i = 0; i < attempts; i++) {
                    int x = player.getBlockX() + random.nextInt(97) - 48;
                    int z = player.getBlockZ() + random.nextInt(97) - 48;
                    accumulate(level, x, z);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLightning(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LightningBolt bolt && !event.getLevel().isClientSide()
                && Config.NIGHT_SNOWSTORMS.get() && Config.NO_NATURAL_LIGHTNING.get()
                && bolt.getCause() == null && event.getLevel().dimension() == Level.OVERWORLD) {
            event.setCanceled(true);
        }
    }

    /**
     * Adds one snow layer to the column at x/z if it is snowing there. Returns true if snow was added.
     */
    public static boolean accumulate(ServerLevel level, int x, int z) {
        if (!level.hasChunk(x >> 4, z >> 4)) return false;
        BlockPos top = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(x, 0, z)).below();
        BlockPos above = top.above();
        if (level.isOutsideBuildHeight(above)) return false;
        Holder<Biome> biome = level.getBiome(above);
        if (biome.value().getPrecipitationAt(above) != Biome.Precipitation.SNOW) return false;
        if (level.getBrightness(LightLayer.BLOCK, above) > 10) return false;
        return addLayer(level, top);
    }

    /** Adds a snow layer at/above {@code top} respecting the depth limit. Visible for tests. */
    public static boolean addLayer(ServerLevel level, BlockPos top) {
        int maxLayers = Config.SNOW_MAX_DEPTH.get() * 8;
        BlockState state = level.getBlockState(top);
        BlockPos above = top.above();
        if (state.is(Blocks.SNOW)) {
            int layers = state.getValue(SnowLayerBlock.LAYERS);
            int depth = layers + 8 * snowBlocksBelow(level, top, maxLayers / 8);
            if (depth >= maxLayers) return false;
            if (layers < 8) {
                level.setBlock(top, state.setValue(SnowLayerBlock.LAYERS, layers + 1), Block.UPDATE_ALL);
                return true;
            }
            if (!level.getBlockState(above).isAir()) return false;
            level.setBlock(top, Blocks.SNOW_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(above, Blocks.SNOW.defaultBlockState(), Block.UPDATE_ALL);
            return true;
        }
        if (state.is(NO_SETTLE) || !level.getBlockState(above).isAir()) return false;
        if (state.is(Blocks.SNOW_BLOCK)) {
            int depth = 8 * (1 + snowBlocksBelow(level, top, maxLayers / 8));
            if (depth >= maxLayers) return false;
        }
        BlockState snow = Blocks.SNOW.defaultBlockState();
        if (!snow.canSurvive(level, above)) return false;
        level.setBlock(above, snow, Block.UPDATE_ALL);
        return true;
    }

    private static int snowBlocksBelow(ServerLevel level, BlockPos pos, int limit) {
        int count = 0;
        BlockPos.MutableBlockPos m = pos.mutable();
        for (int i = 0; i < limit; i++) {
            m.move(0, -1, 0);
            if (!level.getBlockState(m).is(Blocks.SNOW_BLOCK)) break;
            count++;
        }
        return count;
    }
}
