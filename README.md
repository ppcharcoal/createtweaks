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
- **Night blizzards are dark**: the storm fog turns a deep blue-grey at night instead of glowing white (snow-white by day), and a torch or lantern faintly lights the snow around you.
- **Wind-driven snow**: during a blizzard the snow falls much faster than normal and is blown in one direction by a gusting wind from the north-west, in two layers. Calm snowy days keep the gentle vanilla snowfall.
- **Blizzards bite** (Cold Sweat): out in the open during a blizzard it is another **10 °C colder** (about -25 °C instead of -15 °C). Any roof over your head removes the wind chill. Config: `weather.blizzardChill`.
- **Arctic Fox runs the weather**: each day is either **sunny** or **snowy** (about 50/50, rolled per day from the world seed), and **every night is a blizzard**. Natural lightning is suppressed, so blizzards aren't thunderstorms (Trident Channeling still works).
- **Blizzard fog**: outdoors in a blizzard, thick white fog closes in to about 20 blocks. Under a roof it clears.
- **Snow piles up**, up to **2 blocks deep**: layers grow to a full block, then become snow blocks with new layers on top. It builds about a block per stormy night. Snow never settles on glass, ice, campfires or magma, or next to bright light (torches), so greenhouse roofs and lit paths stay clear. Works on its own and alongside Snow! Real Magic.

- **Heat melts snow**: working heat sources clear the snow around them and keep new snow from settling there, so an outdoor factory doesn't get buried. Blaze burners (2-6 blocks, wider the hotter they burn), steam engines on a running boiler (5), pipes, pumps, tanks and basins carrying lava or other hot fluids (2-3), Create: New Age heaters and heat pipes (3), the cast-iron stove (4), lit furnaces and campfires (2).

### The Wendigo
Something follows you through the blizzard. From the third night on, a **Wendigo** may start stalking a player who is out in the open at night. There is **at most one a night** for the whole world, however many players are online.
- It appears far behind you in the fog. You won't see its gaunt body, only **two glowing blue eyes** that shine straight through the blizzard.
- **Look at it and it freezes**, staring back, head slowly tilting.
- **Walk towards it while looking** and it bolts into the fog and is gone, only to return **30 seconds later from a different direction**.
- **Look away and it creeps closer.** Breathing, clicking bones and soft footsteps carry faintly through the storm.
- If it reaches you unseen, or you turn around and it is already within a few blocks, it **screams** (you are briefly slowed and your vision darkens) and **attacks until you are dead**.
- **Only on the surface.** It never comes for someone underground (caves, mines, tunnels with more than 6 blocks overhead; a house still counts as the surface). If its prey goes underground, even mid-attack, it breaks off and waits up top.
- **Light keeps it away.** It never picks a player standing in light (about 4 blocks from a torch, 5 from a lantern or campfire). If its prey steps into the light it switches to someone still in the dark, or slinks off and waits until they leave it. Once it has screamed, light won't save you.
- **Multiplayer:** any player's gaze freezes it, not just its prey's, so friends can watch each other's backs. If its prey logs off, it goes after the nearest player in the dark. Once it has killed its prey it is gone for the night.
- 100 health (5x a zombie), 9 damage, hard to knock back. Wounding it while it stalks drives it off for a while. It is gone at dawn unless it is already hunting.
- Drops bones, leather and sometimes a skull. Killing one earns the **Hunger Satisfied** challenge.
- Config: `wendigo.enabled`, `wendigo.spawnChance`, `wendigo.firstNight`, `wendigo.safeLight`.

### Points of interest
Rare structures reward exploring the wastes:
- All surface structures only generate on **dry, fairly flat land**, never on lakes, frozen or not.
- **Buried Camp**: a half-buried tent with a cold campfire, bedroll and supplies (food, coal, flint, leather).
- **Frozen Supply Cache**: chests and barrels sealed inside packed ice, marked by a lantern pole. Seeds, ingots, coal, bone meal.
- **Trapper's Cabin**: a low, sealed log cabin with a shallow slab roof, a stone footing, glass windows, a fireplace and a **Cast-Iron Stove** stocked with 2 days of fuel. Inside it's about +11 °C on a -18 °C night.
- With Create: **Abandoned Train Station**, a copper-roofed platform with **Create tracks**, a ticket booth and flickering roof lights. Loot is food, coal, torches, rails and a few Create parts.
- With Create: **Abandoned Metro Station** (underground, y -24 to 16), a sealed platform hall with Create tracks and **flickering ceiling lamps**.
- With **Steam 'n' Rails**: station tracks become spruce S&R tracks and platform edges get hazard stripes.
- Ruined portals no longer generate.

### New blocks
- **Cast-Iron Stove** (5 iron, a furnace, 3 bricks): feed it any furnace fuel and it holds up to 3 in-game days. While burning it's a strong heat source (+32 °C with Cold Sweat, fading over 8 blocks) and greenhouse heat.
- **Flickering Lamp** (iron nuggets, glass panes, a torch): an old industrial lamp that mostly shines but stutters now and then.

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

