package com.ppcharcoal.arcticfox.compat;

import com.ppcharcoal.arcticfox.ArcticFox;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/** Reflective access to Create internals, so we never need Create on the compile classpath. */
public final class CreateReflect {
    private CreateReflect() {
    }

    private static boolean init;
    private static Object transportType;
    private static Method getBehaviour;
    private static Field interfaces;
    private static Method providedFluid;
    private static Method getTank;
    private static Field boiler;
    private static Method theoreticalHeat;

    private static synchronized void init() {
        if (init) return;
        init = true;
        try {
            Class<?> transport = Class.forName("com.simibubi.create.content.fluids.FluidTransportBehaviour");
            transportType = transport.getField("TYPE").get(null);
            interfaces = transport.getField("interfaces");
            Class<?> smart = Class.forName("com.simibubi.create.foundation.blockEntity.SmartBlockEntity");
            Class<?> behaviourType = Class.forName("com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType");
            getBehaviour = smart.getMethod("getBehaviour", behaviourType);
            providedFluid = Class.forName("com.simibubi.create.content.fluids.PipeConnection").getMethod("getProvidedFluid");
        } catch (ReflectiveOperationException e) {
            ArcticFox.LOGGER.warn("Create pipe heat disabled", e);
        }
        try {
            getTank = Class.forName("com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity").getMethod("getTank");
            boiler = Class.forName("com.simibubi.create.content.fluids.tank.FluidTankBlockEntity").getField("boiler");
            theoreticalHeat = Class.forName("com.simibubi.create.content.fluids.tank.BoilerData").getMethod("getTheoreticalHeatLevel");
        } catch (ReflectiveOperationException e) {
            ArcticFox.LOGGER.warn("Create steam engine heat disabled", e);
        }
    }

    public static int boilerHeat(@Nullable BlockEntity engine) {
        init();
        if (engine == null || getTank == null) return 0;
        try {
            Object tank = getTank.invoke(engine);
            if (tank == null) return 0;
            Object data = boiler.get(tank);
            return data == null ? 0 : (int) theoreticalHeat.invoke(data);
        } catch (ReflectiveOperationException | ClassCastException e) {
            return 0;
        }
    }

    @Nullable
    public static FluidStack fluidIn(Level level, BlockPos pos, BlockState state) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return null;
        // basins, spouts, drains (and anything else) expose a fluid handler
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, state, be, null);
        if (handler != null) {
            for (int i = 0; i < handler.getTanks(); i++) {
                FluidStack stack = handler.getFluidInTank(i);
                if (!stack.isEmpty()) return stack;
            }
        }
        // pipes and pumps: look at what flows through them
        init();
        if (getBehaviour == null) return null;
        try {
            Object behaviour = getBehaviour.invoke(be, transportType);
            if (behaviour == null) return null;
            Map<?, ?> connections = (Map<?, ?>) interfaces.get(behaviour);
            if (connections == null) return null;
            for (Object connection : connections.values()) {
                if (connection == null) continue;
                FluidStack stack = (FluidStack) providedFluid.invoke(connection);
                if (stack != null && !stack.isEmpty()) return stack;
            }
        } catch (ReflectiveOperationException | IllegalArgumentException | ClassCastException ignored) {
            // not a Create smart block entity
        }
        return null;
    }
}
