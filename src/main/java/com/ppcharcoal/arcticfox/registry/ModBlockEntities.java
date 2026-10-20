package com.ppcharcoal.arcticfox.registry;

import com.ppcharcoal.arcticfox.ArcticFox;
import com.ppcharcoal.arcticfox.block.CabinStoveBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ArcticFox.MOD_ID);

    public static final Supplier<BlockEntityType<CabinStoveBlockEntity>> CABIN_STOVE = BLOCK_ENTITIES.register("cabin_stove",
            () -> BlockEntityType.Builder.of(CabinStoveBlockEntity::new, ModBlocks.CABIN_STOVE.get()).build(null));

    private ModBlockEntities() {
    }
}