### Tool progression
- **Wooden tools removed**: no recipes, hidden from the creative menu and JEI; the bonus chest gives flint tools instead.
- **Stone tools are now Flint tools**: crafted from flint and sticks, retextured and renamed. They mine copper, **not iron**.
- **Copper tools backported** from 1.21.9: sword, shovel, pickaxe, axe and hoe (190 durability, speed 5, enchantability 13). Copper is the first tier that can mine iron.
- New shapeless recipe: **3 gravel → 1 flint**, so you can't get stuck at the start.
- **Smoker: rotten flesh → leather.**
- Progression: punch logs → dig gravel → flint tools → copper → copper tools → iron → diamond.

### Emeralds have a job again
Without villagers, emeralds buy what villagers and illagers used to give:
- **Mending books** (Create sequenced assembly): book, then 3 loops of *deploy emerald → fill 250 mB liquid XP → press*. Without Create: Enchantment Industry, the XP step deploys an experience nugget instead.
- **Totem of Undying** on the Mechanical Crafter: 4 emeralds, gold, a golden apple and a precision mechanism. "Postmortal" is achievable again.
- **Bell**: sticks, gold and an emerald.
- **Liquid XP**: with Create: Enchantment Industry, the Mechanical Grindstone turns an emerald into 30 mB of experience (270 mB for a block).

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

### Recipe viewers only show what you can get
JEI (and EMI/REI, through the shared `c:hidden_from_recipe_viewers` tag) hides about 220 items that can no longer be obtained: drops and crafts of the removed Alex's Mobs species (crocodile chestplate, roadrunner boots, kangaroo items, tarantula hawk elytra, void worm gear...), the removed Mowzie's Mobs bosses and their structures (wrought axe and helm, masks, sol visage, geomancer set, earthrend gauntlet...), Cold Sweat's hoglin and chameleon armor and the Thermolith, the Steam 'n' Rails conductor whistle, the totem of undying, bells, trims from removed structures and spawn eggs of mobs that never spawn. Wooden tools stay hidden too. The list was worked out from every recipe and loot table in the full modpack.

## Compatibility notes
| Mod | Notes |
| --- | --- |
| Create | Pipe lava/water collisions produce scoria; bottomless fluid sources off. No hard dependency. |
| Serene Seasons / GlitchCore | **Optional.** If installed it's locked to winter, and carrots, potatoes, beetroots (plus Farmer's Delight cabbages and onions) count as winter crops so early greenhouses work. Arctic Fox doesn't need it: it runs the weather and crop rules itself. |
| Cold Sweat + Create (replaces Create: Cold Sweat) | **A working factory keeps you warm.** Blaze burners (+3 to +20 °C by heat level), lit blaze burners, steam engines on a running boiler, and pipes, pumps, valves, basins, spouts and item drains holding lava or other hot fluids all radiate heat. Fluid tanks with lava and boilers use Cold Sweat's own heating. Measured 2 blocks away: seething burner +13 °C, lava basin +13 °C, lava tank +7 °C. |
| Cold Sweat | Surface biomes sit around **-12 °C by day and -18 °C at night** (colder in storms), measured in game. Crop growth uses Cold Sweat's real temperature. **No grace period when you respawn at your own bed or respawn anchor** (Cold Sweat would otherwise grant it after every death); first join still gets it. |
| Farmer's Delight | Its wild crops only spawn in warm biomes, so its seeds (cabbage, tomato, onion, rice) are added to cabin and supply cache loot. Greenhouse crops then feed Slice & Dice's cutting automation. Raw FD meats count as raw food. |
| Spoiled | Food spoils after **3 in-game days** instead of 1. Cold Sweat's icebox slows it as usual. |
| Create: Enchantment Industry | Emeralds grind into liquid XP, and Mending books are made with it (see Emeralds). Slice & Dice sprinklers can already spray liquid XP. The Gem Cutter only works with Apotheosis gems. |
| Create: New Age | Heaters, heat pipes and pumps, Stirling engines, solar heating plates, reactor vents and corium warm players (Cold Sweat) and count as greenhouse heat. Corium measured +21 °C two blocks away. |
| Alex's Mobs (Continued) | Its spawn rules use `forge:` climate tags, which now include the frozen biomes. 17 cold-climate species spawn here: moose, snow leopards, bison, grizzlies, froststalkers, tusklins, seals, orcas, whales, crows, eagles and more. Flies, platypuses, raccoons, cockroaches, pupfish and other warm-climate animals are removed (`#arcticfox:out_of_place_wildlife`). Hostile ones on the surface (centipedes, murmurs, farseers) are cave creatures and stay blocked above ground by the frozen-surface rule. |
| Create Deco | Train and metro stations get Create Deco versions with shipping containers, catwalks, warning decals, industrial lamps, cracked and mossy bricks and iron supports. |
| Sophisticated Backpacks / Storage | Use the official **Sophisticated Backpacks Create Integration** and **Sophisticated Storage Create Integration** mods for contraption support. Tested alongside: Backpacks 3.26.9 with integration 0.2.2 (0.2.3 needs Backpacks 3.26.10). |
| Steam 'n' Rails | Parts and blocks only: Conductor mobs are banished. Stations use its tracks and hazard stripes when it's installed. |
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
