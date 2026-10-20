package com.ppcharcoal.arcticfox.world;

import com.ppcharcoal.arcticfox.ArcticFox;
import com.ppcharcoal.arcticfox.Config;
import com.ppcharcoal.arcticfox.block.CabinStoveBlock;
import com.ppcharcoal.arcticfox.compat.CreateReflect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Working heat sources melt the snow around them, and new snow doesn't settle there: Create blaze burners
 * (wider the hotter they burn), steam engines on a running boiler, pipes, pumps, tanks and basins carrying
 * lava or other hot fluids, Create: New Age heaters and heat pipes, the cast-iron stove, lit furnaces and campfires.
 * Every 2 seconds the block entities in the chunks around each player are scanned.
 */
@EventBusSubscriber(modid = ArcticFox.MOD_ID)
public final class SnowMelt {
    private static final int SCAN_INTERVAL = 40;
    private static final int CHUNK_RADIUS = 4;

    /** Heat sources by chunk, refreshed every scan. */
    private static final Map<Level, Map<Long, List<long[]>>> SOURCES = new WeakHashMap<>();

    private static boolean newAgeInit;
    private static Method newAgeHeat;

    private SnowMelt() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !Config.HEAT_MELTS_SNOW.get()) return;
        if (level.getGameTime() % SCAN_INTERVAL != 7) return;
        Set<Long> chunks = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            ChunkPos c = player.chunkPosition();
            for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
                for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                    chunks.add(ChunkPos.asLong(c.x + dx, c.z + dz));
                }
            }
        }
        Map<Long, List<long[]>> found = new HashMap<>();
        for (long key : chunks) {
            scanChunk(level, ChunkPos.getX(key), ChunkPos.getZ(key), found);
        }
        SOURCES.put(level, found);
    }

    /** Finds the heat sources in one chunk, melts the snow around them and records them in {@code out}. */
    public static int scanChunk(ServerLevel level, int cx, int cz, Map<Long, List<long[]>> out) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
        if (chunk == null) return 0;
        int count = 0;
        for (BlockEntity be : new ArrayList<>(chunk.getBlockEntities().values())) {
            BlockPos pos = be.getBlockPos();
            int radius = heatRadius(level, pos, be.getBlockState(), be);
            if (radius <= 0) continue;
            count++;
            out.computeIfAbsent(ChunkPos.asLong(cx, cz), k -> new ArrayList<>()).add(new long[]{pos.asLong(), radius});
            melt(level, pos, radius);
        }
        return count;
    }

    /** Snow-melting radius of a block (0 = not a heat source). */
    public static int heatRadius(Level level, BlockPos pos, BlockState state, BlockEntity be) {
        Block block = state.getBlock();
        if (block instanceof CabinStoveBlock) return state.getValue(CabinStoveBlock.LIT) ? 4 : 0;
        if (state.hasProperty(BlockStateProperties.LIT) && (block == Blocks.FURNACE || block == Blocks.BLAST_FURNACE
                || block == Blocks.SMOKER || block == Blocks.CAMPFIRE || block == Blocks.SOUL_CAMPFIRE)) {
            return state.getValue(BlockStateProperties.LIT) ? 2 : 0;
        }
        var id = BuiltInRegistries.BLOCK.getKey(block);
        String ns = id.getNamespace(), path = id.getPath();
        if (ns.equals("create")) {
            switch (path) {
                case "blaze_burner":
                    return switch (propertyValue(state, "blaze")) {
                        case "smouldering" -> 2;
                        case "fading" -> 3;
                        case "kindled" -> 4;
                        case "seething" -> 6;
                        default -> 0;
                    };
                case "lit_blaze_burner":
                    return 3;
                case "steam_engine":
                    return CreateReflect.boilerHeat(be) > 0 ? 5 : 0;
                case "fluid_tank":
                    return hot(CreateReflect.fluidIn(level, pos, state)) ? 3 : 0;
                case "fluid_pipe", "encased_fluid_pipe", "glass_fluid_pipe", "smart_fluid_pipe", "mechanical_pump",
                     "fluid_valve", "basin", "item_drain", "spout", "hose_pulley", "portable_fluid_interface":
                    return hot(CreateReflect.fluidIn(level, pos, state)) ? 2 : 0;
                default:
                    return 0;
            }
        }
        if (ns.equals("create_new_age")) {
            String blaze = propertyValue(state, "blaze");
            if (blaze.equals("kindled") || blaze.equals("seething")) return blaze.equals("seething") ? 6 : 4;
            if (path.equals("heater") || path.startsWith("heat_") || path.contains("stirling") || path.contains("solar_heating")
                    || path.equals("reactor_heat_vent")) {
                return newAgeHeat(be) > 0 ? 3 : 0;
            }
        }
        if (ns.equals("cold_sweat") && (path.equals("hearth_bottom") || path.equals("boiler"))) {
            return state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT) ? 4 : 0;
        }
        return 0;
    }

    private static boolean hot(FluidStack fluid) {
        return fluid != null && !fluid.isEmpty() && fluid.getFluid().getFluidType().getTemperature(fluid) > 320;
    }

    private static String propertyValue(BlockState state, String name) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals(name)) return state.getValue(property).toString().toLowerCase(Locale.ROOT);
        }
        return "";
    }

    private static double newAgeHeat(BlockEntity be) {
        if (!newAgeInit) {
            newAgeInit = true;
            try {
                newAgeHeat = Class.forName("org.antarcticgardens.cna.content.heat.HeatBlockEntity").getMethod("getHeat");
            } catch (ReflectiveOperationException e) {
                newAgeHeat = null;
            }
        }
        if (newAgeHeat == null || be == null || !newAgeHeat.getDeclaringClass().isInstance(be)) return 0;
        try {
            return ((Number) newAgeHeat.invoke(be)).doubleValue();
        } catch (ReflectiveOperationException e) {
            return 0;
        }
    }

    /** Takes up to 3 layers off every snow pile within {@code radius} (a snow block becomes 5 layers). */
    public static void melt(ServerLevel level, BlockPos center, int radius) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int r2 = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > r2) continue;
                for (int dy = radius + 2; dy >= -3; dy--) {
                    m.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!level.isLoaded(m)) continue;
                    BlockState state = level.getBlockState(m);
                    if (state.is(Blocks.SNOW)) {
                        int layers = state.getValue(SnowLayerBlock.LAYERS) - 3;
                        level.setBlock(m, layers <= 0 ? Blocks.AIR.defaultBlockState() : state.setValue(SnowLayerBlock.LAYERS, layers), Block.UPDATE_ALL);
                        break;
                    }
                    if (state.is(Blocks.SNOW_BLOCK) && level.getBlockState(m.above()).isAir()) {
                        level.setBlock(m, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 5), Block.UPDATE_ALL);
                        break;
                    }
                    if (!state.isAir() && state.isSolidRender(level, m)) break;
                }
            }
        }
    }

    /** True if a known heat source keeps this spot free of snow. */
    public static boolean nearHeat(Level level, BlockPos pos) {
        Map<Long, List<long[]>> sources = SOURCES.get(level);
        if (sources == null || sources.isEmpty()) return false;
        int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                List<long[]> list = sources.get(ChunkPos.asLong(cx + dx, cz + dz));
                if (list == null) continue;
                for (long[] source : list) {
                    BlockPos s = BlockPos.of(source[0]);
                    int r = (int) source[1];
                    int ddx = s.getX() - pos.getX(), ddz = s.getZ() - pos.getZ(), ddy = pos.getY() - s.getY();
                    if (ddx * ddx + ddz * ddz <= r * r && ddy >= -3 && ddy <= r + 3) return true;
                }
            }
        }
        return false;
    }

    /** For tests: replace the remembered sources of a level. */
    public static void remember(Level level, Map<Long, List<long[]>> sources) {
        SOURCES.put(level, sources);
    }
}
