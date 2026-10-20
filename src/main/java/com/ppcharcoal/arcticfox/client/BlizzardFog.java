package com.ppcharcoal.arcticfox.client;

import com.mojang.blaze3d.shaders.FogShape;
import com.ppcharcoal.arcticfox.ArcticFox;
import com.ppcharcoal.arcticfox.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Thick white-out fog when you are outdoors in a blizzard (thunder + snow). */
@EventBusSubscriber(modid = ArcticFox.MOD_ID, value = Dist.CLIENT)
public final class BlizzardFog {
    private static float strength;

    private BlizzardFog() {
    }

    private static float target(ClientLevel level, Entity camera) {
        if (!Config.SPEC.isLoaded() || !Config.BLIZZARD_FOG.get()) return 0;
        float thunder = level.getThunderLevel(1.0F);
        if (thunder <= 0) return 0;
        BlockPos pos = camera.blockPosition();
        if (level.getBiome(pos).value().getPrecipitationAt(pos) != Biome.Precipitation.SNOW) return 0;
        if (level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() > camera.getEyeY() + 1) return 0; // under a roof
        return thunder;
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) return;
        strength = Mth.lerp(0.02F, strength, target(mc.level, event.getCamera().getEntity()));
        if (strength < 0.01F) return;
        float far = Mth.lerp(strength, event.getFarPlaneDistance(), 20.0F);
        event.setFarPlaneDistance(Math.min(event.getFarPlaneDistance(), far));
        event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), far * 0.05F));
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }

    /**
     * Snow-white by day, a dark blue-grey murk at night: the fog follows the daylight, so a night blizzard is
     * properly dark instead of glowing white.
     */
    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (strength < 0.01F) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float partial = (float) event.getPartialTick();
        // 1 at noon, 0 at midnight (also darkened by the storm itself)
        float daylight = Mth.clamp((mc.level.getSkyDarken(partial) - 0.2F) / 0.8F, 0.0F, 1.0F);
        float r = Mth.lerp(daylight, 0.055F, 0.82F);
        float g = Mth.lerp(daylight, 0.065F, 0.86F);
        float b = Mth.lerp(daylight, 0.095F, 0.90F);
        // lightning-free storm, but your own torch or lantern lights up the snow around you a little
        int block = mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, event.getCamera().getBlockPosition());
        float glow = (1 - daylight) * block / 15.0F * 0.18F;
        event.setRed(Mth.lerp(strength, event.getRed(), r + glow));
        event.setGreen(Mth.lerp(strength, event.getGreen(), g + glow * 0.85F));
        event.setBlue(Mth.lerp(strength, event.getBlue(), b + glow * 0.6F));
    }
}
