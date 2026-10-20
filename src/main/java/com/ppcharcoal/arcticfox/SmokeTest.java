package com.ppcharcoal.arcticfox;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
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
                    if (!b.is(BiomeTags.IS_OCEAN) && !b.value().coldEnoughToSnow(new BlockPos(x, y, z))) nonSnowy++;
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
            log("samples=%d oceanBiomes=%.1f%% terrainBelowSea=%.1f%% nonSnowyLandSurface=%d", samples,
                    100.0 * oceans / samples, 100.0 * below / samples, nonSnowy);
            log("height mean=%.1f sd=%.1f min=%d max=%d lowlandWithinSea+24=%.1f%%", mean, sd, min, max, 100.0 * flatColumns / samples);
            final int total = samples;
            biomes.forEach((k, v) -> log("  biome %-32s %5.1f%%", k, 100.0 * v / total));
            if (nonSnowy > samples / 50) {
                log("FAIL: too many non-snowy surface samples");
                ok = false;
            }
            double forest = 100.0 * (biomes.getOrDefault("minecraft:snowy_taiga", 0) + biomes.getOrDefault("minecraft:grove", 0)) / samples;
            double wastes = 100.0 * biomes.getOrDefault("arcticfox:frozen_wastes", 0) / samples;
            log("forest biomes=%.1f%% (expected < 12%%) frozen wastes=%.1f%% (expected > 35%%)", forest, wastes);
            if (forest >= 12 || wastes <= 35) ok = false;

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
            boolean flintRecipe = server.getRecipeManager().byKey(ResourceLocation.parse("minecraft:stone_pickaxe"))
                    .map(h -> h.value().getIngredients().stream().anyMatch(i -> i.test(Items.FLINT.getDefaultInstance())))
                    .orElse(false);
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

            BlockState fluidResult = net.neoforged.neoforge.event.EventHooks.fireFluidPlaceBlockEvent(level, BlockPos.ZERO, BlockPos.ZERO, Blocks.COBBLESTONE.defaultBlockState());
            log("lava+water in world produces: %s (expected arcticfox:scoria)", BuiltInRegistries.BLOCK.getKey(fluidResult.getBlock()));
            if (!fluidResult.is(com.ppcharcoal.arcticfox.registry.ModBlocks.SCORIA.get())) ok = false;

            ok &= checkNewFeatures(server, level);
            ok &= checkWinter(server, level);
            ok &= checkV12(server, level);
            ok &= checkV13(server, level);
            ok &= checkCompat(server, level);
            ok &= checkSnowMelt(level);
            ok &= checkWendigo(level);
            ItemAudit.run(server, level);
            int hiddenCount = 0;
            for (var h : BuiltInRegistries.ITEM.getTagOrEmpty(net.neoforged.neoforge.common.Tags.Items.HIDDEN_FROM_RECIPE_VIEWERS)) hiddenCount++;
            boolean bigMods = net.neoforged.fml.ModList.get().isLoaded("alexsmobs") && net.neoforged.fml.ModList.get().isLoaded("mowziesmobs");
            log("items hidden from recipe viewers: %d (expected > 200 with Alex's Mobs + Mowzie's, > 30 without)", hiddenCount);
            if (hiddenCount < (bigMods ? 200 : 30)) ok = false;
        } catch (Throwable t) {
            ArcticFox.LOGGER.error(P + "smoke test crashed", t);
            ok = false;
        }
        log(ok ? "ARCTICFOX SMOKETEST PASS" : "ARCTICFOX SMOKETEST FAIL");
        server.halt(false);
    }

    private static boolean checkNewFeatures(MinecraftServer server, ServerLevel level) {
        boolean ok = true;
        // --- ores: generate a patch of chunks and measure how many ore blocks touch air/fluid
        java.util.Set<net.minecraft.world.level.block.Block> ores = java.util.Set.of(
                Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
                Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
                Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
                Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.RAW_IRON_BLOCK, Blocks.RAW_COPPER_BLOCK);
        int total = 0, exposed = 0, rawBlocks = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int cx = 20; cx < 26; cx++) {
            for (int cz = 20; cz < 26; cz++) {
                net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunk(cx, cz);
                for (int x = 1; x < 15; x++) for (int z = 1; z < 15; z++) for (int y = -63; y < 200; y++) {
                    m.set(chunk.getPos().getMinBlockX() + x, y, chunk.getPos().getMinBlockZ() + z);
                    BlockState st = chunk.getBlockState(m);
                    if (!ores.contains(st.getBlock())) continue;
                    if (st.is(Blocks.RAW_IRON_BLOCK) || st.is(Blocks.RAW_COPPER_BLOCK)) { rawBlocks++; continue; }
                    total++;
                    for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
                        BlockState n = level.getBlockState(m.relative(d));
                        if (n.isAir() || !n.getFluidState().isEmpty()) { exposed++; break; }
                    }
                }
            }
        }
        double exposedPct = total == 0 ? 0 : 100.0 * exposed / total;
        log("ores in 36 chunks: %d, exposed to air/fluid: %.1f%% (expected > 50%%), raw ore vein blocks: %d (expected 0)", total, exposedPct, rawBlocks);
        if (total == 0 || exposedPct < 50 || rawBlocks > 0) ok = false;

        // --- evaporation: a placed water source is scheduled, a waterlogged grate is not
        BlockPos base = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(8, 0, 8)).above(3);
        int before = com.ppcharcoal.arcticfox.world.Evaporation.get(level).size();
        level.setBlock(base, Blocks.WATER.defaultBlockState(), 3);
        int afterWater = com.ppcharcoal.arcticfox.world.Evaporation.get(level).size();
        BlockPos grate = base.east(3);
        level.setBlock(grate, Blocks.WAXED_COPPER_GRATE.defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), 3);
        int afterGrate = com.ppcharcoal.arcticfox.world.Evaporation.get(level).size();
        log("evaporation: water source scheduled=%s (true) waterlogged grate scheduled=%s (false)", afterWater == before + 1, afterGrate != afterWater);
        if (afterWater != before + 1 || afterGrate != afterWater) ok = false;

        if (net.neoforged.fml.ModList.get().isLoaded("create")) {
            try {
                Class<?> fanType = Class.forName("com.simibubi.create.content.kinetics.fan.processing.FanProcessingType");
                Object type = fanType.getMethod("getAt", net.minecraft.world.level.Level.class, BlockPos.class).invoke(null, level, grate);
                log("create: fan processing type at waterlogged copper grate = %s (expected splashing)", type == null ? "none" : type.getClass().getSimpleName());
                if (type == null || !type.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT).contains("splash")) ok = false;
            } catch (Throwable t) {
                ArcticFox.LOGGER.error(P + "fan check failed", t);
                ok = false;
            }
        }

        BlockPos stronghold = level.findNearestMapStructure(StructureTags.EYE_OF_ENDER_LOCATED, BlockPos.ZERO, 100, false);
        log("nearest stronghold within 100 chunks: %s (expected null)", stronghold);
        if (stronghold != null) ok = false;

        // --- recipes & animals
        boolean smoking = server.getRecipeManager().byKey(ArcticFox.id("leather_from_smoking_rotten_flesh"))
                .map(h -> h.value().getType() == net.minecraft.world.item.crafting.RecipeType.SMOKING).orElse(false);
        log("smoker rotten flesh -> leather recipe present=%s (true)", smoking);
        if (!smoking) ok = false;
        var snowyPlains = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                .getHolderOrThrow(net.minecraft.world.level.biome.Biomes.SNOWY_PLAINS);
        var creatures = snowyPlains.value().getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.CREATURE).unwrap();
        boolean cow = creatures.stream().anyMatch(d -> d.type == com.ppcharcoal.arcticfox.registry.ModEntities.WINTER_COW.get());
        boolean sheep = creatures.stream().anyMatch(d -> d.type == com.ppcharcoal.arcticfox.registry.ModEntities.WINTER_SHEEP.get());
        log("snowy plains spawns winter cow=%s winter sheep=%s (true/true)", cow, sheep);
        if (!cow || !sheep) ok = false;
        var spawned = com.ppcharcoal.arcticfox.registry.ModEntities.WINTER_SHEEP.get().create(level);
        log("winter sheep entity creatable=%s", spawned != null);
        if (spawned == null) ok = false;

        if (net.neoforged.fml.ModList.get().isLoaded("sereneseasons")) {
            try {
                boolean merged = java.util.Arrays.stream(Class.forName("sereneseasons.season.SeasonHooks").getDeclaredMethods())
                        .anyMatch(mth -> mth.getName().contains("sprinklerRain"));
                log("SeasonHooks mixin applied=%s (true)", merged);
                if (!merged) ok = false;
            } catch (Throwable t) {
                ArcticFox.LOGGER.error(P + "mixin check failed", t);
                ok = false;
            }
            log("sprinkler rain compat: %s, isRainingAt(spawn)=%s", com.ppcharcoal.arcticfox.compat.SprinklerRain.describe(),
                    level.isRainingAt(base));
        }
        return ok;
    }

    private static boolean checkWinter(MinecraftServer server, ServerLevel level) {
        boolean ok = true;
        // --- frozen wastes surface is rocky / dirt
        java.util.Map<String, Integer> surface = new TreeMap<>();
        int wasteCols = 0;
        for (int x = -3000; x <= 3000; x += 48) {
            for (int z = -3000; z <= 3000; z += 48) {
                if (!level.getBiome(new BlockPos(x, 70, z)).is(ResourceLocation.parse("arcticfox:frozen_wastes"))) continue;
                NoiseColumn col = level.getChunkSource().getGenerator().getBaseColumn(x, z, level, level.getChunkSource().randomState());
                int h = level.getChunkSource().getGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, level.getChunkSource().randomState());
                if (wasteCols++ > 60) break;
                level.getChunk(x >> 4, z >> 4); // fully generate so surface rules ran
                BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z)).below();
                BlockState st = level.getBlockState(top);
                if (st.is(Blocks.SNOW) || st.is(Blocks.SNOW_BLOCK)) st = level.getBlockState(top.below());
                surface.merge(BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath(), 1, Integer::sum);
            }
            if (wasteCols > 60) break;
        }
        log("frozen wastes ground under the snow: %s", surface);
        int grass = surface.getOrDefault("grass_block", 0);
        if (wasteCols == 0 || grass * 2 > wasteCols) ok = false;

        // --- night snowstorm
        long saved = level.getDayTime();
        level.setDayTime(saved - saved % 24000 + 14000);
        com.ppcharcoal.arcticfox.world.WinterWeather.updateWeather(level);
        boolean storm = level.getLevelData().isRaining() && level.getLevelData().isThundering();
        level.setDayTime(saved - saved % 24000 + 1000);
        com.ppcharcoal.arcticfox.world.WinterWeather.updateWeather(level);
        boolean clearedAtDawn = !level.getLevelData().isThundering();
        level.setDayTime(saved);
        log("night snowstorm=%s (true) cleared at dawn=%s (true)", storm, clearedAtDawn);
        if (!storm || !clearedAtDawn) ok = false;

        // --- snow stacking: grows past one block but stops at 3
        level.getChunk(40 >> 4, 40 >> 4);
        BlockPos base = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(40, 0, 40)).above(10);
        for (int dy = -1; dy < 6; dy++) level.setBlock(base.above(dy), dy == -1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
        int added = 0;
        for (int i = 0; i < 40; i++) if (com.ppcharcoal.arcticfox.world.WinterWeather.addLayer(level, level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, base).below())) added++;
        int snowBlocks = 0;
        for (int dy = 0; dy < 5; dy++) if (level.getBlockState(base.above(dy)).is(Blocks.SNOW_BLOCK)) snowBlocks++;
        BlockState topLayer = level.getBlockState(base.above(2));
        int depthBlocks = Config.SNOW_MAX_DEPTH.get();
        topLayer = level.getBlockState(base.above(depthBlocks - 1));
        log("snow stacking: layers added=%d (expected %d) snow blocks=%d (%d) top=%s", added, depthBlocks * 8, snowBlocks, depthBlocks - 1, topLayer);
        if (added != depthBlocks * 8 || snowBlocks != depthBlocks - 1 || !topLayer.is(Blocks.SNOW)) ok = false;
        BlockPos glass = base.east(4);
        level.setBlock(glass, Blocks.GLASS.defaultBlockState(), 3);
        level.setBlock(glass.above(), Blocks.AIR.defaultBlockState(), 3);
        boolean onGlass = com.ppcharcoal.arcticfox.world.WinterWeather.addLayer(level, glass);
        log("snow settles on glass=%s (false)", onGlass);
        if (onGlass) ok = false;

        // --- crops: exposed surface farm never grows, sealed underground farm does
        level.getChunk(-40 >> 4, -40 >> 4);
        BlockPos field = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(-40, 0, -40)).above(2);
        level.setBlock(field.below(), Blocks.FARMLAND.defaultBlockState().setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE, 7), 3);
        level.setBlock(field, Blocks.WHEAT.defaultBlockState(), 3);
        var exposed = com.ppcharcoal.arcticfox.world.CropRules.check(level, field, level.getBlockState(field));
        BlockPos cave = new BlockPos(-40, -30, -40);
        for (BlockPos p : BlockPos.betweenClosed(cave.offset(-2, -2, -2), cave.offset(2, 3, 2))) level.setBlock(p, Blocks.STONE.defaultBlockState(), 3);
        for (BlockPos p : BlockPos.betweenClosed(cave.offset(-1, 0, -1), cave.offset(1, 1, 1))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(cave.below(), Blocks.FARMLAND.defaultBlockState().setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE, 7), 3);
        level.setBlock(cave, Blocks.WHEAT.defaultBlockState(), 3);
        var underground = com.ppcharcoal.arcticfox.world.CropRules.check(level, cave, level.getBlockState(cave));
        level.setBlock(cave.below(), Blocks.FARMLAND.defaultBlockState(), 3);
        var dry = com.ppcharcoal.arcticfox.world.CropRules.check(level, cave, level.getBlockState(cave));
        Double temp = com.ppcharcoal.arcticfox.compat.ColdSweatCompat.temperatureCelsius(level, field);
        Double caveTemp = com.ppcharcoal.arcticfox.compat.ColdSweatCompat.temperatureCelsius(level, cave);
        log("crops: open sky=%s (EXPOSED) sealed cave=%s (OK) dry farmland=%s (DRY) | cold sweat surface=%s C cave=%s C",
                exposed, underground, dry, temp == null ? "n/a" : String.format(java.util.Locale.ROOT, "%.1f", temp),
                caveTemp == null ? "n/a" : String.format(java.util.Locale.ROOT, "%.1f", caveTemp));
        if (exposed != com.ppcharcoal.arcticfox.world.CropRules.Verdict.EXPOSED
                || underground != com.ppcharcoal.arcticfox.world.CropRules.Verdict.OK
                || dry != com.ppcharcoal.arcticfox.world.CropRules.Verdict.DRY) ok = false;
        if (temp != null) {
            long keep = level.getDayTime();
            level.setWeatherParameters(24000, 0, false, false);
            level.setRainLevel(0); level.setThunderLevel(0);
            double sumNoon = 0, sumNight = 0; int n = 0;
            for (int i = 0; i < 12; i++) {
                int x = -1500 + (i % 4) * 1000, z = -1000 + (i / 4) * 1000;
                level.getChunk(x >> 4, z >> 4);
                BlockPos open = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z)).above();
                if (level.getBiome(open).is(BiomeTags.IS_OCEAN)) continue;
                level.setDayTime(keep - keep % 24000 + 6000);
                Double noon = com.ppcharcoal.arcticfox.compat.ColdSweatCompat.temperatureCelsius(level, open, true);
                level.setDayTime(keep - keep % 24000 + 18000);
                Double midnight = com.ppcharcoal.arcticfox.compat.ColdSweatCompat.temperatureCelsius(level, open, true);
                log("  cold sweat %s %s: noon=%.1f C midnight=%.1f C", open.toShortString(),
                        level.getBiome(open).unwrapKey().map(k -> k.location().getPath()).orElse("?"), noon, midnight);
                sumNoon += noon; sumNight += midnight; n++;
            }
            level.setDayTime(keep);
            double noonAvg = sumNoon / Math.max(1, n), nightAvg = sumNight / Math.max(1, n);
            log("cold sweat clear-weather surface average over %d spots: noon=%.1f C midnight=%.1f C (expected -15..-9 / -21..-15)", n, noonAvg, nightAvg);
            if (n == 0 || noonAvg > -9 || noonAvg < -15 || nightAvg > -15 || nightAvg < -21) ok = false;
        }

        // --- spears
        var spear = com.ppcharcoal.arcticfox.registry.ModItems.IRON_SPEAR.get().getDefaultInstance();
        double reach = spear.getAttributeModifiers().modifiers().stream()
                .filter(e -> e.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE))
                .mapToDouble(e -> e.modifier().amount()).sum();
        boolean recipes = server.getRecipeManager().byKey(ArcticFox.id("iron_spear")).isPresent()
                && server.getRecipeManager().byKey(ArcticFox.id("diamond_spear")).isPresent()
                && server.getRecipeManager().byKey(ArcticFox.id("netherite_spear_smithing")).isPresent();
        log("spears: reach bonus=%.1f (1.5) recipes=%s (true)", reach, recipes);
        if (reach != 1.5 || !recipes) ok = false;

        if (net.neoforged.fml.ModList.get().isLoaded("mowziesmobs")) {
            var structures = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
            for (String id : List.of("frostmaw_spawn", "wrought_chamber", "umvuthana_grove", "monastery")) {
                var holder = structures.getHolder(ResourceLocation.fromNamespaceAndPath("mowziesmobs", id));
                if (holder.isEmpty()) { log("mowzie structure %s missing", id); ok = false; continue; }
                var found = level.getChunkSource().getGenerator().findNearestMapStructure(level,
                        net.minecraft.core.HolderSet.direct(holder.get()), BlockPos.ZERO, 64, false);
                boolean expected = id.equals("frostmaw_spawn");
                log("mowzie %s found within 64 chunks=%s (expected %s)", id, found != null, expected);
                if ((found != null) != expected) ok = false;
            }
        }
        return ok;
    }

    private static boolean checkV12(MinecraftServer server, ServerLevel level) {
        boolean ok = true;
        var mods = net.neoforged.fml.ModList.get();
        // --- spawn near (a few) trees
        BlockPos spawn = level.getSharedSpawnPos();
        String spawnBiome = level.getBiome(spawn).unwrapKey().map(k -> k.location().toString()).orElse("?");
        int trees = com.ppcharcoal.arcticfox.world.SpawnSelector.countLogs(level, spawn, 24);
        log("world spawn %s in %s with %d tree trunks within 24 blocks (expected snowy_plains, 1..60)", spawn.toShortString(), spawnBiome, trees);
        if (!spawnBiome.equals("minecraft:snowy_plains") || trees < 1 || trees > 60) ok = false;

        // --- no ruined portals; new structures exist and are findable
        BlockPos portal = level.findNearestMapStructure(StructureTags.RUINED_PORTAL, BlockPos.ZERO, 64, false);
        log("nearest ruined portal within 64 chunks: %s (expected null)", portal);
        if (portal != null) ok = false;
        var structures = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        List<String> ours = new java.util.ArrayList<>(List.of("buried_camp", "supply_cache", "cabin"));
        String deco = mods.isLoaded("createdeco") ? "_deco" : "";
        if (mods.isLoaded("create")) { ours.add("train_station" + deco); ours.add("metro_station" + deco); }
        for (String id : ours) {
            var holder = structures.getHolder(ArcticFox.id(id));
            var template = server.getStructureManager().get(ArcticFox.id(id));
            if (holder.isEmpty() || template.isEmpty()) { log("structure %s missing (holder=%s template=%s)", id, holder.isPresent(), template.isPresent()); ok = false; continue; }
            BlockPos found = level.getChunkSource().getGenerator().findNearestMapStructure(level,
                    net.minecraft.core.HolderSet.direct(holder.get()), BlockPos.ZERO, 100, false) == null ? null
                    : level.getChunkSource().getGenerator().findNearestMapStructure(level, net.minecraft.core.HolderSet.direct(holder.get()), BlockPos.ZERO, 100, false).getFirst();
            // place a copy in the sky and check its chests carry loot tables
            BlockPos at = new BlockPos(2000 + ours.indexOf(id) * 40, 200, 2000);
            level.getChunk(at.getX() >> 4, at.getZ() >> 4);
            var t = template.get();
            var settings = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings();
            if (id.startsWith("train") || id.startsWith("metro")) {
                level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.PROCESSOR_LIST)
                        .getOptional(ArcticFox.id("rail_swaps")).ifPresent(list -> list.list().forEach(settings::addProcessor));
            }
            t.placeInWorld(level, at, at, settings, level.random, 2);
            int lootChests = 0, createTracks = 0, railwaysTracks = 0, vanillaRails = 0, lamps = 0;
            for (BlockPos p : BlockPos.betweenClosed(at, at.offset(t.getSize()).offset(-1, -1, -1))) {
                if (level.getBlockEntity(p) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity c && c.getLootTable() != null) lootChests++;
                BlockState st = level.getBlockState(p);
                String key = BuiltInRegistries.BLOCK.getKey(st.getBlock()).toString();
                if (key.equals("create:track")) createTracks++;
                if (key.startsWith("railways:track")) railwaysTracks++;
                if (st.is(Blocks.RAIL)) vanillaRails++;
                if (st.is(com.ppcharcoal.arcticfox.registry.ModBlocks.FLICKERING_LAMP.get())) lamps++;
            }
            boolean dry = true;
            if (found != null && !id.startsWith("metro")) {
                var gen = level.getChunkSource().getGenerator();
                int surface = gen.getBaseHeight(found.getX(), found.getZ(), Heightmap.Types.WORLD_SURFACE_WG, level, level.getChunkSource().randomState());
                int floor = gen.getBaseHeight(found.getX(), found.getZ(), Heightmap.Types.OCEAN_FLOOR_WG, level, level.getChunkSource().randomState());
                dry = surface == floor;
            }
            log("structure %s: size=%s nearest=%s on dry land=%s loot chests=%d create tracks=%d S&R tracks=%d vanilla rails=%d lamps=%d",
                    id, t.getSize().toShortString(), found == null ? "none within 100 chunks" : found.toShortString(), dry, lootChests,
                    createTracks, railwaysTracks, vanillaRails, lamps);
            if (found == null || lootChests == 0 || !dry || vanillaRails > 0) ok = false;
            if ((id.startsWith("train") || id.startsWith("metro")) && (createTracks + railwaysTracks == 0 || lamps == 0)) ok = false;
            if (mods.isLoaded("railways") && createTracks > 0) ok = false;
            if (id.equals("cabin")) {
                // warmth test on real ground (the sky copy above is at high altitude, which Cold Sweat makes colder)
                level.getChunk(2400 >> 4, 2400 >> 4);
                BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(2400, 0, 2400)).below();
                t.placeInWorld(level, ground, ground, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(), level.random, 2);
                ok &= checkCabinWarmth(level, ground, t.getSize());
            }
        }

        // --- weather: sunny/snowy days, blizzard nights
        long keep = level.getDayTime();
        int snowy = 0, sunny = 0; boolean weatherOk = true;
        for (int d = 0; d < 20; d++) {
            level.setDayTime(d * 24000L + 6000);
            com.ppcharcoal.arcticfox.world.WinterWeather.updateWeather(level);
            boolean wantSnow = com.ppcharcoal.arcticfox.world.WinterWeather.isSnowyDay(level, d);
            boolean raining = level.getLevelData().isRaining(), thunder = level.getLevelData().isThundering();
            if (wantSnow) snowy++; else sunny++;
            if (raining != wantSnow || thunder) weatherOk = false;
            level.setDayTime(d * 24000L + 18000);
            com.ppcharcoal.arcticfox.world.WinterWeather.updateWeather(level);
            if (!level.getLevelData().isRaining() || !level.getLevelData().isThundering()) weatherOk = false;
        }
        level.setDayTime(keep);
        log("weather over 20 days: %d snowy days, %d sunny days, every night a blizzard, applied correctly=%s", snowy, sunny, weatherOk);
        if (!weatherOk || snowy == 0 || sunny == 0) ok = false;

        // --- raw food
        var fake = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        boolean sick = false;
        for (int i = 0; i < 20 && !sick; i++) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish(
                    fake, Items.BEEF.getDefaultInstance(), 0, net.minecraft.world.item.ItemStack.EMPTY));
            sick = fake.hasEffect(net.minecraft.world.effect.MobEffects.HUNGER);
        }
        fake.removeAllEffects();
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish(
                fake, Items.COOKED_BEEF.getDefaultInstance(), 0, net.minecraft.world.item.ItemStack.EMPTY));
        boolean cookedSick = fake.hasEffect(net.minecraft.world.effect.MobEffects.HUNGER);
        log("raw beef makes you sick=%s (true) cooked beef=%s (false)", sick, cookedSick);
        if (!sick || cookedSick) ok = false;

        // --- fishing: natural water yes, a bucket pond / waterlogged pond no
        BlockPos natural = null;
        for (int x = -2000; x <= 2000 && natural == null; x += 64) {
            for (int z = -2000; z <= 2000 && natural == null; z += 64) {
                level.getChunk(x >> 4, z >> 4);
                BlockPos top = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(x, 0, z)).below();
                if (level.getFluidState(top).is(net.minecraft.tags.FluidTags.WATER)) natural = top;
                else if (level.getBlockState(top).is(net.minecraft.tags.BlockTags.ICE) && level.getFluidState(top.below()).is(net.minecraft.tags.FluidTags.WATER)) natural = top.below();
            }
        }
        BlockPos pond = new BlockPos(2300, 210, 2300);
        level.getChunk(pond.getX() >> 4, pond.getZ() >> 4);
        for (BlockPos p : BlockPos.betweenClosed(pond.offset(-2, -1, -2), pond.offset(2, 0, 2))) level.setBlock(p, Blocks.STONE.defaultBlockState(), 3);
        for (BlockPos p : BlockPos.betweenClosed(pond.offset(-1, 0, -1), pond.offset(1, 0, 1))) level.setBlock(p, Blocks.WATER.defaultBlockState(), 3);
        BlockPos slabs = pond.east(8);
        for (BlockPos p : BlockPos.betweenClosed(slabs.offset(-1, 0, -1), slabs.offset(1, 0, 1))) level.setBlock(p.immutable(),
                Blocks.STONE_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), 3);
        boolean naturalOk = natural != null && com.ppcharcoal.arcticfox.world.Fishing.isNaturalWater(level, natural);
        boolean bucketPond = com.ppcharcoal.arcticfox.world.Fishing.isNaturalWater(level, pond);
        boolean slabPond = com.ppcharcoal.arcticfox.world.Fishing.isNaturalWater(level, slabs);
        log("fishing: natural water at %s=%s (true) bucket pond=%s (false) waterlogged pond=%s (false)",
                natural == null ? "-" : natural.toShortString(), naturalOk, bucketPond, slabPond);
        if (!naturalOk || bucketPond || slabPond) ok = false;

        // --- serene seasons winter crops tag
        var winterCrops = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, ResourceLocation.parse("sereneseasons:winter_crops"));
        log("carrots are a Serene Seasons winter crop=%s", Blocks.CARROTS.defaultBlockState().is(winterCrops));
        if (!Blocks.CARROTS.defaultBlockState().is(winterCrops)) ok = false;

        if (mods.isLoaded("mowziesmobs")) {
            var set = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE_SET).get(ResourceLocation.parse("mowziesmobs:frostmaw_spawns"));
            int spacing = set != null && set.placement() instanceof net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement r ? r.spacing() : -1;
            log("frostmaw spawn spacing=%d chunks (expected 96)", spacing);
            if (spacing != 96) ok = false;
        }
        if (mods.isLoaded("farmersdelight")) {
            var cabin = server.reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, ArcticFox.id("chests/cabin")));
            boolean fdSeeds = false;
            for (int i = 0; i < 60 && !fdSeeds; i++) {
                var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                        .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
                for (var stack : cabin.getRandomItems(params)) {
                    if (BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("farmersdelight")) fdSeeds = true;
                }
            }
            log("farmer's delight seeds found in cabin loot=%s (true)", fdSeeds);
            if (!fdSeeds) ok = false;
        }
        if (mods.isLoaded("create") && com.ppcharcoal.arcticfox.compat.ColdSweatCompat.loaded()) {
            ok &= checkCreateHeat(level);
        }
        return ok;
    }

    /** The cabin's stove must be burning, and with Cold Sweat the inside must be above freezing at midnight. */
    private static boolean checkCabinWarmth(ServerLevel level, BlockPos at, net.minecraft.core.Vec3i size) {
        boolean ok = true;
        BlockPos stovePos = null;
        for (BlockPos p : BlockPos.betweenClosed(at, at.offset(size).offset(-1, -1, -1))) {
            if (level.getBlockEntity(p) instanceof com.ppcharcoal.arcticfox.block.CabinStoveBlockEntity) stovePos = p.immutable();
        }
        int fuel = stovePos == null ? 0 : ((com.ppcharcoal.arcticfox.block.CabinStoveBlockEntity) level.getBlockEntity(stovePos)).getFuel();
        log("cabin stove found=%s fuel=%d ticks (expected 48000) lit=%s", stovePos != null, fuel,
                stovePos != null && level.getBlockState(stovePos).getValue(com.ppcharcoal.arcticfox.block.CabinStoveBlock.LIT));
        if (stovePos == null || fuel < 40000) ok = false;
        if (com.ppcharcoal.arcticfox.compat.ColdSweatCompat.loaded()) {
            long keep = level.getDayTime();
            level.setDayTime(keep - keep % 24000 + 18000);
            BlockPos inside = at.offset(5, 2, 5);
            BlockPos outside = at.offset(5, 2, -30);
            double in = com.ppcharcoal.arcticfox.compat.ColdSweatCompat.temperatureCelsius(level, inside, true);
            double out = com.ppcharcoal.arcticfox.compat.ColdSweatCompat.temperatureCelsius(level, outside, true);
            level.setDayTime(keep);
            log("cabin at midnight: %.1f C inside vs %.1f C outside (expected inside > 10)", in, out);
            if (in <= 10) ok = false;
        }
        return ok;
    }

    private static boolean checkV13(MinecraftServer server, ServerLevel level) {
        boolean ok = true;
        var mods = net.neoforged.fml.ModList.get();
        var recipes = server.getRecipeManager();
        java.util.function.Predicate<String> has = id -> recipes.byKey(ResourceLocation.parse(id)).isPresent();
        if (mods.isLoaded("create")) {
            boolean totem = has.test("arcticfox:mechanical_crafting/totem_of_undying");
            boolean mending = has.test(mods.isLoaded("create_enchantment_industry") ? "arcticfox:sequenced_assembly/mending_book" : "arcticfox:sequenced_assembly/mending_book_nuggets");
            boolean postmortal = server.getAdvancements().get(ResourceLocation.parse("minecraft:adventure/totem_of_undying")) != null;
            log("emeralds: totem recipe=%s mending book recipe=%s bell recipe=%s 'Postmortal' advancement=%s (all true)", totem, mending, has.test("arcticfox:bell"), postmortal);
            if (!totem || !mending || !has.test("arcticfox:bell") || !postmortal) ok = false;
            recipes.byKey(ResourceLocation.parse(mods.isLoaded("create_enchantment_industry") ? "arcticfox:sequenced_assembly/mending_book" : "arcticfox:sequenced_assembly/mending_book_nuggets"))
                    .ifPresent(h -> log("  mending book result: %s", h.value().getResultItem(level.registryAccess()).getComponents()));
        }
        if (mods.isLoaded("create_enchantment_industry")) {
            boolean grind = has.test("arcticfox:grinding/emerald");
            log("CEI: emerald grinds into liquid experience=%s (true)", grind);
            if (!grind) ok = false;
        }
        if (mods.isLoaded("spoiled")) {
            try {
                var recipe = recipes.byKey(ResourceLocation.parse("spoiled:spoiling/initial_to_rotten_flesh")).orElseThrow().value();
                java.lang.reflect.Field timeField = recipe.getClass().getDeclaredField("spoilTime");
                timeField.setAccessible(true);
                int spoilTime = timeField.getInt(recipe);
                Class<?> cfg = Class.forName("com.mrbysco.spoiled.config.SpoiledConfig");
                Object common = cfg.getField("COMMON").get(null);
                Object rateValue = common.getClass().getField("spoilRate").get(common);
                int rate = ((Number) rateValue.getClass().getMethod("get").invoke(rateValue)).intValue();
                double days = spoilTime * (double) rate / 1200.0;
                log("spoiled: %d updates every %d s = food rots after %.2f in-game days (expected 3)", spoilTime, rate, days);
                if (Math.abs(days - 3) > 0.01) ok = false;
            } catch (Throwable t) {
                ArcticFox.LOGGER.error(P + "spoiled check failed", t);
                ok = false;
            }
        }
        if (mods.isLoaded("alexsmobs")) {
            var biomes = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
            java.util.Set<String> all = new java.util.TreeSet<>();
            for (String b : List.of("arcticfox:frozen_wastes", "minecraft:snowy_plains", "minecraft:snowy_taiga", "minecraft:grove",
                    "minecraft:snowy_slopes", "minecraft:ice_spikes", "minecraft:snowy_beach", "minecraft:frozen_ocean", "minecraft:frozen_river")) {
                var holder = biomes.getHolder(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, ResourceLocation.parse(b)));
                if (holder.isEmpty()) continue;
                List<String> found = new java.util.ArrayList<>();
                for (var category : net.minecraft.world.entity.MobCategory.values()) {
                    for (var data : holder.get().value().getMobSettings().getMobs(category).unwrap()) {
                        var key = BuiltInRegistries.ENTITY_TYPE.getKey(data.type);
                        if (!key.getNamespace().equals("alexsmobs")) continue;
                        boolean blocked = category == net.minecraft.world.entity.MobCategory.MONSTER && !data.type.is(ModTags.SURFACE_MONSTERS);
                        found.add(key.getPath() + "(" + category.getName() + (blocked ? ", BLOCKED on surface" : "") + ")");
                        all.add(key.getPath());
                    }
                }
                log("alex's mobs in %s: %s", b, found);
            }
            log("alex's mobs that can spawn in the frozen world: %d species: %s", all.size(), all);
            if (all.isEmpty()) ok = false;
            for (String bad : List.of("fly", "platypus", "raccoon", "cockroach", "devils_hole_pupfish", "sunbird")) {
                if (all.contains(bad)) { log("FAIL: %s still spawns", bad); ok = false; }
            }
        }
        if (mods.isLoaded("create_new_age") && com.ppcharcoal.arcticfox.compat.ColdSweatCompat.loaded()) {
            BlockPos at = new BlockPos(2900, 150, 2600), control = at.offset(0, 0, 20);
            level.getChunk(at.getX() >> 4, at.getZ() >> 4);
            level.setBlock(at.below(), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(control.below(), Blocks.STONE.defaultBlockState(), 3);
            var corium = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create_new_age:corium"));
            level.setBlock(at, corium.defaultBlockState(), 3);
            double hot = com.ppcharcoal.arcticfox.compat.ColdSweatCompat.temperatureCelsius(level, at.offset(2, 0, 0), true);
            double cold = com.ppcharcoal.arcticfox.compat.ColdSweatCompat.temperatureCelsius(level, control.offset(2, 0, 0), true);
            log("create new age: corium %.1f C two blocks away vs %.1f C without it (+%.1f, expected > 3)", hot, cold, hot - cold);
            if (hot - cold <= 3) ok = false;
        }
        if (mods.isLoaded("railways")) {
            var conductor = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse("railways:conductor"));
            boolean banned = conductor.map(c -> c.is(ModTags.BANISHED)).orElse(false);
            log("steam 'n' rails: conductor mob banished=%s (true)", banned);
            if (!banned) ok = false;
        }
        List<String> sophisticated = mods.getMods().stream().map(m -> m.getModId()).filter(id -> id.startsWith("sophisticated")).sorted().toList();
        if (!sophisticated.isEmpty()) log("sophisticated mods loaded alongside: %s", sophisticated);
        return ok;
    }

    private static boolean checkCreateHeat(ServerLevel level) {
        boolean ok = true;
        BlockPos base = new BlockPos(2600, 150, 2600);
        level.getChunk(base.getX() >> 4, base.getZ() >> 4);
        java.util.function.Function<BlockPos, Double> temp = p -> com.ppcharcoal.arcticfox.compat.ColdSweatCompat.temperatureCelsius(level, p, true);
        String[][] cases = {{"blaze_burner", "seething"}, {"basin", "lava"}, {"fluid_tank", "lava"}};
        // Cold Sweat caches the blocks it scanned for the current tick, so place everything first and
        // compare each heat source against an identical control spot 20 blocks away.
        BlockPos[] at = new BlockPos[cases.length];
        for (int i = 0; i < cases.length; i++) {
            at[i] = base.offset(i * 40, 0, 0);
            level.setBlock(at[i].below(), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(at[i].offset(0, -1, 20), Blocks.STONE.defaultBlockState(), 3);
            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", cases[i][0]));
            BlockState state = block.defaultBlockState();
            if (cases[i][1].equals("seething")) {
                for (var prop : state.getProperties()) if (prop.getName().equals("blaze")) state = cycleTo(state, prop, "seething");
            }
            level.setBlock(at[i], state, 3);
            if (cases[i][1].equals("lava")) {
                var handler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, at[i], null);
                int filled = handler == null ? 0 : handler.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA, 4000),
                        net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                log("filled %s with %d mB lava", cases[i][0], filled);
            }
        }
        for (int i = 0; i < cases.length; i++) {
            double heated = temp.apply(at[i].offset(2, 0, 0));
            double control = temp.apply(at[i].offset(2, 0, 20));
            log("create heat: %s with %s: %.1f C two blocks away vs %.1f C without it (+%.1f, expected > 3)", cases[i][0], cases[i][1], heated, control, heated - control);
            if (heated - control <= 3) ok = false;
        }
        return ok;
    }

    private static void loadArea(ServerLevel level, BlockPos center, int chunkRadius) {
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                level.getChunk((center.getX() >> 4) + dx, (center.getZ() >> 4) + dz);
            }
        }
    }

    /** Heat sources melt the snow piled around them and stop new snow from settling there. */
    private static boolean checkSnowMelt(ServerLevel level) {
        boolean ok = true;
        java.util.List<Object[]> cases = new java.util.ArrayList<>();
        cases.add(new Object[]{"cast-iron stove", com.ppcharcoal.arcticfox.registry.ModBlocks.CABIN_STOVE.get().defaultBlockState()
                .setValue(com.ppcharcoal.arcticfox.block.CabinStoveBlock.LIT, true), 4});
        var burner = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("create", "blaze_burner"));
        if (burner.isPresent()) {
            BlockState st = burner.get().defaultBlockState();
            for (var prop : st.getProperties()) if (prop.getName().equals("blaze")) st = cycleTo(st, prop, "kindled");
            cases.add(new Object[]{"kindled blaze burner", st, 4});
        }
        int i = 0;
        for (Object[] c : cases) {
            BlockPos base = new BlockPos(-3000 - 40 * i++, 240, -3000);
            loadArea(level, base, 1);
            for (BlockPos p : BlockPos.betweenClosed(base.offset(-8, 0, -8), base.offset(8, 0, 8))) level.setBlock(p, Blocks.STONE.defaultBlockState(), 2);
            for (BlockPos p : BlockPos.betweenClosed(base.offset(-8, 1, -8), base.offset(8, 6, 8))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            BlockPos heat = base.above();
            level.setBlock(heat, (BlockState) c[1], 3);
            BlockPos near = base.offset(2, 1, 0), far = base.offset(7, 1, 0);
            level.setBlock(near, Blocks.SNOW_BLOCK.defaultBlockState(), 2);
            level.setBlock(near.above(), Blocks.SNOW.defaultBlockState().setValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS, 4), 2);
            level.setBlock(base.offset(0, 1, -3), Blocks.SNOW.defaultBlockState().setValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS, 8), 2);
            level.setBlock(far, Blocks.SNOW.defaultBlockState().setValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS, 6), 2);
            java.util.Map<Long, java.util.List<long[]>> found = new java.util.HashMap<>();
            int sources = 0;
            for (int pass = 0; pass < 6; pass++) {
                found.clear();
                sources = com.ppcharcoal.arcticfox.world.SnowMelt.scanChunk(level, heat.getX() >> 4, heat.getZ() >> 4, found);
            }
            com.ppcharcoal.arcticfox.world.SnowMelt.remember(level, found);
            boolean melted = isAir(level.getBlockState(near)) && isAir(level.getBlockState(near.above())) && isAir(level.getBlockState(base.offset(0, 1, -3)));
            boolean farKept = level.getBlockState(far).is(Blocks.SNOW);
            boolean blocked = !com.ppcharcoal.arcticfox.world.WinterWeather.addLayer(level, base.offset(1, 0, 1));
            boolean farSettles = com.ppcharcoal.arcticfox.world.WinterWeather.addLayer(level, base.offset(-7, 0, -7));
            log("snow melt: %s sources=%d melted nearby=%s far snow kept=%s new snow blocked nearby=%s settles far away=%s (all true)",
                    c[0], sources, melted, farKept, blocked, farSettles);
            if (sources < 1 || !melted || !farKept || !blocked || !farSettles) ok = false;
        }
        com.ppcharcoal.arcticfox.world.SnowMelt.remember(level, new java.util.HashMap<>());
        return ok;
    }

    /** The Wendigo: 100 hp, freezes while watched, flees when approached, returns from elsewhere, screams and attacks. */
    private static boolean checkWendigo(ServerLevel level) {
        boolean ok = true;
        long keepTime = level.getDayTime();
        level.setDayTime(keepTime - keepTime % 24000 + 18000);
        try {
            BlockPos base = new BlockPos(4000, 240, 4000);
            loadArea(level, base, 4);
            for (BlockPos p : BlockPos.betweenClosed(base.offset(-10, 0, -6), base.offset(44, 0, 6))) level.setBlock(p, Blocks.STONE.defaultBlockState(), 2);
            for (BlockPos p : BlockPos.betweenClosed(base.offset(-10, 1, -6), base.offset(44, 5, 6))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            var fake = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
                    new com.mojang.authlib.GameProfile(java.util.UUID.fromString("7d1c9a3e-1f62-4b5e-9a10-5e2f0c0ffee1"), "[ArcticFoxWendigoTest]"));
            fake.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            fake.moveTo(base.getX() + 0.5, base.getY() + 1, base.getZ() + 0.5, -90, 0); // facing +X
            fake.setYHeadRot(-90);
            var w = com.ppcharcoal.arcticfox.registry.ModEntities.WENDIGO.get().create(level);
            w.moveTo(base.getX() + 20.5, base.getY() + 1, base.getZ() + 0.5, 90, 0);
            level.addFreshEntity(w);
            w.setPrey(fake);
            log("wendigo: max health=%.0f (100 = 5x zombie) attack=%.0f", w.getMaxHealth(), w.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
            if (w.getMaxHealth() != 100) ok = false;

            // watched: holds still
            double x0 = w.getX();
            for (int i = 0; i < 60; i++) w.tick();
            log("wendigo watched: moved %.2f blocks, state=%d (expected < 0.3, %d)", Math.abs(w.getX() - x0), w.getState(), com.ppcharcoal.arcticfox.entity.Wendigo.WATCHED);
            if (Math.abs(w.getX() - x0) > 0.3 || w.getState() != com.ppcharcoal.arcticfox.entity.Wendigo.WATCHED) ok = false;

            // looking away: it creeps closer
            fake.setYRot(90); fake.setYHeadRot(90);
            double d0 = w.distanceTo(fake);
            for (int i = 0; i < 60; i++) w.tick();
            double d1 = w.distanceTo(fake);
            log("wendigo unwatched: distance %.1f -> %.1f, state=%d (expected closer, %d)", d0, d1, w.getState(), com.ppcharcoal.arcticfox.entity.Wendigo.STALK);
            if (d1 > d0 - 2 || w.getState() == com.ppcharcoal.arcticfox.entity.Wendigo.HUNT) ok = false;

            // walking towards it while looking: it bolts and vanishes
            if (w.distanceTo(fake) < 8) w.moveTo(base.getX() + 16.5, base.getY() + 1, base.getZ() + 0.5);
            fake.setYRot(-90); fake.setYHeadRot(-90);
            for (int i = 0; i < 5; i++) w.tick();
            fake.moveTo(fake.getX() + 4, fake.getY(), fake.getZ(), -90, 0);
            for (int i = 0; i < 3; i++) w.tick();
            int fled = w.getState();
            for (int i = 0; i < 40; i++) w.tick();
            log("wendigo approached: state=%d then hidden=%s invisible=%s (expected %d, true, true)", fled, w.isHidden(), w.isInvisible(), com.ppcharcoal.arcticfox.entity.Wendigo.FLEE);
            if (fled != com.ppcharcoal.arcticfox.entity.Wendigo.FLEE || !w.isHidden() || !w.isInvisible()) ok = false;

            // 30 seconds later it returns from another direction
            double before = w.lastBearing();
            w.endHideNow();
            double after = Math.atan2(w.getZ() - fake.getZ(), w.getX() - fake.getX());
            double diff = Math.abs(Mth.wrapDegrees((float) Math.toDegrees(after - before)));
            log("wendigo returns: hidden=%s %.0f blocks away horizontally, %.0f degrees from where it vanished (expected false, 26-35, >= 100)",
                    w.isHidden(), Math.hypot(w.getX() - fake.getX(), w.getZ() - fake.getZ()), diff);
            if (w.isHidden() || w.isInvisible() || diff < 100) ok = false;

            // a second player watching freezes it too, even while its prey looks away
            var friend = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
                    new com.mojang.authlib.GameProfile(java.util.UUID.fromString("7d1c9a3e-1f62-4b5e-9a10-5e2f0c0ffee2"), "[ArcticFoxWendigoFriend]"));
            friend.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            com.ppcharcoal.arcticfox.entity.WendigoRules.EXTRA_PLAYERS.clear();
            com.ppcharcoal.arcticfox.entity.WendigoRules.EXTRA_PLAYERS.add(fake);
            com.ppcharcoal.arcticfox.entity.WendigoRules.EXTRA_PLAYERS.add(friend);
            com.ppcharcoal.arcticfox.entity.WendigoRules.clearCache();
            fake.moveTo(base.getX() + 0.5, base.getY() + 1, base.getZ() + 0.5, 90, 0); fake.setYHeadRot(90); // looking away
            friend.moveTo(base.getX() + 0.5, base.getY() + 1, base.getZ() + 3.5, -90, 0); friend.setYHeadRot(-90);
            w.moveTo(base.getX() + 18.5, base.getY() + 1, base.getZ() + 1.5);
            w.setPrey(fake);
            double xw = w.getX();
            for (int i = 0; i < 40; i++) w.tick();
            log("wendigo with 2 players, prey looking away, friend watching: moved %.2f, state=%d (expected < 0.3, %d)",
                    Math.abs(w.getX() - xw), w.getState(), com.ppcharcoal.arcticfox.entity.Wendigo.WATCHED);
            if (Math.abs(w.getX() - xw) > 0.3 || w.getState() != com.ppcharcoal.arcticfox.entity.Wendigo.WATCHED) ok = false;
            friend.moveTo(friend.getX(), friend.getY(), friend.getZ(), 90, 0); friend.setYHeadRot(90);

            // light: its prey steps next to a lantern -> it backs off, switches to the player in the dark
            BlockPos lamp = fake.blockPosition().offset(1, 0, -1);
            level.setBlock(lamp, Blocks.LANTERN.defaultBlockState(), 3);
            com.ppcharcoal.arcticfox.entity.WendigoRules.clearCache();
            boolean lit = com.ppcharcoal.arcticfox.entity.WendigoRules.isInLight(fake);
            boolean friendLit = com.ppcharcoal.arcticfox.entity.WendigoRules.isInLight(friend);
            friend.moveTo(base.getX() + 0.5, base.getY() + 1, base.getZ() - 5.5, 90, 0); friend.setYHeadRot(90);
            com.ppcharcoal.arcticfox.entity.WendigoRules.clearCache();
            for (int i = 0; i < 3; i++) w.tick();
            var newPrey = w.getPrey();
            log("wendigo light: prey lit=%s, other player lit=%s, switched to the player in the dark=%s (expected true, false, true)",
                    lit, com.ppcharcoal.arcticfox.entity.WendigoRules.isInLight(friend), newPrey == friend);
            if (!lit || newPrey != friend) ok = false;
            // now both in the light -> it slinks off and hides, and waits while they stay lit
            BlockPos lamp2 = friend.blockPosition().offset(1, 0, 1);
            level.setBlock(lamp2, Blocks.LANTERN.defaultBlockState(), 3);
            com.ppcharcoal.arcticfox.entity.WendigoRules.clearCache();
            for (int i = 0; i < 40; i++) w.tick();
            boolean hidAway = w.isHidden();
            w.endHideNow();
            for (int i = 0; i < 2; i++) w.tick();
            log("wendigo light: everyone lit -> hidden=%s, a minute later still not stalking anyone lit (state=%d, expected hidden or fleeing)",
                    hidAway, w.getState());
            if (!hidAway) ok = false;
            level.setBlock(lamp, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(lamp2, Blocks.AIR.defaultBlockState(), 3);
            com.ppcharcoal.arcticfox.entity.WendigoRules.clearCache();
            w.endHideNow();
            com.ppcharcoal.arcticfox.entity.WendigoRules.EXTRA_PLAYERS.remove(friend);

            // careless: it gets within reach unseen -> scream, then attack
            w.calm();
            w.setPrey(fake);
            w.moveTo(fake.getX() + 2.5, fake.getY(), fake.getZ());
            fake.setYRot(90); fake.setYHeadRot(90);
            w.tick();
            int s1 = w.getState();
            for (int i = 0; i < 45; i++) w.tick();
            int s2 = w.getState();
            for (int i = 0; i < 40; i++) w.tick();
            log("wendigo strikes: state %d -> %d, claw attacks=%d (expected %d -> %d, > 0)", s1, s2, w.strikes,
                    com.ppcharcoal.arcticfox.entity.Wendigo.SCREAM, com.ppcharcoal.arcticfox.entity.Wendigo.HUNT);
            if (s1 != com.ppcharcoal.arcticfox.entity.Wendigo.SCREAM || s2 != com.ppcharcoal.arcticfox.entity.Wendigo.HUNT || w.strikes == 0) ok = false;
            // its prey dies -> it leaves for the night
            float keepHealth = fake.getHealth();
            fake.setHealth(0);
            for (int i = 0; i < 3; i++) w.tick();
            log("wendigo after killing its prey: removed=%s (expected true)", w.isRemoved());
            if (!w.isRemoved()) ok = false;
            fake.setHealth(keepHealth > 0 ? keepHealth : 20);
            w.discard();

            // surface only: once its prey goes underground it stops the attack and backs off
            var w2 = com.ppcharcoal.arcticfox.registry.ModEntities.WENDIGO.get().create(level);
            w2.moveTo(fake.getX() + 2.5, fake.getY(), fake.getZ(), 90, 0);
            level.addFreshEntity(w2);
            w2.setPrey(fake);
            fake.setYRot(90); fake.setYHeadRot(90);
            for (int i = 0; i < 45; i++) w2.tick();
            int huntState = w2.getState();
            BlockPos roof = fake.blockPosition().above(9);
            for (BlockPos p : BlockPos.betweenClosed(roof.offset(-1, 0, -1), roof.offset(1, 0, 1))) level.setBlock(p, Blocks.STONE.defaultBlockState(), 2);
            boolean under = com.ppcharcoal.arcticfox.entity.WendigoRules.isUnderground(fake);
            boolean candidate = com.ppcharcoal.arcticfox.world.WendigoSpawner.candidates(level).contains(fake);
            int strikesBefore = w2.strikes;
            for (int i = 0; i < 2; i++) w2.tick();
            int afterState = w2.getState();
            for (int i = 0; i < 40; i++) w2.tick();
            log("wendigo underground: hunting=%s, prey underground=%s, prey still a spawn candidate=%s, -> state=%d, attacks after=%d, hidden=%s (expected true, true, false, %d, 0, true)",
                    huntState == com.ppcharcoal.arcticfox.entity.Wendigo.HUNT, under, candidate, afterState, w2.strikes - strikesBefore, w2.isHidden(),
                    com.ppcharcoal.arcticfox.entity.Wendigo.FLEE);
            if (huntState != com.ppcharcoal.arcticfox.entity.Wendigo.HUNT || !under || candidate
                    || afterState != com.ppcharcoal.arcticfox.entity.Wendigo.FLEE || w2.strikes - strikesBefore > 1 || !w2.isHidden()) ok = false;
            for (BlockPos p : BlockPos.betweenClosed(roof.offset(-1, 0, -1), roof.offset(1, 0, 1))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            w2.discard();

            // one a night: tonight's Wendigo comes once, whatever the number of players
            level.setDayTime(5 * 24000L + 18000);
            var nights = com.ppcharcoal.arcticfox.world.WendigoSpawner.Nights.get(level);
            long keepNight = nights.lastNight;
            nights.lastNight = -1;
            com.ppcharcoal.arcticfox.entity.Wendigo first = null, second = null;
            for (int attempt = 0; attempt < 8 && first == null; attempt++) {
                BlockPos g = new BlockPos(1200 + attempt * 300, 0, 900);
                loadArea(level, g, 4);
                BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, g);
                if (!level.getFluidState(top.below()).isEmpty()) continue;
                fake.moveTo(top.getX() + 0.5, top.getY(), top.getZ() + 0.5, 0, 0);
                com.ppcharcoal.arcticfox.entity.WendigoRules.clearCache();
                first = com.ppcharcoal.arcticfox.world.WendigoSpawner.trySpawnTonight(level);
            }
            if (first != null) {
                second = com.ppcharcoal.arcticfox.world.WendigoSpawner.trySpawnTonight(level);
                log("wendigo spawner: first tonight=%s at %.0f blocks, second tonight=%s (expected true, 34-42, false)",
                        true, first.distanceTo(fake), second != null);
                first.discard();
            } else {
                log("wendigo spawner: no Wendigo could spawn (expected one)");
            }
            if (first == null || second != null) ok = false;
            if (second != null) second.discard();
            nights.lastNight = keepNight;
            com.ppcharcoal.arcticfox.entity.WendigoRules.EXTRA_PLAYERS.clear();
        } catch (Throwable t) {
            ArcticFox.LOGGER.error(P + "wendigo check failed", t);
            ok = false;
        } finally {
            level.setDayTime(keepTime);
        }
        return ok;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState cycleTo(BlockState state, net.minecraft.world.level.block.state.properties.Property prop, String value) {
        java.util.Optional parsed = prop.getValue(value);
        return parsed.isPresent() ? state.setValue(prop, (Comparable) parsed.get()) : state;
    }

    private static boolean checkCompat(MinecraftServer server, ServerLevel level) {
        boolean ok = true;
        net.neoforged.fml.ModList mods = net.neoforged.fml.ModList.get();
        if (mods.isLoaded("sereneseasons")) {
            try {
                Object state = Class.forName("sereneseasons.api.season.SeasonHelper")
                        .getMethod("getSeasonState", net.minecraft.world.level.Level.class).invoke(null, level);
                Object sub = state.getClass().getMethod("getSubSeason").invoke(state);
                var key = new java.util.concurrent.atomic.AtomicReference<GameRules.Key<GameRules.BooleanValue>>();
                GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> k, GameRules.Type<T> type) {
                        if (k.getId().equals("doSeasonCycle")) key.set((GameRules.Key<GameRules.BooleanValue>) (Object) k);
                    }
                });
                Boolean cycle = key.get() == null ? null : server.getGameRules().getBoolean(key.get());
                log("serene seasons: subSeason=%s doSeasonCycle=%s (expected MID_WINTER / false)", sub, cycle);
                if (!"MID_WINTER".equals(String.valueOf(sub)) || !Boolean.FALSE.equals(cycle)) ok = false;
            } catch (Throwable t) {
                ArcticFox.LOGGER.error(P + "serene seasons check failed", t);
                ok = false;
            }
        }
        if (mods.isLoaded("create")) {
            try {
                var bottomless = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.FLUID,
                        ResourceLocation.fromNamespaceAndPath("create", "bottomless/allow"));
                boolean waterBottomless = net.minecraft.world.level.material.Fluids.WATER.defaultFluidState().is(bottomless);
                log("create: water is bottomless=%s (expected false)", waterBottomless);
                if (waterBottomless) ok = false;

                Class<?> spill = Class.forName("com.simibubi.create.api.event.PipeCollisionEvent$Spill");
                Object event = spill.getConstructor(net.minecraft.world.level.Level.class, BlockPos.class,
                                net.minecraft.world.level.material.Fluid.class, net.minecraft.world.level.material.Fluid.class, BlockState.class)
                        .newInstance(level, BlockPos.ZERO, net.minecraft.world.level.material.Fluids.WATER, net.minecraft.world.level.material.Fluids.LAVA, null);
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post((net.neoforged.bus.api.Event) event);
                BlockState pipeResult = (BlockState) spill.getMethod("getState").invoke(event);
                log("create: lava pipe into water produces %s (expected arcticfox:scoria)",
                        pipeResult == null ? "null" : BuiltInRegistries.BLOCK.getKey(pipeResult.getBlock()));
                if (pipeResult == null || !pipeResult.is(com.ppcharcoal.arcticfox.registry.ModBlocks.SCORIA.get())) ok = false;

                for (String r : List.of("mechanical_crafting/elytra", "mechanical_crafting/shulker_shell")) {
                    var recipe = server.getRecipeManager().byKey(ArcticFox.id(r));
                    String type = recipe.map(h -> BuiltInRegistries.RECIPE_TYPE.getKey(h.value().getType()).toString()).orElse("missing");
                    String result = recipe.map(h -> BuiltInRegistries.ITEM.getKey(h.value().getResultItem(level.registryAccess()).getItem()).toString()).orElse("-");
                    log("create recipe %s: type=%s result=%s", r, type, result);
                    if (!type.equals("create:mechanical_crafting")) ok = false;
                }
                boolean sky = server.getAdvancements().get(ResourceLocation.parse("minecraft:end/elytra")) != null;
                log("advancement 'Sky's the Limit' present with Create=%s (true)", sky);
                if (!sky) ok = false;
            } catch (Throwable t) {
                ArcticFox.LOGGER.error(P + "create check failed", t);
                ok = false;
            }
        }
        return ok;
    }

    private static boolean isAir(BlockState state) {
        return state.isAir() || !state.getFluidState().isEmpty();
    }

    private static void log(String fmt, Object... args) {
        ArcticFox.LOGGER.info(P + String.format(java.util.Locale.ROOT, fmt, args));
    }
}
