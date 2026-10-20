package com.ppcharcoal.arcticfox.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.ppcharcoal.arcticfox.ArcticFox;
import com.ppcharcoal.arcticfox.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

/**
 * Blizzard snowfall: during a snowstorm the snow is driven by the wind - it falls about ten times faster than
 * vanilla snow and is blown in one direction by the wind (gusting, always the same way), in two layers. Calm snowy days keep the vanilla snowfall.
 */
@EventBusSubscriber(modid = ArcticFox.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class BlizzardSnow {
    private static final ResourceLocation SNOW = ResourceLocation.withDefaultNamespace("textures/environment/snow.png");

    private BlizzardSnow() {
    }

    @SubscribeEvent
    public static void onRegisterEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(BuiltinDimensionTypes.OVERWORLD_EFFECTS, new Effects());
    }

    /** The vanilla overworld sky, plus the blizzard snowfall. */
    static final class Effects extends DimensionSpecialEffects.OverworldEffects {
        private final float[] sizeX = new float[1024];
        private final float[] sizeZ = new float[1024];

        Effects() {
            for (int i = 0; i < 32; i++) {
                for (int j = 0; j < 32; j++) {
                    float dz = j - 16, dx = i - 16;
                    float len = Mth.sqrt(dz * dz + dx * dx);
                    sizeX[i << 5 | j] = len == 0 ? 0 : -dx / len;
                    sizeZ[i << 5 | j] = len == 0 ? 0 : dz / len;
                }
            }
        }

        @Override
        public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture lightTexture,
                                         double camX, double camY, double camZ) {
            if (!Config.SPEC.isLoaded() || !Config.NIGHT_SNOWSTORMS.get()) return false;
            float storm = level.getThunderLevel(partialTick);
            float rain = level.getRainLevel(partialTick);
            if (storm < 0.2F || rain <= 0) return false; // calm snow: vanilla
            renderBlizzard(level, ticks, partialTick, lightTexture, camX, camY, camZ, rain, storm);
            return true;
        }

        private void renderBlizzard(ClientLevel level, int ticks, float partialTick, LightTexture lightTexture,
                                    double camX, double camY, double camZ, float rain, float storm) {
            lightTexture.turnOnLightLayer();
            int cx = Mth.floor(camX), cy = Mth.floor(camY), cz = Mth.floor(camZ);
            int range = Minecraft.useFancyGraphics() ? 10 : 5;
            float time = ticks + partialTick;
            // one wind for every storm: a fixed direction in the world (from the north-west), with gusts that change
            // its strength, never its direction. The distance it has carried the snow is integrated in double
            // precision, so the snow never jumps or reverses.
            double t = ticks + (double) partialTick;
            double windTravel = 0.8 * t - 0.2 / 0.045 * Math.cos(0.045 * t) - 0.1 / 0.13 * Math.cos(0.13 * t + 1.7);
            final float windX = 0.6F, windZ = 0.8F;
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(Minecraft.useShaderTransparency());
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, SNOW);
            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
            boolean any = false;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int z = cz - range; z <= cz + range; z++) {
                for (int x = cx - range; x <= cx + range; x++) {
                    int idx = (z - cz + 16) * 32 + x - cx + 16;
                    double hx = sizeX[idx] * 0.5, hz = sizeZ[idx] * 0.5;
                    pos.set(x, camY, z);
                    Biome biome = level.getBiome(pos).value();
                    if (!biome.hasPrecipitation()) continue;
                    int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                    int y0 = Math.max(cy - range, ground), y1 = Math.max(cy + range, ground);
                    int lightY = Math.max(ground, cy);
                    if (y0 == y1) continue;
                    pos.set(x, y0, z);
                    if (biome.getPrecipitationAt(pos) != Biome.Precipitation.SNOW) continue;
                    RandomSource random = RandomSource.create((long) (x * x * 3121 + x * 45238971 ^ z * z * 418711 + z * 13761));
                    double dx = x + 0.5 - camX, dz = z + 0.5 - camZ;
                    float fade = (float) Math.sqrt(dx * dx + dz * dz) / range;
                    float alpha = ((1.0F - fade * fade) * 0.3F + 0.5F) * rain;
                    pos.set(x, lightY, z);
                    int light = LevelRenderer.getLightColor(level, pos);
                    int sky = light >> 16 & 65535, block = light & 65535;
                    int skyL = (sky * 3 + 240) / 4, blockL = (block * 3 + 240) / 4;
                    // the sheet faces the camera; its horizontal axis is (sizeX, sizeZ). Scroll the texture along it by
                    // the wind's component on that axis, so every sheet carries the snow the same way across the world.
                    float along = (float) (windX * sizeX[idx] + windZ * sizeZ[idx]);
                    // two layers in the same wind: a dense one and a lighter, slightly faster one
                    for (int layer = 0; layer < 2; layer++) {
                        float fall = layer == 0 ? 30.0F : 24.0F;              // ticks per texture repeat (vanilla: 512)
                        float v = -((time + random.nextFloat() * fall) % fall) / fall;
                        double speed = layer == 0 ? 0.11 : 0.15; // blocks per tick at full wind
                        float u = random.nextFloat() - (float) ((windTravel * speed * along) % 1.0);
                        float wobble = (float) (random.nextGaussian() * 0.02);
                        float a = alpha * (layer == 0 ? 1.0F : 0.6F) * Math.min(1.0F, storm * 1.5F);
                        float off = layer * 0.37F;
                        quad(buffer, x, z, y0, y1, hx, hz, camX, camY, camZ, u + off, v + wobble + off, a, skyL, blockL);
                        any = true;
                    }
                }
            }
            if (any) {
                BufferUploader.drawWithShader(buffer.buildOrThrow());
            } else {
                var mesh = buffer.build();
                if (mesh != null) mesh.close();
            }
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
            lightTexture.turnOffLightLayer();
        }

        private static void quad(BufferBuilder b, int x, int z, int y0, int y1, double hx, double hz,
                                 double camX, double camY, double camZ, float u, float v, float a, int skyL, int blockL) {
            float top = y0 * 0.25F + v, bottom = y1 * 0.25F + v; // same mapping as vanilla, so it falls downwards
            b.addVertex((float) (x - camX - hx + 0.5), (float) (y1 - camY), (float) (z - camZ - hz + 0.5)).setUv(u, top).setColor(1.0F, 1.0F, 1.0F, a).setUv2(blockL, skyL);
            b.addVertex((float) (x - camX + hx + 0.5), (float) (y1 - camY), (float) (z - camZ + hz + 0.5)).setUv(u + 1.0F, top).setColor(1.0F, 1.0F, 1.0F, a).setUv2(blockL, skyL);
            b.addVertex((float) (x - camX + hx + 0.5), (float) (y0 - camY), (float) (z - camZ + hz + 0.5)).setUv(u + 1.0F, bottom).setColor(1.0F, 1.0F, 1.0F, a).setUv2(blockL, skyL);
            b.addVertex((float) (x - camX - hx + 0.5), (float) (y0 - camY), (float) (z - camZ - hz + 0.5)).setUv(u, bottom).setColor(1.0F, 1.0F, 1.0F, a).setUv2(blockL, skyL);
        }
    }
}
