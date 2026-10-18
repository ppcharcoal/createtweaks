package com.ppcharcoal.arcticfox.entity;

import com.ppcharcoal.arcticfox.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.Level;

/** A shaggy, pale cow that survives the eternal winter. Breeds into more winter cows. */
public class WinterCow extends Cow {
    public WinterCow(EntityType<? extends Cow> type, Level level) {
        super(type, level);
    }

    @Override
    public Cow getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.WINTER_COW.get().create(level);
    }
}
