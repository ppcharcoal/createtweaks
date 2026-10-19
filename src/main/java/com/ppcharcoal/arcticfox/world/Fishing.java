package com.ppcharcoal.arcticfox.world;

import com.ppcharcoal.arcticfox.ArcticFox;
import com.ppcharcoal.arcticfox.Config;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;

/**
 * Fish only live in natural water: a catch only counts if the water the bobber is in is connected
 * to at least one water source that generated with the world (not placed from a bucket, pipe or tank,
 * and not a player-waterlogged block). Ice fishing through a hole in a frozen lake works fine.
 */
@EventBusSubscriber(modid = ArcticFox.MOD_ID)
public final class Fishing {
    private static final int SEARCH_LIMIT = 4096;

    private Fishing() {
    }

    @SubscribeEvent
    public static void onFished(ItemFishedEvent event) {
        if (!Config.NATURAL_FISHING_ONLY.get() || !(event.getHookEntity().level() instanceof ServerLevel level)) return;
        if (!isNaturalWater(level, event.getHookEntity().blockPosition())) {
            event.setCanceled(true);
            event.getEntity().displayClientMessage(Component.translatable("message.arcticfox.no_fish"), true);
        }
    }

    public static boolean isNaturalWater(ServerLevel level, BlockPos start) {
        if (!level.getFluidState(start).is(FluidTags.WATER)) {
            start = start.below();
            if (!level.getFluidState(start).is(FluidTags.WATER)) return false;
        }
        Evaporation.Data data = Evaporation.get(level);
        LongOpenHashSet seen = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        queue.enqueue(start.asLong());
        seen.add(start.asLong());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        while (!queue.isEmpty() && seen.size() < SEARCH_LIMIT) {
            long current = queue.dequeueLong();
            pos.set(current);
            FluidState fluid = level.getFluidState(pos);
            if (fluid.isSource() && !data.isArtificial(current)) return true;
            for (Direction d : Direction.values()) {
                long next = BlockPos.offset(current, d);
                if (seen.contains(next)) continue;
                pos.set(next);
                if (!level.isLoaded(pos) || !level.getFluidState(pos).is(FluidTags.WATER)) continue;
                seen.add(next);
                queue.enqueue(next);
            }
        }
        // a huge connected body (beyond the search limit) is a lake or ocean
        return seen.size() >= SEARCH_LIMIT;
    }
}
