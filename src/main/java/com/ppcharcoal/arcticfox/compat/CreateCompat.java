package com.ppcharcoal.arcticfox.compat;

import com.ppcharcoal.arcticfox.ArcticFox;
import com.ppcharcoal.arcticfox.event.GameplayEvents;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;

import java.lang.reflect.Method;

/**
 * Create resolves lava/water (and honey/chocolate) collisions inside pipes through its own
 * PipeCollisionEvent instead of vanilla's fluid interactions. We hook it reflectively (no compile
 * dependency on Create) at the lowest priority and turn the result into scoria as well.
 */
public final class CreateCompat {
    private static final String EVENT = "com.simibubi.create.api.event.PipeCollisionEvent";

    private CreateCompat() {
    }

    public static void init() {
        try {
            Class<?> base = Class.forName(EVENT);
            Method getState = base.getMethod("getState");
            Method setState = base.getMethod("setState", BlockState.class);
            for (String sub : new String[]{"$Flow", "$Spill"}) {
                register(Class.forName(EVENT + sub), getState, setState);
            }
            ArcticFox.LOGGER.info("Hooked Create pipe fluid collisions");
        } catch (ReflectiveOperationException | RuntimeException e) {
            ArcticFox.LOGGER.warn("Could not hook Create pipe fluid collisions; pipe cobblestone generators may still work", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Event> void register(Class<?> eventClass, Method getState, Method setState) {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, (Class<T>) eventClass, event -> {
            try {
                BlockState state = (BlockState) getState.invoke(event);
                if (state != null) {
                    BlockState replaced = GameplayEvents.scoriafy(state);
                    if (replaced != state) {
                        setState.invoke(event, replaced);
                    }
                }
            } catch (ReflectiveOperationException e) {
                ArcticFox.LOGGER.error("Failed to adjust Create pipe collision", e);
            }
        });
    }
}
