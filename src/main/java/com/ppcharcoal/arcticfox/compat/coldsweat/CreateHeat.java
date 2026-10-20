package com.ppcharcoal.arcticfox.compat.coldsweat;

import com.momosoftworks.coldsweat.api.event.core.registry.BlockTempRegisterEvent;
import com.momosoftworks.coldsweat.api.temperature.block_temp.BlockTemp;
import com.ppcharcoal.arcticfox.ArcticFox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * Create x Cold Sweat: a working factory keeps you warm (replaces the "Create: Cold Sweat" addon).
 * All temperatures are in Cold Sweat MC units (1 MC = 25 C) and fade out over the block's range.
 * <ul>
 *     <li>Blaze burners: smouldering +3 C, fading +5 C, kindled +10 C, seething +20 C (range 8).</li>
 *     <li>Lit blaze burner (the campfire-like one): +6 C.</li>
 *     <li>Steam engines on a running boiler: +8 C (range 7).</li>
 *     <li>Pipes, pumps, valves, basins, spouts and item drains holding a hot fluid (lava!) radiate heat
 *     (+7 C per block for lava, diminishing with many blocks, up to +20 C). Fluid tanks are handled by
 *     Cold Sweat itself (fill level and boiler heat).</li>
 * </ul>
 * Only loaded when Cold Sweat is installed; Create is accessed reflectively.
 */
public final class CreateHeat {
    private static final double C = 1 / 25.0;

