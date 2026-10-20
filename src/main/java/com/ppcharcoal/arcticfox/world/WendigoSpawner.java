package com.ppcharcoal.arcticfox.world;

import com.ppcharcoal.arcticfox.ArcticFox;
import com.ppcharcoal.arcticfox.Config;
import com.ppcharcoal.arcticfox.entity.Wendigo;
import com.ppcharcoal.arcticfox.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

/** At night, a Wendigo may start stalking a player who is out in the open, appearing behind them in the fog. */
@EventBusSubscriber(modid = ArcticFox.MOD_ID)
public final class WendigoSpawner {
    private WendigoSpawner() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        if (level.getGameTime() % 200 != 13 || !Config.WENDIGO.get() || level.getDifficulty() == Difficulty.PEACEFUL) return;
        if (!WinterWeather.isNight(level) || WinterWeather.dayIndex(level.getDayTime()) < Config.WENDIGO_FIRST_NIGHT.get()) return;
        if (!level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING)) return;
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            if (player.isCreative() || player.isSpectator()) continue;
            if (!level.canSeeSky(player.blockPosition().above())) continue; // only out in the open
            if (!level.getEntitiesOfClass(Wendigo.class, new AABB(player.blockPosition()).inflate(160)).isEmpty()) continue;
            if (random.nextDouble() >= Config.WENDIGO_CHANCE.get()) continue;
            spawnFor(level, player);
        }
    }

    /** Spawns a Wendigo 34-42 blocks behind the player, on open ground. Returns null if no spot was found. */
    @Nullable
    public static Wendigo spawnFor(ServerLevel level, net.minecraft.world.entity.player.Player player) {
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
}
