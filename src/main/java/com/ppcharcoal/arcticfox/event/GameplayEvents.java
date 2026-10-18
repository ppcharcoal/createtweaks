package com.ppcharcoal.arcticfox.event;

import com.ppcharcoal.arcticfox.ArcticFox;
import com.ppcharcoal.arcticfox.Config;
import com.ppcharcoal.arcticfox.ModTags;
import com.ppcharcoal.arcticfox.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = ArcticFox.MOD_ID)
public final class GameplayEvents {
    private GameplayEvents() {
    }

    /** Swap any generator output (cobblestone, stone, ...) for useless scoria. */
    public static BlockState scoriafy(BlockState state) {
        if (state != null && Config.SCORIA_GENERATORS.get() && state.is(ModTags.FLUID_GENERATOR_OUTPUTS)) {
            return ModBlocks.SCORIA.get().defaultBlockState();
        }
        return state;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFluidPlaceBlock(BlockEvent.FluidPlaceBlockEvent event) {
        event.setNewState(scoriafy(event.getNewState()));
    }

    // ---------------------------------------------------------------- dimensions

    private static boolean isSealed(net.minecraft.resources.ResourceKey<Level> dimension) {
        return (dimension == Level.NETHER && Config.BLOCK_NETHER.get())
                || (dimension == Level.END && Config.BLOCK_END.get());
    }

    @SubscribeEvent
    public static void onPortalSpawn(BlockEvent.PortalSpawnEvent event) {
        if (Config.BLOCK_NETHER.get()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onTravel(EntityTravelToDimensionEvent event) {
        if (isSealed(event.getDimension())) {
            event.setCanceled(true);
            if (event.getEntity() instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable("message.arcticfox.dimension_sealed"), true);
            }
        }
    }

    // ---------------------------------------------------------------- mobs

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!Config.BANISH_ILLAGERS_AND_VILLAGERS.get()) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity.getType().is(ModTags.BANISHED)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!Config.RESTRICT_SURFACE_MONSTERS.get()) {
            return;
        }
        MobSpawnType type = event.getSpawnType();
        if (type != MobSpawnType.NATURAL && type != MobSpawnType.CHUNK_GENERATION && type != MobSpawnType.PATROL) {
            return;
        }
        Entity mob = event.getEntity();
        if (mob.getType().getCategory() != MobCategory.MONSTER || mob.getType().is(ModTags.SURFACE_MONSTERS)) {
            return;
        }
        LevelAccessor level = event.getLevel();
        if (level.dimensionType().hasSkyLight()
                && level.canSeeSky(BlockPos.containing(event.getX(), event.getY(), event.getZ()))) {
            event.setSpawnCancelled(true);
        }
    }
}
