package com.ppcharcoal.arcticfox.entity;

import com.ppcharcoal.arcticfox.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Shared rules for the Wendigo: who it can stalk, and where light keeps it away. */
public final class WendigoRules {
    /** Extra players that are not in the level's player list (test fake players). */
    public static final List<Player> EXTRA_PLAYERS = new ArrayList<>();

    private static final Map<UUID, long[]> LIT_CACHE = new java.util.HashMap<>();

    private WendigoRules() {
    }

    /** Every player in the level that can see or be seen (not spectators). */
    public static List<Player> players(Level level) {
        List<Player> list = new ArrayList<>(level.players());
        for (Player p : EXTRA_PLAYERS) if (p.level() == level && !list.contains(p)) list.add(p);
        list.removeIf(Player::isSpectator);
        return list;
    }

    /** A player the Wendigo may stalk: alive, in survival/adventure, on the surface, not standing in the light. */
    public static boolean canStalk(Player player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator() && !isUnderground(player) && !isInLight(player);
    }

    /**
     * Underground: no open sky above and more than 6 blocks of ground/rock overhead (caves, mines, tunnels).
     * A house with a roof is still the surface.
     */
    public static boolean isUnderground(net.minecraft.world.entity.Entity entity) {
        Level level = entity.level();
        BlockPos pos = entity.blockPosition();
        if (level.canSeeSky(pos.above())) return false;
        int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        return top - pos.getY() > 6;
    }

    /**
     * True if the player stands in light from a light source: block light of at least {@code wendigo.safeLight} at their
     * feet or eyes, or a light-emitting block close enough to give that much (also counts freshly placed torches before
     * the light engine has caught up). Cached for half a second per player.
     */
    public static boolean isInLight(Player player) {
        int threshold = Config.WENDIGO_SAFE_LIGHT.get();
        if (threshold <= 0) return false;
        Level level = player.level();
        long now = level.getGameTime();
        long[] cached = LIT_CACHE.get(player.getUUID());
        long posKey = player.blockPosition().asLong();
        if (cached != null && cached[0] == posKey && now - cached[1] < 10 && cached[1] <= now) return cached[2] == 1;
        boolean lit = computeLit(level, player.blockPosition(), threshold);
        LIT_CACHE.put(player.getUUID(), new long[]{posKey, now, lit ? 1 : 0});
        return lit;
    }

    public static void clearCache() {
        LIT_CACHE.clear();
    }

    private static boolean computeLit(Level level, BlockPos feet, int threshold) {
        if (level.getBrightness(LightLayer.BLOCK, feet) >= threshold || level.getBrightness(LightLayer.BLOCK, feet.above()) >= threshold) {
            return true;
        }
        int r = Math.max(0, 15 - threshold);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r + 1; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    int manhattan = Math.abs(dx) + Math.abs(dz) + Math.max(0, Math.abs(dy - (dy > 0 ? 1 : 0)));
                    if (manhattan > r) continue;
                    m.set(feet.getX() + dx, feet.getY() + dy, feet.getZ() + dz);
                    if (!level.isLoaded(m)) continue;
                    int emission = level.getBlockState(m).getLightEmission(level, m);
                    if (emission - manhattan >= threshold) return true;
                }
            }
        }
        return false;
    }
}
