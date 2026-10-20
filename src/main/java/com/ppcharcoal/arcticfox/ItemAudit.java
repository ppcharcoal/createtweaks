package com.ppcharcoal.arcticfox;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.loot.LootTable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Dev/CI only: writes item_audit.txt listing every item that no recipe produces, together with the
 * loot tables that mention it, and every entity with the biomes it naturally spawns in. Used to
 * curate #c:hidden_from_recipe_viewers.
 */
final class ItemAudit {
    private ItemAudit() {
    }

    static void run(MinecraftServer server, ServerLevel level) {
        try {
            var access = server.registryAccess();
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, access);

            Set<Item> produced = new HashSet<>();
            Map<Item, Integer> mentioned = new HashMap<>();
            for (var holder : server.getRecipeManager().getRecipes()) {
                Recipe<?> recipe = holder.value();
                try {
                    produced.add(recipe.getResultItem(access).getItem());
                } catch (Throwable ignored) {
                }
                try {
                    String json = Recipe.CODEC.encodeStart(ops, recipe).getOrThrow().toString();
                    for (String id : itemIds(json)) {
                        BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(id)).ifPresent(i -> mentioned.merge(i, 1, Integer::sum));
                    }
                } catch (Throwable ignored) {
                }
            }

            Map<Item, List<String>> loot = new HashMap<>();
            var reg = server.reloadableRegistries();
            for (ResourceLocation id : reg.getKeys(Registries.LOOT_TABLE)) {
                try {
                    LootTable table = reg.getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id));
                    String json = LootTable.DIRECT_CODEC.encodeStart(ops, table).getOrThrow().toString();
                    for (String s : itemIds(json)) {
                        BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(s)).ifPresent(i -> loot.computeIfAbsent(i, k -> new ArrayList<>()).add(id.toString()));
                    }
                } catch (Throwable ignored) {
                }
            }

            Map<EntityType<?>, Set<String>> spawns = new HashMap<>();
            var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
            for (var entry : biomes.entrySet()) {
                Biome biome = entry.getValue();
                for (MobCategory cat : MobCategory.values()) {
                    for (var data : biome.getMobSettings().getMobs(cat).unwrap()) {
                        spawns.computeIfAbsent(data.type, k -> new TreeSet<>()).add(entry.getKey().location().getPath());
                    }
                }
            }

            StringBuilder out = new StringBuilder();
            Map<String, List<String>> byMod = new TreeMap<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (produced.contains(item)) continue;
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                StringBuilder line = new StringBuilder("ITEM ").append(id);
                line.append(" | inRecipes=").append(mentioned.getOrDefault(item, 0));
                List<String> tables = loot.getOrDefault(item, List.of());
                line.append(" | loot=").append(tables.size() > 6 ? tables.subList(0, 6) + "+" + (tables.size() - 6) : tables);
                if (item instanceof SpawnEggItem egg) {
                    EntityType<?> type = egg.getType(new ItemStack(item));
                    line.append(" | egg=").append(EntityType.getKey(type)).append(" spawnsIn=").append(spawns.getOrDefault(type, Set.of()).size());
                }
                byMod.computeIfAbsent(id.getNamespace(), k -> new ArrayList<>()).add(line.toString());
            }
            for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
                ResourceLocation id = EntityType.getKey(type);
                Set<String> where = spawns.getOrDefault(type, Set.of());
                String tags = type.builtInRegistryHolder().tags().map(t -> t.location().toString())
                        .filter(t -> t.startsWith("arcticfox:")).toList().toString();
                byMod.computeIfAbsent(id.getNamespace(), k -> new ArrayList<>()).add("ENTITY " + id + " | " + type.getCategory()
                        + " | spawnsIn=" + where.size() + " " + (where.size() > 5 ? new ArrayList<>(where).subList(0, 5) + "..." : where) + " | tags=" + tags);
            }
            byMod.forEach((mod, lines) -> {
                out.append("##### ").append(mod).append('\n');
                lines.forEach(l -> out.append(l).append('\n'));
            });
            Path file = server.getServerDirectory().resolve("item_audit.txt");
            Files.writeString(file, out.toString());
            ArcticFox.LOGGER.info("[ArcticFox/SMOKE] item audit written to {} ({} recipes, {} items not produced)", file,
                    server.getRecipeManager().getRecipes().size(), byMod.values().stream().mapToInt(List::size).sum());
        } catch (Throwable t) {
            ArcticFox.LOGGER.error("[ArcticFox/SMOKE] item audit failed", t);
        }
    }

    private static Set<String> itemIds(String json) {
        Set<String> ids = new HashSet<>();
        var m = java.util.regex.Pattern.compile("\"([a-z0-9_.-]+:[a-z0-9_/.-]+)\"").matcher(json);
        while (m.find()) ids.add(m.group(1));
        return ids;
    }
}
