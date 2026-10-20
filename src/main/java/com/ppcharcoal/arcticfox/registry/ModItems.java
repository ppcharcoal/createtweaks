package com.ppcharcoal.arcticfox.registry;

import com.ppcharcoal.arcticfox.ArcticFox;
import com.ppcharcoal.arcticfox.ModTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import com.ppcharcoal.arcticfox.item.SpearItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ArcticFox.MOD_ID);

    /**
     * Copper tier, matching the 1.21.9 copper tools (190 uses, speed 5, enchantability 13),
     * sitting between flint (vanilla stone tier) and iron. Copper is the first tier that can mine iron.
     */
    public static final Tier COPPER = new Tier() {
        @Override
        public int getUses() {
            return 190;
        }

        @Override
        public float getSpeed() {
            return 5.0F;
        }

        @Override
        public float getAttackDamageBonus() {
            return 1.0F;
        }

        @Override
        public TagKey<Block> getIncorrectBlocksForDrops() {
            return ModTags.INCORRECT_FOR_COPPER_TOOL;
        }

        @Override
        public int getEnchantmentValue() {
            return 13;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(Items.COPPER_INGOT);
        }
    };

    public static final DeferredItem<SwordItem> COPPER_SWORD = ITEMS.register("copper_sword",
            () -> new SwordItem(COPPER, new Item.Properties().attributes(SwordItem.createAttributes(COPPER, 3, -2.4F))));
    public static final DeferredItem<ShovelItem> COPPER_SHOVEL = ITEMS.register("copper_shovel",
            () -> new ShovelItem(COPPER, new Item.Properties().attributes(DiggerItem.createAttributes(COPPER, 1.5F, -3.0F))));
    public static final DeferredItem<PickaxeItem> COPPER_PICKAXE = ITEMS.register("copper_pickaxe",
            () -> new PickaxeItem(COPPER, new Item.Properties().attributes(DiggerItem.createAttributes(COPPER, 1.0F, -2.8F))));
    public static final DeferredItem<AxeItem> COPPER_AXE = ITEMS.register("copper_axe",
            () -> new AxeItem(COPPER, new Item.Properties().attributes(DiggerItem.createAttributes(COPPER, 7.0F, -3.2F))));
    public static final DeferredItem<HoeItem> COPPER_HOE = ITEMS.register("copper_hoe",
            () -> new HoeItem(COPPER, new Item.Properties().attributes(DiggerItem.createAttributes(COPPER, -1.0F, -2.0F))));

    public static final DeferredItem<BlockItem> SCORIA = ITEMS.registerSimpleBlockItem(ModBlocks.SCORIA);
    public static final DeferredItem<BlockItem> FLICKERING_LAMP = ITEMS.registerSimpleBlockItem(ModBlocks.FLICKERING_LAMP);
    public static final DeferredItem<BlockItem> CABIN_STOVE = ITEMS.registerSimpleBlockItem(ModBlocks.CABIN_STOVE);

    public static final DeferredItem<DeferredSpawnEggItem> WINTER_COW_SPAWN_EGG = ITEMS.register("winter_cow_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.WINTER_COW, 0xD9DCE0, 0x6B7380, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> WINTER_SHEEP_SPAWN_EGG = ITEMS.register("winter_sheep_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.WINTER_SHEEP, 0xF2F4F7, 0x2E2A30, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> WENDIGO_SPAWN_EGG = ITEMS.register("wendigo_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.WENDIGO, 0x4A3A2E, 0x7FD8FF, new Item.Properties()));

    public static final DeferredItem<SpearItem> IRON_SPEAR = ITEMS.register("iron_spear",
            () -> new SpearItem(Tiers.IRON, 5.0F, new Item.Properties()));
    public static final DeferredItem<SpearItem> DIAMOND_SPEAR = ITEMS.register("diamond_spear",
            () -> new SpearItem(Tiers.DIAMOND, 6.0F, new Item.Properties()));
    public static final DeferredItem<SpearItem> NETHERITE_SPEAR = ITEMS.register("netherite_spear",
            () -> new SpearItem(Tiers.NETHERITE, 7.0F, new Item.Properties().fireResistant()));

    /** Transitional item of the emerald + experience Mending book sequenced assembly (Create). */
    public static final DeferredItem<Item> INCOMPLETE_MENDING_BOOK = ITEMS.registerSimpleItem("incomplete_mending_book",
            new Item.Properties().stacksTo(1));

    private ModItems() {
    }
}
