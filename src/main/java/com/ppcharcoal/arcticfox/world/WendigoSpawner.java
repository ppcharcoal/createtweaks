package com.ppcharcoal.arcticfox.world;

import com.ppcharcoal.arcticfox.ArcticFox;
import com.ppcharcoal.arcticfox.Config;
import com.ppcharcoal.arcticfox.entity.Wendigo;
import com.ppcharcoal.arcticfox.entity.WendigoRules;
import com.ppcharcoal.arcticfox.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * At most one Wendigo per night for the whole world. Every 10 seconds at night there is a chance that it picks one of
 * the players who are out in the open and not standing in the light, and starts stalking them from behind.
 */
@EventBusSubscriber(modid = ArcticFox.MOD_ID)
public final class WendigoSpawner {
    private WendigoSpawner() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        if (level.getGameTime() % 200 != 13 || !Config.WENDIGO.get() || level.getDifficulty() == Difficulty.PEACEFUL) return;
        if (!level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING)) return;
        if (level.getRandom().nextDouble() >= Config.WENDIGO_CHANCE.get()) return;
        trySpawnTonight(level);
    }

    /** The night number (counting from 0), or -1 during the day. */
    public static long nightIndex(ServerLevel level) {
        return WinterWeather.isNight(level) ? WinterWeather.dayIndex(level.getDayTime()) : -1;
    }

    /** Players a Wendigo could start stalking: out in the open, in the dark, survival or adventure. */
    public static List<Player> candidates(ServerLevel level) {
        List<Player> list = new ArrayList<>();
        for (Player p : WendigoRules.players(level)) {
            if (WendigoRules.canStalk(p) && level.canSeeSky(p.blockPosition().above())) list.add(p);
        }
        return list;
    }

    /** Spawns tonight's Wendigo if there hasn't been one yet tonight. Returns it, or null. */
    @Nullable
    public static Wendigo trySpawnTonight(ServerLevel level) {
        long night = nightIndex(level);
        if (night < Config.WENDIGO_FIRST_NIGHT.get()) return null;
        Nights nights = Nights.get(level);
        if (nights.lastNight == night) return null; // only one a night
        List<Player> players = candidates(level);
        if (players.isEmpty()) return null;
        Player prey = players.get(level.getRandom().nextInt(players.size()));
        Wendigo wendigo = spawnFor(level, prey);
        if (wendigo != null) {
            nights.lastNight = night;
            nights.setDirty();
        }
        return wendigo;
    }

    /** Spawns a Wendigo 34-42 blocks behind the player, on open ground. Returns null if no spot was found. */
    @Nullable
    public static Wendigo spawnFor(ServerLevel level, Player player) {
        RandomSource random = level.getRandom();
        Vec3 look = player.getViewVector(1.0F);
        double behind = Math.atan2(-look.z, -look.x);
        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = behind + (random.nextDouble() - 0.5) * Math.PI * (attempt < 10 ? 1.0 : 2.0);
            double r = 34 + random.nextDouble() * 8;
            int x = Mth.floor(player.getX() + Math.cos(angle) * r);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * r);
            if (!level.hasChunk(x >> 4, z >> 4)) continue;
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (!level.getFluidState(top.below()).isEmpty() || !level.getFluidState(top).isEmpty()) continue;
            var ground = level.getBlockState(top.below());
            if (!ground.isFaceSturdy(level, top.below(), Direction.UP) && !ground.is(net.minecraft.world.level.block.Blocks.SNOW)) continue;
            if (Math.abs(top.getY() - player.getY()) > 20) continue;
            Wendigo wendigo = ModEntities.WENDIGO.get().create(level);
            if (wendigo == null) return null;
            wendigo.moveTo(x + 0.5, top.getY(), z + 0.5, random.nextFloat() * 360, 0);
            if (!level.noCollision(wendigo)) continue;
            wendigo.finalizeSpawn(level, level.getCurrentDifficultyAt(top), MobSpawnType.EVENT, null);
            wendigo.setPrey(player);
            level.addFreshEntity(wendigo);
            ArcticFox.LOGGER.debug("A Wendigo is stalking {}", player.getName().getString());
            return wendigo;
        }
        return null;
    }

    /** Remembers the last night a Wendigo came, so restarts and multiple players still mean one a night. */
    public static final class Nights extends SavedData {
        private static final String NAME = "arcticfox_wendigo";
        public long lastNight = -1;

        public static Nights get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Nights::new, Nights::load, null), NAME);
        }

        private static Nights load(CompoundTag tag, HolderLookup.Provider registries) {
            Nights n = new Nights();
            n.lastNight = tag.getLong("LastNight");
            return n;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putLong("LastNight", lastNight);
            return tag;
        }
    }
}