    private CreateHeat() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(CreateHeat::onRegister);
    }

    private static Block[] blocksOf(String namespace, String... ids) {
        return Arrays.stream(ids)
                .map(id -> BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath(namespace, id)).orElse(null))
                .filter(Objects::nonNull).filter(b -> b != Blocks.AIR).toArray(Block[]::new);
    }

    private static Block[] blocks(String... ids) {
        return Arrays.stream(ids)
                .map(id -> BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("create", id)).orElse(null))
                .filter(Objects::nonNull).filter(b -> b != Blocks.AIR).toArray(Block[]::new);
    }

    private static void onRegister(BlockTempRegisterEvent event) {
        // Arctic Fox's own cast-iron stove: +18 C while burning (range 7)
        event.register(new StoveTemp(com.ppcharcoal.arcticfox.registry.ModBlocks.CABIN_STOVE.get()));
        if (!com.ppcharcoal.arcticfox.Config.CREATE_HEAT.get()) return;
        Block[] burners = blocks("blaze_burner");
        if (burners.length == 0) return; // Create not installed
        event.register(new BlazeBurnerTemp(burners));
        event.register(new SimpleHeat(6 * C, 6, 12 * C, blocks("lit_blaze_burner")));
        event.register(new SteamEngineTemp(blocks("steam_engine")));
        // Register first so these win over Cold Sweat's own (weaker, plain-pipe-only) fluid pipe temperature
        event.registerFirst(new FluidHolderTemp(blocks("fluid_pipe", "encased_fluid_pipe", "glass_fluid_pipe",
                "smart_fluid_pipe", "mechanical_pump", "fluid_valve", "basin", "item_drain", "spout")));
        // Create: New Age heat network (heaters, heat pipes/pumps, Stirling engines, solar plates, reactor vents)
        Block[] newAgeHeat = blocksOf("create_new_age", "heater", "heat_pipe", "heat_pump", "stirling_engine",
                "basic_solar_heating_plate", "advanced_solar_heating_plate", "reactor_heat_vent", "reactor_rod");
        if (newAgeHeat.length > 0) {
            event.register(new NewAgeHeatTemp(newAgeHeat));
            event.register(new SimpleHeat(25 * C, 8, 40 * C, blocksOf("create_new_age", "corium")));
            event.register(new SimpleHeat(6 * C, 5, 12 * C, blocksOf("create_new_age", "solid_corium")));
        }
        ArcticFox.LOGGER.info("Registered Create heat sources for Cold Sweat");
    }

    static class StoveTemp extends SimpleHeat {
        StoveTemp(Block... blocks) {
            super(18 * C, 7, 26 * C, blocks);
        }

        @Override
        public boolean isValid(Level level, BlockPos pos, BlockState state) {
            return state.hasProperty(com.ppcharcoal.arcticfox.block.CabinStoveBlock.LIT) && state.getValue(com.ppcharcoal.arcticfox.block.CabinStoveBlock.LIT);
        }
    }

    /** Fixed heat while the block exists. */
    static class SimpleHeat extends BlockTemp {
        private final double temp, range, max;

        SimpleHeat(double temp, double range, double max, Block... blocks) {
            super(blocks);
            this.temp = temp;
            this.range = range;
            this.max = max;
        }

        @Override
        public double getTemperature(Level level, @Nullable LivingEntity entity, BlockState state, BlockPos pos, double distance) {
            return temp;
        }

        @Override
        public double getRange(@Nullable LivingEntity entity, Level level, BlockPos pos, BlockState state) {
            return range;
        }

        @Override
        public double getMaxEffect(@Nullable LivingEntity entity, Level level, BlockPos pos, BlockState state) {
            return max;
        }
    }

    static class BlazeBurnerTemp extends BlockTemp {
        BlazeBurnerTemp(Block... blocks) {
            super(blocks);
        }

        @Override
        public double getTemperature(Level level, @Nullable LivingEntity entity, BlockState state, BlockPos pos, double distance) {
            for (Property<?> property : state.getProperties()) {
                if (!property.getName().equals("blaze")) continue;
                String heat = state.getValue(property).toString().toLowerCase(java.util.Locale.ROOT);
                return switch (heat) {
                    case "smouldering" -> 3 * C;
                    case "fading" -> 5 * C;
                    case "kindled" -> 10 * C;
                    case "seething" -> 20 * C;
                    default -> 0;
                };
            }
            return 0;
        }

        @Override
        public double getRange(@Nullable LivingEntity entity, Level level, BlockPos pos, BlockState state) {
            return 8;
        }

        @Override
        public double getMaxEffect(@Nullable LivingEntity entity, Level level, BlockPos pos, BlockState state) {
            return 30 * C;
        }
    }

    static class SteamEngineTemp extends BlockTemp {
        SteamEngineTemp(Block... blocks) {
            super(blocks);
        }

        @Override
        public double getTemperature(Level level, @Nullable LivingEntity entity, BlockState state, BlockPos pos, double distance) {
            return Reflect.boilerHeat(level.getBlockEntity(pos)) > 0 ? 8 * C : 0;
        }

        @Override
        public double getRange(@Nullable LivingEntity entity, Level level, BlockPos pos, BlockState state) {
            return 7;
        }

        @Override
        public double getMaxEffect(@Nullable LivingEntity entity, Level level, BlockPos pos, BlockState state) {
            return 16 * C;
        }
    }

    static class FluidHolderTemp extends BlockTemp {
        FluidHolderTemp(Block... blocks) {
            super(blocks);
        }

        @Override
        public double getTemperature(Level level, @Nullable LivingEntity entity, BlockState state, BlockPos pos, double distance) {
            FluidStack fluid = Reflect.fluidIn(level, pos, state);
            if (fluid == null || fluid.isEmpty()) return 0;
            int kelvin = fluid.getFluid().getFluidType().getTemperature(fluid);
            if (kelvin <= 320) return 0;
            // lava (1300 K) -> +7 C per block
            return Math.min(1.0, (kelvin - 300) / 1000.0) * 7 * C;
        }

        @Override
        public double getRange(@Nullable LivingEntity entity, Level level, BlockPos pos, BlockState state) {
            return 6;
        }

        @Override
        public double getMaxEffect(@Nullable LivingEntity entity, Level level, BlockPos pos, BlockState state) {
            return 20 * C;
        }

        @Override
        public boolean isLogarithmic(@Nullable LivingEntity entity, Level level, BlockPos pos, BlockState state) {
            return true;
        }
    }

    /**
     * Create: New Age stores heat in its block entities (HeatBlockEntity#getHeat / #maxHeat). A full block gives +12 C;
     * a heater (which has a blaze-burner style heat level) uses that level instead.
     */
    static class NewAgeHeatTemp extends BlockTemp {
        private static Method getHeat, maxHeat;
        private static boolean init;

        NewAgeHeatTemp(Block... blocks) {
            super(blocks);
        }

        private static synchronized void init() {
            if (init) return;
            init = true;
            try {
                Class<?> heat = Class.forName("org.antarcticgardens.cna.content.heat.HeatBlockEntity");
                getHeat = heat.getMethod("getHeat");
                maxHeat = heat.getMethod("maxHeat");
            } catch (ReflectiveOperationException e) {
                ArcticFox.LOGGER.warn("Create: New Age heat reading disabled", e);
            }
        }

        @Override
        public double getTemperature(Level level, @Nullable LivingEntity entity, BlockState state, BlockPos pos, double distance) {
            for (Property<?> property : state.getProperties()) {
                if (property.getValue("seething").isPresent() || property.getName().equals("blaze")) {
                    String heat = state.getValue(property).toString().toLowerCase(java.util.Locale.ROOT);
                    switch (heat) {
                        case "smouldering": return 3 * C;
                        case "fading": return 5 * C;
                        case "kindled": return 10 * C;
                        case "seething": return 20 * C;
                        default: break;
                    }
                }
            }
            init();
            BlockEntity be = level.getBlockEntity(pos);
            if (getHeat == null || be == null || !getHeat.getDeclaringClass().isInstance(be)) return 0;
            try {
                double heat = ((Number) getHeat.invoke(be)).doubleValue();
                double max = ((Number) maxHeat.invoke(be)).doubleValue();
                if (heat <= 0 || max <= 0) return 0;
                return Math.min(1.0, heat / max) * 12 * C;
            } catch (ReflectiveOperationException e) {
                return 0;
            }
        }

        @Override
        public double getRange(@Nullable LivingEntity entity, Level level, BlockPos pos, BlockState state) {
            return 7;
        }

        @Override
        public double getMaxEffect(@Nullable LivingEntity entity, Level level, BlockPos pos, BlockState state) {
            return 24 * C;
        }

        @Override
        public boolean isLogarithmic(@Nullable LivingEntity entity, Level level, BlockPos pos, BlockState state) {
            return true;
        }
    }

    /** Reflective access to Create internals, so we never need Create on the compile classpath. */
    static final class Reflect {
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

        static int boilerHeat(@Nullable BlockEntity engine) {
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
        static FluidStack fluidIn(Level level, BlockPos pos, BlockState state) {
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
}
