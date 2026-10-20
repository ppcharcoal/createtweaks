package com.ppcharcoal.arcticfox.registry;

import com.ppcharcoal.arcticfox.ArcticFox;
import com.ppcharcoal.arcticfox.entity.WinterCow;
import com.ppcharcoal.arcticfox.entity.WinterSheep;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = ArcticFox.MOD_ID)
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, ArcticFox.MOD_ID);

    public static final TagKey<Block> WINTER_ANIMALS_SPAWNABLE_ON = TagKey.create(Registries.BLOCK, ArcticFox.id("winter_animals_spawnable_on"));

    public static final DeferredHolder<EntityType<?>, EntityType<WinterCow>> WINTER_COW = ENTITY_TYPES.register("winter_cow",
            () -> EntityType.Builder.<WinterCow>of(WinterCow::new, MobCategory.CREATURE)
                    .sized(0.9F, 1.4F).eyeHeight(1.3F).passengerAttachments(1.36875F).clientTrackingRange(10)
                    .build(ArcticFox.id("winter_cow").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<WinterSheep>> WINTER_SHEEP = ENTITY_TYPES.register("winter_sheep",
            () -> EntityType.Builder.<WinterSheep>of(WinterSheep::new, MobCategory.CREATURE)
                    .sized(0.9F, 1.3F).eyeHeight(1.235F).passengerAttachments(1.2375F).clientTrackingRange(10)
                    .build(ArcticFox.id("winter_sheep").toString()));

    private ModEntities() {
    }

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(WINTER_COW.get(), Cow.createAttributes().build());
        event.put(WINTER_SHEEP.get(), Sheep.createAttributes().build());
    }

    @SubscribeEvent
    public static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(WINTER_COW.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModEntities::canWinterAnimalSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(WINTER_SHEEP.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModEntities::canWinterAnimalSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /** Like vanilla animal spawning, but also allowed on snow (vanilla only allows grass blocks). */
    public static <T extends Animal> boolean canWinterAnimalSpawn(EntityType<T> type, ServerLevelAccessor level,
                                                                  MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return isSpawnableGround(level, pos) && level.getRawBrightness(pos, 0) > 8;
    }

    private static boolean isSpawnableGround(LevelAccessor level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(WINTER_ANIMALS_SPAWNABLE_ON);
    }
}
