package com.ppcharcoal.arcticfox;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue BLOCK_NETHER = BUILDER
            .comment("Prevent nether portals from lighting and any entity from travelling to the Nether.")
            .define("dimensions.blockNether", true);

    public static final ModConfigSpec.BooleanValue BLOCK_END = BUILDER
            .comment("Prevent any entity from travelling to the End.")
            .define("dimensions.blockEnd", true);

    public static final ModConfigSpec.BooleanValue DISABLE_WATER_SOURCES = BUILDER
            .comment("Force the waterSourceConversion gamerule off on every server start (no infinite water).")
            .define("fluids.disableInfiniteWater", true);

    public static final ModConfigSpec.BooleanValue EVAPORATE_PLACED_WATER = BUILDER
            .comment("Water source blocks placed after world generation (buckets, dispensers, Create pipes) evaporate. Waterlogged blocks are never affected.")
            .define("fluids.evaporatePlacedWater", true);

    public static final ModConfigSpec.IntValue EVAPORATION_SECONDS = BUILDER
            .comment("Seconds before a placed water source evaporates.")
            .defineInRange("fluids.evaporationSeconds", 15, 1, 3600);

    public static final ModConfigSpec.BooleanValue SCORIA_GENERATORS = BUILDER
            .comment("Fluid interactions that would create blocks in #arcticfox:fluid_generator_outputs (cobblestone, stone, Create limestone/scoria) create useless Scoria instead.")
            .define("fluids.scoriaGenerators", true);

    public static final ModConfigSpec.BooleanValue RESTRICT_SURFACE_MONSTERS = BUILDER
            .comment("Only monsters in #arcticfox:surface_monsters may naturally spawn where they can see the sky.")
            .define("mobs.restrictSurfaceMonsters", true);

    public static final ModConfigSpec.BooleanValue BANISH_ILLAGERS_AND_VILLAGERS = BUILDER
            .comment("Entities in #arcticfox:banished (villagers, traders, illagers, witches...) are never allowed to exist.")
            .define("mobs.banishVillagersAndIllagers", true);

    public static final ModConfigSpec.BooleanValue LOCK_WINTER = BUILDER
            .comment("If Serene Seasons is installed, lock every world into mid winter (doSeasonCycle=false).")
            .define("compat.lockWinter", true);

    public static final ModConfigSpec.ConfigValue<String> WINTER_SUB_SEASON = BUILDER
            .comment("Serene Seasons sub-season to lock to: early_winter, mid_winter or late_winter.")
            .define("compat.winterSubSeason", "mid_winter");

    public static final ModConfigSpec.BooleanValue SPRINKLER_RAIN_FIX = BUILDER
            .comment("Let Create Slice & Dice sprinkler 'rain' count as rain while Serene Seasons is installed (replaces Serene & Slice Compat).")
            .define("compat.sprinklerRainFix", true);

    public static final ModConfigSpec.BooleanValue NIGHT_SNOWSTORMS = BUILDER
            .comment("Arctic Fox controls the weather: sunny or snowy days, a blizzard every night.")
            .define("weather.nightSnowstorms", true);

    public static final ModConfigSpec.DoubleValue SNOWY_DAY_CHANCE = BUILDER
            .comment("Chance that a day is snowy instead of sunny (nights are always blizzards).")
            .defineInRange("weather.snowyDayChance", 0.5, 0.0, 1.0);

    public static final ModConfigSpec.BooleanValue BLIZZARD_FOG = BUILDER
            .comment("Thick white fog outdoors during blizzards (client side).")
            .define("weather.blizzardFog", true);

    public static final ModConfigSpec.BooleanValue NO_NATURAL_LIGHTNING = BUILDER
            .comment("Suppress natural lightning so storms are blizzards (trident Channeling still works).")
            .define("weather.noNaturalLightning", true);

    public static final ModConfigSpec.BooleanValue SNOW_STACKING = BUILDER
            .comment("Snow piles up while it snows, beyond one block, up to snow.maxDepthBlocks.")
            .define("snow.stacking", true);

    public static final ModConfigSpec.IntValue SNOW_MAX_DEPTH = BUILDER
            .comment("Maximum snow depth in blocks.")
            .defineInRange("snow.maxDepthBlocks", 2, 1, 8);

    public static final ModConfigSpec.IntValue SNOW_ATTEMPTS_PER_TICK = BUILDER
            .comment("Columns per player per tick that receive snow while it snows (doubled during snowstorms). ~6 gives roughly 1 block per snowstorm night.")
            .defineInRange("snow.attemptsPerTick", 6, 0, 256);

    public static final ModConfigSpec.BooleanValue CROP_RULES = BUILDER
            .comment("Crops only grow when sheltered (roof above), warm (above crops.minTemperature) and watered (moist farmland).")
            .define("crops.enabled", true);

    public static final ModConfigSpec.DoubleValue CROP_MIN_TEMPERATURE = BUILDER
            .comment("Minimum temperature in Celsius for crops to grow (needs Cold Sweat; otherwise heat sources / underground are used).")
            .defineInRange("crops.minTemperature", 0.0, -100.0, 100.0);

    public static final ModConfigSpec.BooleanValue NO_GRACE_ON_RESPAWN = BUILDER
            .comment("Cold Sweat: players who respawn at their own spawn point (bed / respawn anchor) don't get the grace period again.")
            .define("compat.noGraceOnRespawn", true);

    public static final ModConfigSpec.BooleanValue RAW_FOOD_SICKNESS = BUILDER
            .comment("Eating raw meat or fish has an 80% chance to give Hunger for 30 seconds, like rotten flesh.")
            .define("survival.rawFoodSickness", true);

    public static final ModConfigSpec.BooleanValue NATURAL_FISHING_ONLY = BUILDER
            .comment("Fishing only catches anything in water connected to naturally generated water.")
            .define("survival.naturalFishingOnly", true);

    public static final ModConfigSpec.BooleanValue SPAWN_NEAR_TREES = BUILDER
            .comment("New worlds put the spawn point in a biome with scattered trees (#arcticfox:spawn_biomes).")
            .define("world.spawnNearTrees", true);

    public static final ModConfigSpec.BooleanValue CREATE_HEAT = BUILDER
            .comment("Cold Sweat + Create: blaze burners, steam engines and hot fluids in pipes, pumps, basins etc. warm you up.")
            .define("compat.createHeat", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
