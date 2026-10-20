package com.ppcharcoal.arcticfox.registry;

import com.ppcharcoal.arcticfox.ArcticFox;
import net.minecraft.world.level.block.Block;
import com.ppcharcoal.arcticfox.block.CabinStoveBlock;
import com.ppcharcoal.arcticfox.block.FlickeringLampBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ArcticFox.MOD_ID);

    /**
     * Brittle volcanic rubble produced whenever lava and water meet. It has no loot table,
     * so it drops nothing - not even with Silk Touch - which kills cobblestone generators.
     */
    public static final DeferredBlock<Block> SCORIA = BLOCKS.registerSimpleBlock("scoria",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_BLACK)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(1.5F, 3.0F)
                    .sound(SoundType.TUFF)
                    .noLootTable());

    public static final DeferredBlock<FlickeringLampBlock> FLICKERING_LAMP = BLOCKS.registerBlock("flickering_lamp", FlickeringLampBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5F, 6.0F).sound(SoundType.LANTERN)
                    .lightLevel(state -> state.getValue(FlickeringLampBlock.LIT) ? 13 : 2).requiresCorrectToolForDrops());

    public static final DeferredBlock<CabinStoveBlock> CABIN_STOVE = BLOCKS.registerBlock("cabin_stove", CabinStoveBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3.5F, 6.0F).sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(CabinStoveBlock.LIT) ? 11 : 0).requiresCorrectToolForDrops().noOcclusion());

    private ModBlocks() {
    }
}
