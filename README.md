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
- **Winter Cows & Winter Sheep.** New pale, shaggy cows and dark-faced sheep spawn sparsely on snowy land (on snow as well as grass), breed true, can be milked/sheared, and have spawn eggs. Winter cows drop 1-3 leather. A few pigs and chickens spawn too.
- **Ores only where you can see them.** Coal, copper, iron, gold, redstone, lapis, diamond, emerald and Create zinc only generate in blocks that touch air or water: cave walls, ravines, cliffs and mountain faces. Only 3% of ore that would be sealed inside solid rock survives, and the huge noise-generated raw iron/copper veins are off. Strip mining finds almost nothing - explore caves instead.
- Applies to newly created worlds using the default world type (the `minecraft:normal` world preset is overridden).

### Frozen Wastes
- New **Frozen Wastes** biome is the default landscape (~60% of the world): flat, treeless snowfields over patchy **stone outcrops, gravel, coarse dirt and dirt**.
- **Forests are rare** (~4%): only old-growth taiga regions keep their spruce trees, plus vanilla snowy taiga and groves on mountain slopes.
- Frostmaw, igloos, mineshafts, ruined portals and trial chambers all generate in the wastes.

### Blizzards and deep snow
- **Arctic Fox runs the weather**: each day is either **sunny** or **snowy** (about 50/50, rolled per day from the world seed), and **every night is a blizzard**. Natural lightning is suppressed, so blizzards aren't thunderstorms (Trident Channeling still works).
- **Blizzard fog**: outdoors in a blizzard, thick white fog closes in to about 20 blocks. Under a roof it clears.
- **Snow piles up**, up to **3 blocks deep**: layers grow to a full block, then become snow blocks with new layers on top. It builds about a block per stormy night. Snow never settles on glass, ice, campfires or magma, or next to bright light (torches), so greenhouse roofs and lit paths stay clear. Works on its own and alongside Snow! Real Magic.

### Points of interest
Rare structures reward exploring the wastes:
- **Buried Camp**: a half-buried tent with a cold campfire, bedroll and supplies (food, coal, flint, leather, sometimes a spear).
- **Frozen Supply Cache**: chests and barrels sealed inside packed ice, marked by a lantern pole. Seeds, ingots, coal, bone meal.
- **Trapper's Cabin**: a spruce cabin with a lit fireplace, bed, furnace and stocked chests (seeds, food, saplings, a fishing rod).
- With Create: **Abandoned Train Station**, a copper-roofed platform with a ticket booth and overgrown rails, stocked with Create parts.
- With Create: **Abandoned Metro Station** (underground, y -24 to 16), a sealed platform hall with old rails and electron tubes and brass.
- Ruined portals no longer generate.

