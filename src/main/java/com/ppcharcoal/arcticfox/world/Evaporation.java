package com.ppcharcoal.arcticfox.world;

import com.ppcharcoal.arcticfox.ArcticFox;
import com.ppcharcoal.arcticfox.Config;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Water source blocks that get placed after world generation (buckets, dispensers, Create pipes...)
 * evaporate after a short while. Natural water and waterlogged blocks are never touched, so water has
 * to be kept in tanks, and permanent water features (e.g. a fan washing catalyst) need waterlogging.
 */
@EventBusSubscriber(modid = ArcticFox.MOD_ID)
public final class Evaporation {
    private static final String DATA_NAME = ArcticFox.MOD_ID + "_evaporation";

    /** Position of an ice block a player just broke, and the tick it happened (ice melting into water stays). */
    private static long lastIceBreakPos = Long.MIN_VALUE;
    private static long lastIceBreakTick = Long.MIN_VALUE;

    private Evaporation() {
    }

    public static boolean isPlacedWaterSource(BlockState state) {
        return state.is(Blocks.WATER) && state.getFluidState().isSource();
    }

    @SubscribeEvent
    public static void onIceBroken(BlockEvent.BreakEvent event) {
        if (event.getState().is(Blocks.ICE) && event.getLevel() instanceof Level level) {
            lastIceBreakPos = event.getPos().asLong();
            lastIceBreakTick = level.getGameTime();
        }
    }

    @SubscribeEvent
    public static void onBlockChanged(BlockEvent.NeighborNotifyEvent event) {
        if (!Config.EVAPORATE_PLACED_WATER.get()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!isPlacedWaterSource(event.getState())) return;
        BlockPos pos = event.getPos();
        if (pos.asLong() == lastIceBreakPos && level.getGameTime() - lastIceBreakTick <= 1) return;
        get(level).schedule(pos, level.getGameTime() + Config.EVAPORATION_SECONDS.get() * 20L);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 10 != 0) return;
        Data data = level.getDataStorage().get(factory(), DATA_NAME);
        if (data != null && !data.pending.isEmpty()) {
            data.process(level);
        }
    }

    public static Data get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory(), DATA_NAME);
    }

    private static SavedData.Factory<Data> factory() {
        return new SavedData.Factory<>(Data::new, Data::load, null);
    }

    public static final class Data extends SavedData {
        private final Long2LongOpenHashMap pending = new Long2LongOpenHashMap();

        public void schedule(BlockPos pos, long dueTick) {
            pending.put(pos.asLong(), dueTick);
            setDirty();
        }

        public int size() {
            return pending.size();
        }

        void process(ServerLevel level) {
            long now = level.getGameTime();
            boolean changed = false;
            ObjectIterator<Long2LongMap.Entry> it = pending.long2LongEntrySet().fastIterator();
            while (it.hasNext()) {
                Long2LongMap.Entry entry = it.next();
                if (entry.getLongValue() > now) continue;
                BlockPos pos = BlockPos.of(entry.getLongKey());
                if (!level.isLoaded(pos)) continue; // try again once the chunk is loaded
                it.remove();
                changed = true;
                if (isPlacedWaterSource(level.getBlockState(pos))) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.6F);
                    level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0.01);
                }
            }
            if (changed) setDirty();
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            long[] positions = new long[pending.size()];
            long[] due = new long[pending.size()];
            int i = 0;
            for (Long2LongMap.Entry entry : pending.long2LongEntrySet()) {
                positions[i] = entry.getLongKey();
                due[i] = entry.getLongValue();
                i++;
            }
            tag.putLongArray("positions", positions);
            tag.putLongArray("due", due);
            return tag;
        }

        static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data data = new Data();
            long[] positions = tag.getLongArray("positions");
            long[] due = tag.getLongArray("due");
            for (int i = 0; i < Math.min(positions.length, due.length); i++) {
                data.pending.put(positions[i], due[i]);
            }
            return data;
        }
    }
}
