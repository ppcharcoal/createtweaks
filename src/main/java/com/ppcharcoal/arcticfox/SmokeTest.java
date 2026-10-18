package com.ppcharcoal.arcticfox;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Dev/CI only (enabled with -Darcticfox.smoketest=true): samples the generated overworld and
 * prints biome, ocean and terrain statistics, checks that removed content is really gone, then
 * stops the server.
 */
public final class SmokeTest {
    public static final boolean ENABLED = Boolean.getBoolean("arcticfox.smoketest");
    private static final String P = "[ArcticFox/SMOKE] ";

    private SmokeTest() {
    }

    public static void run(MinecraftServer server) {
        boolean ok = true;
        try {
            ServerLevel level = server.overworld();
            ChunkGenerator generator = level.getChunkSource().getGenerator();
            RandomState random = level.getChunkSource().randomState();
            BiomeSource source = generator.getBiomeSource();
            int sea = generator.getSeaLevel();
            log("generator=%s biomeSource=%s seaLevel=%d", generator.getClass().getSimpleName(), source.getClass().getName(), sea);

            Map<String, Integer> biomes = new TreeMap<>();
            int samples = 0, oceans = 0, nonSnowy = 0, below = 0;
            long sum = 0, sumSq = 0;
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
            int flatColumns = 0;
            final int radius = 6144, step = 96;
            for (int x = -radius; x <= radius; x += step) {
                for (int z = -radius; z <= radius; z += step) {
                    int h = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, random);
                    int y = Math.max(h, sea);
                    Holder<Biome> b = source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), random.sampler());
                    String name = b.unwrapKey().map(k -> k.location().toString()).orElse("?");
                    biomes.merge(name, 1, Integer::sum);
                    samples++;
                    if (b.is(BiomeTags.IS_OCEAN)) oceans++;
                    if (!b.value().coldEnoughToSnow(new BlockPos(x, y, z))) nonSnowy++;
                    if (h < sea) below++;
                    if (!b.is(BiomeTags.IS_OCEAN) && !b.is(BiomeTags.IS_MOUNTAIN)) {
                        if (h >= sea - 2 && h <= sea + 24) flatColumns++;
                    }
                    sum += h;
                    sumSq += (long) h * h;
                    min = Math.min(min, h);
                    max = Math.max(max, h);
                }
            }
            double mean = (double) sum / samples;
            double sd = Math.sqrt((double) sumSq / samples - mean * mean);
            log("samples=%d oceanBiomes=%.1f%% terrainBelowSea=%.1f%% nonSnowySurface=%d", samples,
                    100.0 * oceans / samples, 100.0 * below / samples, nonSnowy);
            log("height mean=%.1f sd=%.1f min=%d max=%d lowlandWithinSea+24=%.1f%%", mean, sd, min, max, 100.0 * flatColumns / samples);
            biomes.forEach((k, v) -> log("  biome %-32s %5.1f%%", k, 100.0 * v / samples));
            if (nonSnowy > samples / 50) {
                log("FAIL: too many non-snowy surface samples");
                ok = false;
            }

            // Floating island / overhang scan: solid block above an air gap of >=4, above sea level+12,
            // with the solid run touching open sky above it (i.e. not a cave roof inside a mountain).
            int columns = 0, floating = 0;
            for (int x = -2048; x <= 2048; x += 64) {
                for (int z = -2048; z <= 2048; z += 64) {
                    NoiseColumn col = generator.getBaseColumn(x, z, level, random);
                    columns++;
                    int top = level.getMaxBuildHeight() - 1;
                    int y = top;
                    while (y > sea && isAir(col.getBlock(y))) y--;
                    if (y <= sea + 12) continue;
                    int solidStart = y;
                    while (y > sea && !isAir(col.getBlock(y))) y--;
                    int gapTop = y;
                    while (y > sea && isAir(col.getBlock(y))) y--;
                    int gap = gapTop - y;
                    if (gap >= 4 && solidStart - gapTop <= 24 && y > sea) floating++;
                }
            }
            log("overhang/floating columns: %d of %d (%.2f%%)", floating, columns, 100.0 * floating / columns);

            // Removed content
            for (String id : List.of("minecraft:nether/root", "minecraft:end/root", "minecraft:story/enter_the_nether",
                    "minecraft:adventure/trade", "minecraft:husbandry/balanced_diet")) {
                boolean present = server.getAdvancements().get(ResourceLocation.parse(id)) != null;
                log("advancement %s present=%s (expected false)", id, present);
                if (present) ok = false;
            }
            for (String id : List.of("minecraft:story/upgrade_tools", "minecraft:adventure/adventuring_time")) {
                boolean present = server.getAdvancements().get(ResourceLocation.parse(id)) != null;
                log("advancement %s present=%s (expected true)", id, present);
                if (!present) ok = false;
            }
            log("advancements loaded: %d", server.getAdvancements().getAllAdvancements().size());
            boolean woodenRecipe = server.getRecipeManager().byKey(ResourceLocation.parse("minecraft:wooden_pickaxe")).isPresent();
            boolean flintRecipe = server.getRecipeManager().byKey(ResourceLocation.parse("minecraft:stone_pickaxe")).isPresent();
            boolean copperRecipe = server.getRecipeManager().byKey(ArcticFox.id("copper_pickaxe")).isPresent();
            log("recipes: wooden_pickaxe=%s (false) flint/stone_pickaxe=%s (true) copper_pickaxe=%s (true)", woodenRecipe, flintRecipe, copperRecipe);
            if (woodenRecipe || !flintRecipe || !copperRecipe) ok = false;

            BlockState iron = Blocks.IRON_ORE.defaultBlockState();
            boolean flintMinesIron = Items.STONE_PICKAXE.getDefaultInstance().isCorrectToolForDrops(iron);
            boolean flintMinesCopper = Items.STONE_PICKAXE.getDefaultInstance().isCorrectToolForDrops(Blocks.COPPER_ORE.defaultBlockState());
            boolean copperMinesIron = com.ppcharcoal.arcticfox.registry.ModItems.COPPER_PICKAXE.get().getDefaultInstance().isCorrectToolForDrops(iron);
            boolean copperMinesDiamond = com.ppcharcoal.arcticfox.registry.ModItems.COPPER_PICKAXE.get().getDefaultInstance().isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState());
            log("mining: flint->iron=%s (false) flint->copper=%s (true) copper->iron=%s (true) copper->diamond=%s (false)",
                    flintMinesIron, flintMinesCopper, copperMinesIron, copperMinesDiamond);
            if (flintMinesIron || !flintMinesCopper || !copperMinesIron || copperMinesDiamond) ok = false;

            boolean scoriaTag = Blocks.COBBLESTONE.defaultBlockState().is(ModTags.FLUID_GENERATOR_OUTPUTS);
            log("cobblestone in fluid_generator_outputs=%s (true)", scoriaTag);
            if (!scoriaTag) ok = false;

            BlockPos village = level.findNearestMapStructure(StructureTags.VILLAGE, BlockPos.ZERO, 64, false);
            log("nearest village within 64 chunks: %s (expected null)", village);
            if (village != null) ok = false;
            log("structure registry size: %d, biome registry has %d entries", level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).size(),
                    level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).size());
            log("waterSourceConversion=%s doPatrolSpawning=%s doTraderSpawning=%s",
                    server.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_WATER_SOURCE_CONVERSION),
                    server.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DO_PATROL_SPAWNING),
                    server.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DO_TRADER_SPAWNING));
            log("scoria block registered: %s", BuiltInRegistries.BLOCK.containsKey(ArcticFox.id("scoria")));
        } catch (Throwable t) {
            ArcticFox.LOGGER.error(P + "smoke test crashed", t);
            ok = false;
        }
        log(ok ? "ARCTICFOX SMOKETEST PASS" : "ARCTICFOX SMOKETEST FAIL");
        server.halt(false);
    }

    private static boolean isAir(BlockState state) {
        return state.isAir() || !state.getFluidState().isEmpty();
    }

    private static void log(String fmt, Object... args) {
        ArcticFox.LOGGER.info(P + String.format(java.util.Locale.ROOT, fmt, args));
    }
}
