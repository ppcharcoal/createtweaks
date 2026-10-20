package com.ppcharcoal.arcticfox.block;

import com.ppcharcoal.arcticfox.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CabinStoveBlockEntity extends BlockEntity {
    /** Up to 3 in-game days of fuel. */
    public static final int MAX_FUEL = 3 * 24000;
    private int fuel;

    public CabinStoveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CABIN_STOVE.get(), pos, state);
    }

    public int getFuel() {
        return fuel;
    }

    public boolean addFuel(int ticks) {
        if (fuel >= MAX_FUEL) return false;
        fuel = Math.min(MAX_FUEL, fuel + ticks);
        setChanged();
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CabinStoveBlockEntity stove) {
        if (stove.fuel > 0) stove.fuel--;
        boolean lit = stove.fuel > 0;
        if (state.getValue(CabinStoveBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(CabinStoveBlock.LIT, lit), Block.UPDATE_ALL);
        }
        if (level.getGameTime() % 200 == 0) stove.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Fuel", fuel);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuel = tag.getInt("Fuel");
    }
}
