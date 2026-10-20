package com.ppcharcoal.arcticfox.entity;

import com.ppcharcoal.arcticfox.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.level.Level;

/** A dark-faced, thick-fleeced sheep adapted to the cold. Breeds into more winter sheep. */
public class WinterSheep extends Sheep {
    public WinterSheep(EntityType<? extends Sheep> type, Level level) {
        super(type, level);
    }

    @Override
    public Sheep getBreedOffspring(ServerLevel level, AgeableMob partner) {
        WinterSheep child = ModEntities.WINTER_SHEEP.get().create(level);
        if (child != null) {
            Sheep other = partner instanceof Sheep sheep ? sheep : this;
            child.setColor(this.random.nextBoolean() ? this.getColor() : other.getColor());
        }
        return child;
    }
}
