package com.ppcharcoal.arcticfox.compat;

import com.ppcharcoal.arcticfox.ArcticFox;
import com.ppcharcoal.arcticfox.event.ModBusEvents;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.List;

/**
 * Hides the removed wooden tools and everything in #c:hidden_from_recipe_viewers (items of removed
 * mobs, bosses and structures that can no longer be obtained). Only loaded by JEI, so JEI stays optional.
 */
@JeiPlugin
public final class ArcticJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ArcticFox.id("jei_plugin");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        List<ItemStack> hidden = new ArrayList<>(ModBusEvents.WOODEN_TOOLS.stream().map(ItemStack::new).toList());
        for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(Tags.Items.HIDDEN_FROM_RECIPE_VIEWERS)) {
            hidden.add(new ItemStack(holder));
        }
        try {
            runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hidden);
        } catch (RuntimeException e) {
            ArcticFox.LOGGER.warn("Could not hide {} items from JEI", hidden.size(), e);
        }
        ArcticFox.LOGGER.info("Hid {} unobtainable items from JEI", hidden.size());
    }
}
