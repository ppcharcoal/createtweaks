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

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (strength < 0.01F) return;
        event.setRed(Mth.lerp(strength, event.getRed(), 0.82F));
        event.setGreen(Mth.lerp(strength, event.getGreen(), 0.86F));
        event.setBlue(Mth.lerp(strength, event.getBlue(), 0.90F));
    }
}
