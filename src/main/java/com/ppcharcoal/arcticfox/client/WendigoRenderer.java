package com.ppcharcoal.arcticfox.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.ppcharcoal.arcticfox.ArcticFox;
import com.ppcharcoal.arcticfox.entity.Wendigo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Renders the Wendigo, plus two glowing blue eyes that shine through the blizzard fog. */
public class WendigoRenderer extends MobRenderer<Wendigo, WendigoModel> {
    private static final ResourceLocation TEXTURE = ArcticFox.id("textures/entity/wendigo.png");
    private static final ResourceLocation GLOW = ArcticFox.id("textures/entity/wendigo_eye_glow.png");
    private static final float SCALE = 0.68F;

    public WendigoRenderer(EntityRendererProvider.Context context) {
        super(context, new WendigoModel(context.bakeLayer(WendigoModel.LAYER)), 0.55F);
        addLayer(new EyeGlow(this));
    }

    @Override
    protected void scale(Wendigo entity, PoseStack pose, float partialTick) {
        pose.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(Wendigo entity) {
        return TEXTURE;
    }

    /**
     * Additive, unfogged glow sprites: the position_tex_color shader ignores fog, so the eyes stay visible
     * deep inside the blizzard while the body is lost in it. The further away, the larger the halo.
     */
    static final RenderType EYES = new RenderType("arcticfox_wendigo_eyes", DefaultVertexFormat.POSITION_TEX_COLOR,
            VertexFormat.Mode.QUADS, 256, false, true,
            () -> {
                RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
                RenderSystem.setShaderTexture(0, GLOW);
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                RenderSystem.depthMask(false);
                RenderSystem.enableDepthTest();
                RenderSystem.disableCull();
            },
            () -> {
                RenderSystem.depthMask(true);
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableBlend();
                RenderSystem.enableCull();
            }) {
    };

    static final class EyeGlow extends RenderLayer<Wendigo, WendigoModel> {
        EyeGlow(RenderLayerParent<Wendigo, WendigoModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, Wendigo entity, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (entity.isInvisible()) return;
            Minecraft mc = Minecraft.getInstance();
            double dist = mc.gameRenderer.getMainCamera().getPosition().distanceTo(entity.getEyePosition(partialTick));
            // flicker a little, like light catching wet eyes
            float flicker = 0.85F + 0.15F * Mth.sin(ageInTicks * 0.31F) * Mth.sin(ageInTicks * 0.17F + 1.3F);
            float alpha = entity.getState() == Wendigo.WATCHED || entity.getState() == Wendigo.HUNT || entity.getState() == Wendigo.SCREAM ? 1.0F : 0.8F;
            float size = 1.6F + (float) Math.min(7.0, dist / 5.0); // half size in model pixels, grows with distance
            pose.pushPose();
            getParentModel().translateToHead(pose);
            VertexConsumer vc = buffers.getBuffer(EYES);
            for (float ex : new float[]{-1.5F, 1.5F}) {
                quad(vc, pose, ex / 16F, -4.0F / 16F, -4.15F / 16F, size / 16F, alpha * flicker);
                quad(vc, pose, ex / 16F, -4.0F / 16F, -4.2F / 16F, 0.9F / 16F, 1.0F); // hot core
            }
            pose.popPose();
        }

        private static void quad(VertexConsumer vc, PoseStack pose, float x, float y, float z, float half, float a) {
            var m = pose.last().pose();
            int alpha = (int) (Mth.clamp(a, 0, 1) * 255);
            vc.addVertex(m, x - half, y - half, z).setUv(0, 0).setColor(255, 255, 255, alpha);
            vc.addVertex(m, x - half, y + half, z).setUv(0, 1).setColor(255, 255, 255, alpha);
            vc.addVertex(m, x + half, y + half, z).setUv(1, 1).setColor(255, 255, 255, alpha);
            vc.addVertex(m, x + half, y - half, z).setUv(1, 0).setColor(255, 255, 255, alpha);
        }
    }
}
