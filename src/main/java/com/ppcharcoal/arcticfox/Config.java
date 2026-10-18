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

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
