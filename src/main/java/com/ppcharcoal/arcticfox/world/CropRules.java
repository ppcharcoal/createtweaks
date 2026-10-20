package com.ppcharcoal.arcticfox.world;

import com.ppcharcoal.arcticfox.ArcticFox;
import com.ppcharcoal.arcticfox.Config;
import com.ppcharcoal.arcticfox.compat.ColdSweatCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;

/**
 * Crops only grow when they are:
 * <ol>
 *     <li><b>sheltered</b> - a roof of any kind above them (glass greenhouse, cave ceiling); open-sky crops never grow,</li>
 *     <li><b>warm</b> - above 0 C. With Cold Sweat the real world temperature is used (hearths, depth, fires all count).
 *     Without it: underground with no sky light, or a heat source (campfire, furnace, magma, lava, blaze burner)
 *     within a few blocks,</li>
 *     <li><b>watered</b> - crops on farmland need it to be moist (Create sprinklers, waterlogged blocks or water nearby).</li>
 * </ol>
 */
@EventBusSubscriber(modid = ArcticFox.MOD_ID)
public final class CropRules {
    public static final TagKey<Block> HEAT_SOURCES = TagKey.create(Registries.BLOCK, ArcticFox.id("greenhouse_heat_sources"));
    public static final TagKey<Block> IGNORED = TagKey.create(Registries.BLOCK, ArcticFox.id("ignores_crop_rules"));

    public enum Verdict { OK, EXPOSED, COLD, DRY }

    private CropRules() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onCropGrow(CropGrowEvent.Pre event) {
        if (!Config.CROP_RULES.get() || !(event.getLevel() instanceof Level level) || level.isClientSide()) return;
        if (check(level, event.getPos(), event.getState()) != Verdict.OK) {
            event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
        }
    }

    public static Verdict check(Level level, BlockPos pos, BlockState state) {
        if (state.is(IGNORED) || !level.dimensionType().natural()) return Verdict.OK;
        // 1. sheltered: something precipitation-blocking above the plant
        if (level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() <= pos.getY()) {
            return Verdict.EXPOSED;
        }
        // 2. warm
        if (!isWarm(level, pos)) return Verdict.COLD;
        // 3. watered
        BlockState soil = level.getBlockState(pos.below());
        if (soil.getBlock() instanceof FarmBlock && soil.getValue(FarmBlock.MOISTURE) == 0) return Verdict.DRY;
        return Verdict.OK;
    }

    public static boolean isWarm(Level level, BlockPos pos) {
        Double celsius = ColdSweatCompat.temperatureCelsius(level, pos);
        if (celsius != null) {
            return celsius > Config.CROP_MIN_TEMPERATURE.get();
        }
        if (level.getBrightness(LightLayer.SKY, pos) == 0) return true; // deep enough underground
        return hasHeatNearby(level, pos, 5);
    }

    private static boolean hasHeatNearby(LevelAccessor level, BlockPos pos, int radius) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-radius, -2, -radius), pos.offset(radius, 2, radius))) {
            if (level.getBlockState(p).is(HEAT_SOURCES)) return true;
        }
        return false;
    }
}
