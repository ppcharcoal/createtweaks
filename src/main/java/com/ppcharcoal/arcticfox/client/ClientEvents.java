package com.ppcharcoal.arcticfox.client;

import com.ppcharcoal.arcticfox.ArcticFox;
import com.ppcharcoal.arcticfox.registry.ModEntities;
import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.entity.SheepRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Sheep;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = ArcticFox.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    private static final ResourceLocation WINTER_COW = ArcticFox.id("textures/entity/winter_cow.png");
    private static final ResourceLocation WINTER_SHEEP = ArcticFox.id("textures/entity/winter_sheep.png");

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.WINTER_COW.get(), context -> new CowRenderer(context) {
            @Override
            public ResourceLocation getTextureLocation(Cow entity) {
                return WINTER_COW;
            }
        });
        event.registerEntityRenderer(ModEntities.WINTER_SHEEP.get(), context -> new SheepRenderer(context) {
            @Override
            public ResourceLocation getTextureLocation(Sheep entity) {
                return WINTER_SHEEP;
            }
        });
    }
}