### Survival
- **Cook your food**: raw meat and fish (vanilla and Farmer's Delight) have an 80% chance to give Hunger for 30 s, like rotten flesh.
- **Fish only live in natural water**: you catch nothing unless the water is connected to water that generated with the world. Bucket ponds, tank water and waterlogged-block pools don't count; holes in a frozen lake do.
- **A start with trees**: new worlds spawn you in snowy plains with scattered spruces, not a forest and not bare wastes. If it's bare, a few spruces are planted.

### Farming only indoors
Crops (and anything else that "grows", like sugar cane and berries) only grow when all three hold:
1. **Sheltered**: any roof above them. Open-sky fields never grow.
2. **Warm**: above 0 °C. With Cold Sweat the real temperature is used: hearths, fires, depth (20+ blocks down is about +15 °C) and insulation all count. Without Cold Sweat, crops count as warm underground (no sky light) or with a heat source within 5 blocks (campfire, lit furnace, magma, lava, Create blaze burner).
3. **Watered**: farmland must be moist. Use Create sprinklers, waterlogged blocks, or water from your tanks.

So farms belong in heated glass greenhouses or underground, watered by Create. Serene Seasons' own greenhouse-glass and underground rules still apply on top.

### Eternal winter (Serene Seasons)
- On every server start the mod sets `doSeasonCycle=false` and `/season set mid_winter`, so the season never changes. Crops need greenhouses (Serene Seasons' glass rule) to grow.
- Configurable sub-season (`early_winter`, `mid_winter`, `late_winter`).

### Sealed dimensions
- Nether portals can't be lit and any travel to the **Nether** or the **End** is cancelled ("The way is frozen shut."). Other modded dimensions (e.g. Create Aeronautics/Sable internals) are untouched.

### No infinite resources
- **No infinite water.** The `waterSourceConversion` gamerule is forced off, so two sources no longer create a third.
- **Placed water evaporates.** Any water source placed after world generation - bucket, dispenser or Create pipe - evaporates after 15 seconds (configurable). Natural water and **waterlogged blocks** are never touched, so keep water in Create fluid tanks.
- **Washing with fans.** A **waterlogged copper grate** in front of an encased fan is a permanent bulk-washing catalyst (Create already treats copper grates as fan-transparent and reads their water). Waterlogged iron/Create bars work too.
- **Create bottomless fluids disabled.** The `create:bottomless/allow` fluid tag is emptied, so a hose pulley in a big lake no longer counts as an infinite source.
- **No cobblestone/stone generators.** Whenever lava and water meet - in the world *or* inside Create pipes - the block produced is **Scoria**: a cobblestone-like volcanic block that drops **nothing**, not even with Silk Touch. This also catches Create's honey/chocolate + lava limestone and scoria generators. Obsidian is still produced as normal.
  - Outputs are controlled by the block tag `#arcticfox:fluid_generator_outputs`.

### No villagers, illagers or witches
- Villagers, zombie villagers, wandering traders, trader llamas, pillagers, vindicators, evokers, illusioners, ravagers, vexes and witches are removed the moment they try to exist (`#arcticfox:banished`).
- Patrols and wandering traders are switched off by gamerule.
- **Villages (all types), pillager outposts, woodland mansions and witch huts never generate.**
- **Strongholds never generate** (eyes of ender have nothing to find).

### Only frozen monsters on the surface
- Monsters spawning naturally where they can see the sky are cancelled unless they are in `#arcticfox:surface_monsters` (default: strays, plus phantoms so membranes stay obtainable).
- Stray spawns are boosted in all snowy biomes. Caves and the deep dark spawn normally.

### Spears (1.21.11 backport)
- **Iron, Diamond and Netherite spears** (netherite via smithing upgrade). Recipe: ingot (top right), stick (center), stick (bottom left).
- **Jab** (attack): a fast stab with +1.5 blocks of reach.
- **Charge** (hold right-click while moving): everything in front of the tip gets struck, with damage scaling with your speed (or your horse's). A full gallop hits about 3x as hard as a jab and can knock riders off their mounts. Each target can be hit once per second; a charge lasts up to 5 seconds.
- Takes Sharpness, Smite, Bane, Fire Aspect, Knockback, Looting, Unbreaking and Mending.

### Tool progression
- **Wooden tools removed**: no recipes, hidden from the creative menu and JEI; the bonus chest gives flint tools instead.
- **Stone tools are now Flint tools**: crafted from flint and sticks, retextured and renamed. They mine copper, **not iron**.
- **Copper tools backported** from 1.21.9: sword, shovel, pickaxe, axe and hoe (190 durability, speed 5, enchantability 13). Copper is the first tier that can mine iron.
- New shapeless recipe: **3 gravel → 1 flint**, so you can't get stuck at the start.
- **Smoker: rotten flesh → leather.**
- Progression: punch logs → dig gravel → flint tools → copper → copper tools → iron → diamond.

### Elytra & shulker boxes, Create-made
With the End sealed, both are built on a **Mechanical Crafter** grid (Create required):
- **Elytra** (7x4): brass sheets, a precision mechanism, andesite alloy, leather, white sails and phantom membranes.
  ```
  B B . P . B B      B = brass sheet       P = precision mechanism
  M S A L A S M      M = phantom membrane  S = white sail
  M S . . . S M      A = andesite alloy    L = leather
  M . . . . . M
  ```
- **Shulker shells x2** (5x4): brass sheets, iron sheets, an ender pearl (from cave endermen) and a brass casing. Shulker boxes then use the normal vanilla recipe.
  ```
  . B B B .      B = brass sheet   I = iron sheet
  B I E I B      E = ender pearl   C = brass casing
  B . C . B
  I . . . I
  ```
- **Create-style textures:** shulker boxes look like brass-framed casings (dyed boxes tint the wooden panels), the shell is a brass clamshell, and the elytra is sail canvas on wooden spars with brass edging.
- "Sky's the Limit" moves to the Adventure tab.

### Advancements that stay achievable
- The entire **Nether** and **End** tabs are removed, along with every advancement that needs villagers, illagers, raids, bees, warm biomes, sniffers, frogs, allays, armadillos or the other dimensions (65 in total).
- **Adventuring Time** is rewritten to require the 14 biomes that actually generate.
- **Hidden in the Depths** (crying obsidian) moves to the Story tab - ruined portals still generate.
- If a Nether-resource mod such as **Create: Netherless** is installed, the blaze rod, Local Brewery, ancient debris, netherite armor, lodestone and netherite hoe advancements are kept and re-parented into the Overworld tabs; without it they are removed.

## Compatibility notes
| Mod | Notes |
| --- | --- |
| Create | Pipe lava/water collisions produce scoria; bottomless fluid sources off. No hard dependency. |
| Serene Seasons / GlitchCore | **Optional.** If installed it's locked to winter, and carrots, potatoes, beetroots (plus Farmer's Delight cabbages and onions) count as winter crops so early greenhouses work. Arctic Fox doesn't need it: it runs the weather and crop rules itself. |
| Cold Sweat + Create (replaces Create: Cold Sweat) | **A working factory keeps you warm.** Blaze burners (+3 to +20 °C by heat level), lit blaze burners, steam engines on a running boiler, and pipes, pumps, valves, basins, spouts and item drains holding lava or other hot fluids all radiate heat. Fluid tanks with lava and boilers use Cold Sweat's own heating. Measured 2 blocks away: seething burner +13 °C, lava basin +13 °C, lava tank +7 °C. |
| Cold Sweat | Surface biomes sit around **-12 °C by day and -18 °C at night** (colder in storms), measured in game. Crop growth uses Cold Sweat's real temperature. **No grace period when you respawn at your own bed or respawn anchor** (Cold Sweat would otherwise grant it after every death); first join still gets it. |
| Farmer's Delight | Its wild crops only spawn in warm biomes, so its seeds (cabbage, tomato, onion, rice) are added to cabin and supply cache loot. Greenhouse crops then feed Slice & Dice's cutting automation. Raw FD meats count as raw food. |
| Snow! Real Magic | Its own snow covering still works; Arctic Fox stacks snow above one block, up to 3 blocks deep. |
| Mowzie's Mobs | **Frostmaw is the only boss, and a very rare find** (one per ~96 chunks instead of 25). Ferrous Wroughtnaut, Umvuthi and the Sculptor are removed, along with their structures (wrought chamber, Umvuthana grove, monastery) and advancements. Frostmaw spawns in Frozen Wastes and snowfields, and drops and steals its ice crystal as normal. Advancements for jungle, savanna and forest Mowzie mobs that can't spawn here are removed. |
| Create: Netherless | Detected automatically; keeps the Nether-material advancements reachable. |
| JEI | Wooden tools hidden. |
| Create Slice & Dice | Sprinkler rain works under Serene Seasons: built-in replacement for Serene & Slice Compat, supporting both Slice & Dice 4.3+ (Atmosphere) and 4.2.x and older (WetAir). |
| Create Aeronautics / Sable, Copycats+, Slice & Dice, Serene & Slice Compat | No conflicts; only the Nether and End are blocked. |

## Configuration
`config/arcticfox-common.toml` - every feature above can be toggled. Tags can be edited with a datapack:
`#arcticfox:fluid_generator_outputs`, `#arcticfox:banished`, `#arcticfox:surface_monsters`, `#arcticfox:needs_copper_tool`.

## Building
```
./gradlew build
```
The jar ends up in `build/libs/`. GitHub Actions builds every push, then boots a dedicated server twice - once alone and once with Create + Serene Seasons - generates a world and checks biomes, terrain, advancements, recipes, mining tiers, scoria generation and the winter lock. The jar is uploaded as an artifact, and tags starting with `v` publish a release. Build/test logs are mirrored to the `ci-logs` branch.
