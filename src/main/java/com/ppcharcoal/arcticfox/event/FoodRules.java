package com.ppcharcoal.arcticfox.event;

import com.ppcharcoal.arcticfox.ArcticFox;
import com.ppcharcoal.arcticfox.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Raw meat and fish make you sick, like rotten flesh: 80% chance of Hunger for 30 seconds. Cook your food! */
@EventBusSubscriber(modid = ArcticFox.MOD_ID)
public final class FoodRules {
    public static final TagKey<Item> RAW_FOODS = TagKey.create(Registries.ITEM, ArcticFox.id("raw_foods"));

    private FoodRules() {
    }

    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (!Config.RAW_FOOD_SICKNESS.get() || event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Player player) || !event.getItem().is(RAW_FOODS)) return;
        if (player.getRandom().nextFloat() < 0.8F) {
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 600, 0));
        }
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().is(RAW_FOODS) && Config.SPEC.isLoaded() && Config.RAW_FOOD_SICKNESS.get()) {
            event.getToolTip().add(Component.translatable("tooltip.arcticfox.raw_food").withStyle(ChatFormatting.DARK_GREEN));
        }
    }
}
