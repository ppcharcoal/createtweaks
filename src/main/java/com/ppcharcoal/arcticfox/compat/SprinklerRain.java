package com.ppcharcoal.arcticfox.compat;

import com.ppcharcoal.arcticfox.ArcticFox;
import com.ppcharcoal.arcticfox.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Asks Create Slice & Dice whether a position is inside sprinkler "rain", without a compile dependency.
 * Supports both implementations:
 * <ul>
 *     <li>Slice & Dice 4.3+ : the Atmosphere weather API (local weather areas)</li>
 *     <li>Slice & Dice up to 4.2.x : the WetAir helper</li>
 * </ul>
 */
public final class SprinklerRain {
    private static boolean initialized;
    private static Object atmosphereApi;
    private static Method getWeather;
    private static Method atPositionOrDefault;
    private static Method precipitation;
    private static Object wetAirTarget;
    private static Method wetAirCheck;
    private static boolean failed;

    private SprinklerRain() {
    }

    public static boolean isSprinklerRain(Level level, BlockPos pos) {
        if (failed || !Config.SPEC.isLoaded() || !Config.SPRINKLER_RAIN_FIX.get()) return false;
        try {
            init();
            if (getWeather != null) {
                Object weather = getWeather.invoke(atmosphereApi, level);
                Object holder = atPositionOrDefault.invoke(weather, level, Vec3.atCenterOf(pos));
                Object condition = holder instanceof Holder<?> h ? h.value() : holder;
                if (condition != null && precipitation.invoke(condition) == Biome.Precipitation.RAIN) {
                    return true;
                }
            }
            if (wetAirCheck != null) {
                return (Boolean) wetAirCheck.invoke(wetAirTarget, level, pos);
            }
        } catch (Throwable t) {
            failed = true;
            ArcticFox.LOGGER.warn("Sprinkler rain compat disabled after an error", t);
        }
        return false;
    }

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;
        try {
            Class<?> api = Class.forName("com.possible_triangle.atmosphere.api.v1.WeatherAPI");
            atmosphereApi = api.getField("INSTANCE").get(null);
            getWeather = findMethod(atmosphereApi.getClass(), "getWeather", 1);
            atPositionOrDefault = findMethod(getWeather.getReturnType(), "atPositionOrDefault", 2);
            precipitation = Class.forName("com.possible_triangle.atmosphere.api.v1.WeatherCondition").getMethod("precipitation");
            ArcticFox.LOGGER.info("Sprinkler rain compat: using Atmosphere weather API");
        } catch (Throwable ignored) {
            getWeather = null;
        }
        try {
            Class<?> wetAir = Class.forName("com.possible_triangle.sliceanddice.block.sprinkler.WetAir");
            wetAirCheck = wetAir.getMethod("check", Level.class, BlockPos.class);
            if (!Modifier.isStatic(wetAirCheck.getModifiers())) {
                Field instance = wetAir.getField("INSTANCE");
                wetAirTarget = instance.get(null);
            }
            ArcticFox.LOGGER.info("Sprinkler rain compat: using Slice & Dice WetAir");
        } catch (Throwable ignored) {
            wetAirCheck = null;
        }
    }

    private static Method findMethod(Class<?> type, String name, int params) throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == params) {
                return method;
            }
        }
        throw new NoSuchMethodException(type.getName() + "#" + name);
    }

    /** For the CI smoke test. */
    public static String describe() {
        init();
        return "atmosphere=" + (getWeather != null) + " wetAir=" + (wetAirCheck != null);
    }
}
