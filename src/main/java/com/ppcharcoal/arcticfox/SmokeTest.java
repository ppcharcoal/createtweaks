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
            ok &= checkCompat(server, level);
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
        log("snow stacking: layers added=%d (expected 24) snow blocks=%d (2) top=%s", added, snowBlocks, topLayer);
        if (added != 24 || snowBlocks != 2 || !topLayer.is(Blocks.SNOW)) ok = false;
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
