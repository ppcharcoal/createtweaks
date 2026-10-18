package com.ppcharcoal.arcticfox.compat;

import com.ppcharcoal.arcticfox.ArcticFox;
import com.ppcharcoal.arcticfox.event.ModBusEvents;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Hides the removed wooden tools from JEI. Only loaded by JEI, so JEI stays optional. */
@JeiPlugin
public final class ArcticJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ArcticFox.id("jei_plugin");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        List<ItemStack> hidden = ModBusEvents.WOODEN_TOOLS.stream().map(ItemStack::new).toList();
        runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hidden);
    }
}
