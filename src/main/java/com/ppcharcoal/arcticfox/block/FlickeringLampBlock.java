package com.ppcharcoal.arcticfox.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** An old industrial ceiling lamp that mostly works, but flickers now and then. */
public class FlickeringLampBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public FlickeringLampBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LIT, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(LIT, true);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        if (!level.isClientSide && !oldState.is(this)) level.scheduleTick(pos, this, 20 + level.random.nextInt(80));
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true; // restarts the flicker cycle for lamps placed during world generation
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) level.scheduleTick(pos, this, 1 + random.nextInt(40));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean lit = state.getValue(LIT);
        if (lit) {
            if (random.nextFloat() < 0.35F) {
                level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_CLIENTS);
                level.scheduleTick(pos, this, 2 + random.nextInt(5));          // short blackout
            } else {
                level.scheduleTick(pos, this, 30 + random.nextInt(120));       // steady light
            }
        } else {
            level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_CLIENTS);
            level.scheduleTick(pos, this, random.nextFloat() < 0.5F ? 2 + random.nextInt(4) : 30 + random.nextInt(100));
        }
    }
}
