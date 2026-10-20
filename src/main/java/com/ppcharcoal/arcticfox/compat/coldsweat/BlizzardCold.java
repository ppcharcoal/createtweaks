package com.ppcharcoal.arcticfox.compat.coldsweat;

import com.momosoftworks.coldsweat.api.event.core.registry.TempModifierRegisterEvent;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.api.util.placement.Placement;
import com.ppcharcoal.arcticfox.ArcticFox;
import com.ppcharcoal.arcticfox.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.function.Function;

/**
 * Cold Sweat: a blizzard bites. Anyone out in the open during a snowstorm feels {@code weather.blizzardChill} degrees
 * colder (default 10 C, on top of the -15 C of the wastes), scaled by how strong the storm is. Shelter (anything
 * overhead) or being underground removes it.
 */
public final class BlizzardCold {
    private static final double C = 1 / 25.0;

    private BlizzardCold() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(BlizzardCold::onRegister);
        NeoForge.EVENT_BUS.addListener(BlizzardCold::onPlayerTick);
    }

    private static void onRegister(TempModifierRegisterEvent event) {
        event.register(ArcticFox.id("blizzard"), Modifier::new);
    }

    /** Makes sure every player carries the modifier (also players from before this version). */
    private static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % 100 != 7) return;
        if (!Temperature.hasModifier(player, Temperature.Trait.WORLD, Modifier.class)) {
            Temperature.addModifier(player, new Modifier().tickRate(20), Temperature.Trait.WORLD, Placement.LAST);
        }
    }

    /** Degrees Celsius of extra cold at this spot right now (0 when sheltered or calm). */
    public static double chillAt(Level level, BlockPos pos) {
        if (!Config.NIGHT_SNOWSTORMS.get()) return 0;
        float storm = level.getThunderLevel(1.0F) * level.getRainLevel(1.0F);
        if (storm <= 0) return 0;
        if (level.getBiome(pos).value().getPrecipitationAt(pos) != Biome.Precipitation.SNOW) return 0;
        if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) > pos.getY() + 1) return 0; // sheltered
        return Config.BLIZZARD_CHILL.get() * storm;
    }

    /** Smoke test: chill outdoors vs under a roof, and the modifier applied to a world temperature. */
    public static String selfTest(Level level, Player player, BlockPos open) {
        float rain = level.getRainLevel(1.0F), thunder = level.getThunderLevel(1.0F);
        level.setRainLevel(1.0F);
        level.setThunderLevel(1.0F);
        try {
            player.moveTo(open.getX() + 0.5, open.getY(), open.getZ() + 0.5);
            double outdoors = chillAt(level, BlockPos.containing(player.getX(), player.getEyeY(), player.getZ()));
            double applied = new Modifier().update(0.0, player, Temperature.Trait.WORLD) * 25.0;
            BlockPos roof = open.above(4);
            level.setBlock(roof, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
            double sheltered = chillAt(level, BlockPos.containing(player.getX(), player.getEyeY(), player.getZ()));
            level.setBlock(roof, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            level.setRainLevel(0.0F);
            level.setThunderLevel(0.0F);
            double calm = chillAt(level, BlockPos.containing(player.getX(), player.getEyeY(), player.getZ()));
            boolean ok = outdoors >= 9.9 && applied <= -9.9 && sheltered == 0 && calm == 0;
            return String.format(java.util.Locale.ROOT, "%s blizzard chill: outdoors -%.1f C, modifier turns 0 C into %.1f C, under a roof -%.1f C, calm night -%.1f C (expected -10, -10, 0, 0)",
                    ok ? "PASS" : "FAIL", outdoors, applied, sheltered, calm);
        } finally {
            level.setRainLevel(rain);
            level.setThunderLevel(thunder);
        }
    }

    public static final class Modifier extends TempModifier {
        public Modifier() {
        }

        @Override
        protected Function<Double, Double> calculate(LivingEntity entity, Temperature.Trait trait) {
            double chill = chillAt(entity.level(), BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ()));
            if (chill <= 0) return temp -> temp;
            double mc = chill * C;
            return temp -> temp - mc;
        }
    }
}
