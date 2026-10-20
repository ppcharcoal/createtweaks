package com.ppcharcoal.arcticfox.registry;

import com.mojang.serialization.MapCodec;
import com.ppcharcoal.arcticfox.ArcticFox;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Locale;
import java.util.function.Supplier;

public final class ModConditions {
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, ArcticFox.MOD_ID);

    public static final Supplier<MapCodec<NetherResourcesAvailable>> NETHER_RESOURCES_AVAILABLE =
            CONDITIONS.register("nether_resources_available", () -> NetherResourcesAvailable.CODEC);

    /**
     * True when a mod that produces Nether resources in the Overworld (e.g. Create: Netherless)
     * is installed. Used to keep Nether-material advancements that are still reachable.
     */
    public enum NetherResourcesAvailable implements ICondition {
        INSTANCE;

        public static final MapCodec<NetherResourcesAvailable> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public boolean test(IContext context) {
            return ModList.get().getMods().stream()
                    .anyMatch(mod -> mod.getModId().toLowerCase(Locale.ROOT).contains("netherless"));
        }

        @Override
        public MapCodec<? extends ICondition> codec() {
            return CODEC;
        }
    }

    private ModConditions() {
    }
}
