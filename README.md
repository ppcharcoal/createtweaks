<p align="center"><img src="icon.png" width="128" alt="Arctic Fox icon"></p>

# Arctic Fox : Create Tweaks

A NeoForge **1.21.1** overhaul for an eternal-winter Create playthrough. Everything that used to be infinite - cobblestone, water, villager trades, raid loot - is gone, so Create becomes a tool for **refining scarce materials** instead of a machine for building infinite farms.

Mod id: `arcticfox` · Requires NeoForge 21.1+ · Server **and** client.

## Feature list

### A frozen world
- **Only snowy biomes.** A custom biome source (`arcticfox:arctic`) wraps the vanilla overworld layout and swaps every temperate/warm biome for a frozen one: snowy plains, snowy taiga, grove, snowy slopes, ice spikes, frozen/jagged peaks, snowy beaches, frozen rivers and frozen oceans. Cave biomes (lush caves, dripstone caves, deep dark) still exist underground.
- **Mostly land, not ocean.** Continentalness is pushed inland, so oceans are noticeably rarer and the frozen biomes are real land, not just ice sheets on water.
- **Flat wasteland terrain.** Lowland relief between sea level and the foothills is squashed to ~30% of vanilla, giving wide, flat, windswept plains. Mountain ranges and peaks keep their shape (just slightly lower).
- **No floating islands.** The terrain "factor" has a floor and the 3D terrain noise is dampened, which stops land from detaching into floating chunks and overhangs.
- **Livestock still exists.** Cows, sheep, pigs and chickens are added to snowy land biomes so leather, wool, beds and food stay possible.
- Applies to newly created worlds using the default world type (the `minecraft:normal` world preset is overridden).

### Eternal winter (Serene Seasons)
- On every server start the mod sets `doSeasonCycle=false` and `/season set mid_winter`, so the season never changes. Crops need greenhouses (Serene Seasons' glass rule) to grow.
- Configurable sub-season (`early_winter`, `mid_winter`, `late_winter`).

### Sealed dimensions
- Nether portals can't be lit and any travel to the **Nether** or the **End** is cancelled ("The way is frozen shut."). Other modded dimensions (e.g. Create Aeronautics/Sable internals) are untouched.

### No infinite resources
- **No infinite water.** The `waterSourceConversion` gamerule is forced off, so two sources no longer create a third.
- **Create bottomless fluids disabled.** The `create:bottomless/allow` fluid tag is emptied, so a hose pulley in a big lake no longer counts as an infinite source.
- **No cobblestone/stone generators.** Whenever lava and water meet - in the world *or* inside Create pipes - the block produced is **Scoria**: a cobblestone-like volcanic block that drops **nothing**, not even with Silk Touch. This also catches Create's honey/chocolate + lava limestone and scoria generators. Obsidian is still produced as normal.
  - Outputs are controlled by the block tag `#arcticfox:fluid_generator_outputs`.

### No villagers, illagers or witches
- Villagers, zombie villagers, wandering traders, trader llamas, pillagers, vindicators, evokers, illusioners, ravagers, vexes and witches are removed the moment they try to exist (`#arcticfox:banished`).
- Patrols and wandering traders are switched off by gamerule.
- **Villages (all types), pillager outposts, woodland mansions and witch huts never generate.**

### Only frozen monsters on the surface
- Monsters spawning naturally where they can see the sky are cancelled unless they are in `#arcticfox:surface_monsters` (default: strays, plus phantoms so membranes stay obtainable).
- Stray spawns are boosted in all snowy biomes. Caves and the deep dark spawn normally.

### Tool progression
- **Wooden tools removed**: no recipes, hidden from the creative menu and JEI; the bonus chest gives flint tools instead.
- **Stone tools are now Flint tools**: crafted from flint and sticks, retextured and renamed. They mine copper, **not iron**.
- **Copper tools backported** from 1.21.9: sword, shovel, pickaxe, axe and hoe (190 durability, speed 5, enchantability 13). Copper is the first tier that can mine iron.
- New shapeless recipe: **3 gravel → 1 flint**, so you can't get stuck at the start.
- Progression: punch logs → dig gravel → flint tools → copper → copper tools → iron → diamond.

### Advancements that stay achievable
- The entire **Nether** and **End** tabs are removed, along with every advancement that needs villagers, illagers, raids, bees, warm biomes, sniffers, frogs, allays, armadillos or the other dimensions (65 in total).
- **Adventuring Time** is rewritten to require the 14 biomes that actually generate.
- **Hidden in the Depths** (crying obsidian) moves to the Story tab - ruined portals still generate.
- If a Nether-resource mod such as **Create: Netherless** is installed, the blaze rod, Local Brewery, ancient debris, netherite armor, lodestone and netherite hoe advancements are kept and re-parented into the Overworld tabs; without it they are removed.

## Compatibility notes
| Mod | Notes |
| --- | --- |
| Create | Pipe lava/water collisions produce scoria; bottomless fluid sources off. No hard dependency. |
| Serene Seasons / GlitchCore | Locked to winter on every server start. |
| Cold Sweat / Create: Cold Sweat | Works as-is - permanent winter makes heating and insulation essential. |
| Create: Netherless | Detected automatically; keeps the Nether-material advancements reachable. |
| JEI | Wooden tools hidden. |
| Create Aeronautics / Sable, Copycats+, Slice & Dice, Serene & Slice Compat | No conflicts; only the Nether and End are blocked. |

## Configuration
`config/arcticfox-common.toml` - every feature above can be toggled. Tags can be edited with a datapack:
`#arcticfox:fluid_generator_outputs`, `#arcticfox:banished`, `#arcticfox:surface_monsters`, `#arcticfox:needs_copper_tool`.

## Building
```
./gradlew build
```
The jar ends up in `build/libs/`. GitHub Actions builds every push, then boots a dedicated server twice - once alone and once with Create + Serene Seasons - generates a world and checks biomes, terrain, advancements, recipes, mining tiers, scoria generation and the winter lock. The jar is uploaded as an artifact, and tags starting with `v` publish a release. Build/test logs are mirrored to the `ci-logs` branch.
