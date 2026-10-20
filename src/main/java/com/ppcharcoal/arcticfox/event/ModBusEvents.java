package com.ppcharcoal.arcticfox.event;

import com.ppcharcoal.arcticfox.ArcticFox;
import com.ppcharcoal.arcticfox.registry.ModItems;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import java.util.List;

@EventBusSubscriber(modid = ArcticFox.MOD_ID)
public final class ModBusEvents {
    public static final List<Item> WOODEN_TOOLS = List.of(
            Items.WOODEN_SWORD, Items.WOODEN_SHOVEL, Items.WOODEN_PICKAXE, Items.WOODEN_AXE, Items.WOODEN_HOE);

    private ModBusEvents() {
    }

    @SubscribeEvent
    public static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        CreativeModeTab.TabVisibility both = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;

        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES || event.getTabKey() == CreativeModeTabs.COMBAT) {
            for (Item item : WOODEN_TOOLS) {
                try {
                    event.remove(new ItemStack(item), both);
                } catch (RuntimeException ignored) {
                    // not present in this tab
                }
            }
        }

        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.insertAfter(new ItemStack(Items.STONE_HOE), new ItemStack(ModItems.COPPER_SHOVEL.get()), both);
            event.insertAfter(new ItemStack(ModItems.COPPER_SHOVEL.get()), new ItemStack(ModItems.COPPER_PICKAXE.get()), both);
            event.insertAfter(new ItemStack(ModItems.COPPER_PICKAXE.get()), new ItemStack(ModItems.COPPER_AXE.get()), both);
            event.insertAfter(new ItemStack(ModItems.COPPER_AXE.get()), new ItemStack(ModItems.COPPER_HOE.get()), both);
        } else if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            CreativeModeTab.TabVisibility parentOnly = CreativeModeTab.TabVisibility.PARENT_TAB_ONLY;
            event.insertAfter(new ItemStack(Items.STONE_SWORD), new ItemStack(ModItems.COPPER_SWORD.get()), both);
            event.insertAfter(new ItemStack(Items.STONE_AXE), new ItemStack(ModItems.COPPER_AXE.get()), parentOnly);
            event.insertAfter(new ItemStack(Items.TRIDENT), new ItemStack(ModItems.IRON_SPEAR.get()), both);
            event.insertAfter(new ItemStack(ModItems.IRON_SPEAR.get()), new ItemStack(ModItems.DIAMOND_SPEAR.get()), both);
            event.insertAfter(new ItemStack(ModItems.DIAMOND_SPEAR.get()), new ItemStack(ModItems.NETHERITE_SPEAR.get()), both);
        } else if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ModItems.SCORIA.get());
        } else if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.CABIN_STOVE.get());
            event.accept(ModItems.FLICKERING_LAMP.get());
        } else if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.WINTER_COW_SPAWN_EGG.get());
            event.accept(ModItems.WINTER_SHEEP_SPAWN_EGG.get());
            event.accept(ModItems.WENDIGO_SPAWN_EGG.get());
        }
    }
}
