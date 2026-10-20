package com.ppcharcoal.arcticfox.compat;

import com.ppcharcoal.arcticfox.ArcticFox;
import com.ppcharcoal.arcticfox.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.lang.reflect.Method;

/** Optional Cold Sweat integration, all reflective so Cold Sweat stays optional. */
@EventBusSubscriber(modid = ArcticFox.MOD_ID)
public final class ColdSweatCompat {
    /** Cold Sweat's flag (in player persistent data) that the one-time grace period was already handed out. */
    public static final String GRACE_FLAG = "GivenGracePeriod";

    private static boolean initialized;
    private static Method roughTemperature;
    private static Method convert;
    private static Object unitsMc;
    private static Object unitsC;

    private ColdSweatCompat() {
    }

    public static boolean loaded() {
        return ModList.get().isLoaded("cold_sweat");
    }

    /** World temperature at a position in Celsius, or null if Cold Sweat is not installed. */
    public static Double temperatureCelsius(Level level, BlockPos pos) {
        return temperatureCelsius(level, pos, false);
    }

    /** @param fresh bypass Cold Sweat's 10 second temperature cache */
    public static Double temperatureCelsius(Level level, BlockPos pos, boolean fresh) {
        if (!loaded()) return null;
        try {
            init();
            if (roughTemperature == null) return null;
            double mc = (double) roughTemperature.invoke(null, level, pos, fresh ? 2 : 0);
            return (double) convert.invoke(null, mc, unitsMc, unitsC, true);
        } catch (Throwable t) {
            ArcticFox.LOGGER.warn("Cold Sweat temperature lookup failed; falling back to heat-source rules", t);
            roughTemperature = null;
            return null;
        }
    }

    private static synchronized void init() throws ReflectiveOperationException {
        if (initialized) return;
        initialized = true;
        Class<?> worldHelper = Class.forName("com.momosoftworks.coldsweat.util.world.WorldHelper");
        roughTemperature = worldHelper.getMethod("getRoughTemperatureAt", Level.class, BlockPos.class, int.class);
        Class<?> temperature = Class.forName("com.momosoftworks.coldsweat.api.util.Temperature");
        Class<?> units = Class.forName("com.momosoftworks.coldsweat.api.util.Temperature$Units");
        convert = temperature.getMethod("convert", double.class, units, units, boolean.class);
        for (Object constant : units.getEnumConstants()) {
            String name = ((Enum<?>) constant).name();
            if (name.equals("MC")) unitsMc = constant;
            if (name.equals("C")) unitsC = constant;
        }
    }

    /**
     * Cold Sweat stores "grace already given" in player persistent data, which does not survive death,
     * so every respawn grants the grace period again. If the player has a spawn point (bed / respawn anchor)
     * we carry the flag over, so respawning at your base never gives grace again.
     */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath() || !loaded() || !Config.NO_GRACE_ON_RESPAWN.get()) return;
        if (event.getOriginal() instanceof ServerPlayer original && original.getRespawnPosition() != null) {
            event.getEntity().getPersistentData().putBoolean(GRACE_FLAG, true);
        }
    }
}
