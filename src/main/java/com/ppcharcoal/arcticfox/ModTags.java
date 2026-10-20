package com.ppcharcoal.arcticfox;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    /** Blocks a fluid interaction is not allowed to create; they become scoria instead. */
    public static final TagKey<Block> FLUID_GENERATOR_OUTPUTS = TagKey.create(Registries.BLOCK, ArcticFox.id("fluid_generator_outputs"));
    /** Blocks that flint (stone tier) tools cannot harvest, but copper tools can. */
    public static final TagKey<Block> NEEDS_COPPER_TOOL = TagKey.create(Registries.BLOCK, ArcticFox.id("needs_copper_tool"));
    public static final TagKey<Block> INCORRECT_FOR_COPPER_TOOL = TagKey.create(Registries.BLOCK, ArcticFox.id("incorrect_for_copper_tool"));

    /** Monsters allowed to spawn naturally on the surface (where they can see the sky). */
    public static final TagKey<EntityType<?>> SURFACE_MONSTERS = TagKey.create(Registries.ENTITY_TYPE, ArcticFox.id("surface_monsters"));
    /** Entities that are removed as soon as they try to join a world. */
    public static final TagKey<EntityType<?>> BANISHED = TagKey.create(Registries.ENTITY_TYPE, ArcticFox.id("banished"));

    private ModTags() {
    }
}
