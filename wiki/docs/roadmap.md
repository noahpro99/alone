---
sidebar_position: 4
title: "Roadmap"
---

# Roadmap

What's **already in the pack** is documented in [Features](./features/survival-meters.md). This page
is the other half: the systems still **planned but not yet built**, drawn from the project's design
vision. Nothing here is a committed timeline — features graduate from this page into the Features
wiki as they ship.

The through-line is unchanged: *vanilla Minecraft is a game about accumulation; Alone is a game
about maintenance,* with a long-horizon goal (the Dragon) that takes seasons to attempt.

## Fire, heat & industry

The campfire, the clay kiln (fires pottery), and the bellows-fed bloomery (smelts iron ore to a bloom)
exist; the rest of the heat-tech tree does not yet.

- **A campfire menu.** The campfire should open its **own screen** on right-click — a hearth interface with
  slots to lay food on to cook, tend the fuel, and set a pot to boil — rather than today's stack-items-on-top
  + hold-right-click interactions. Makes the fire a proper station you manage.
- **A pit / bowl furnace** — the *crudest* smelting, below the built bloomery. IRL, before proper furnaces,
  metal was won in a **clay-lined pit or bowl** dug in the ground: layered with charcoal and (crushed) ore,
  covered, and force-fed air with bellows or a blowpipe until it burned hot enough to reduce the ore to a
  spongy **bloom** you then hammer. It's basically a hole-in-the-ground bloomery — smoky, inefficient, and
  the true start of metallurgy. Could slot in as a **pre-bloomery copper tier** (a dug pit you smelt in
  before you can build the clay {@code bloomery} already in the pack).
- **A charcoal clamp** (bulk charcoal from a covered wood pile) — a batch-efficiency step up from what
  works today: smelting logs one at a time in a **furnace** already yields charcoal (as does a
  burnt-out campfire), so this is an optimisation, not a missing source.
- **A glass furnace** and glass gating (below), above the kiln and bloomery already in the pack.
- **The ash economy:** hardwood ash → lye → potash, feeding soap and glass flux.
- **Glass that gates itself** behind forge heat + flux + lime, arriving late as the trophy it
  historically was.
- ~~**A flint-and-pyrite fire tier**~~ **shipped** — the primitive strike-a-light between friction fire and
  the forged ferro rod. **Pyrite** turns up now and then when you sift **gravel**; craft it with a flint
  shard into a **flint-and-pyrite** striker that catches quick for almost no effort. Unlike the ferro rod its
  cooler spark still needs dry tinder, so it fails in the rain — that all-weather edge stays the ferro rod's.
  See [Fire](./features/fire.md#flint-and-pyrite--the-strike-a-light).
- **Candles & lanterns:** cheap, short-lived tallow candles and permanent iron+oil lanterns above the
  burn-out torch, so deep caving becomes a light-supply problem.

## Water

- **Finite, flowing water** with real currents (you can't swim up a waterfall; boats get swept).
- **Glass bottles** and **seawater distillation** to reclaim *fresh* water from the sea (a condenser, not
  just evaporation). *(Boiling seawater to dryness for **salt** has shipped — see
  [Food Preservation](./features/preservation.md#getting-salt--boil-down-seawater).)*

*(Hot-rock boiling — heat stones in the fire and drop them into a hide/bark vessel that can't go over the
flame — has shipped. See [Water & Drinking](./features/water-and-drinking.md#hot-rock-boiling--for-the-waterskin).)*

## Food, farming & preservation

- **Deeper fishing:** bait, tackle tiers, and the right spots (a fishing rod is still vanilla otherwise).
  *(**Finite, slow-recovering fish stocks** have shipped — fish out a spot and the catches dry up until it
  recovers over the following days, so a small pond empties fast while a lake or the sea lasts; see
  [Food & Eating](./features/food-and-eating.md#fishing).)*
- **Purpose-built food stores:** **sealed barrels, hung caches, and smokehouses** that beat the scent
  better than a plain chest. *(The scent system has shipped — carried fresh meat draws wolves and polar
  bears, which will **snatch it and bolt** if they reach you, countered by preserving or caching it; see
  [Food Preservation](./features/preservation.md#fresh-meat-draws-predators).)*
- **Mushroom toxicity & identification:** some foraged mushrooms are poisonous, learned by trial or
  study and then permanently marked in a foraging journal.

*(**Weeds & field tending** has shipped — crops die to blight and winter frost, **weeds sprout on the soil
around a plot and choke the crop** (and crops sow more weeds if you neglect them), so a field is upkeep you
weed by hand, not a one-time build; trampling and farmland erosion are vanilla. See
[Farming with the seasons](./features/world-and-seasons.md#farming-with-the-seasons).)*

## Wildlife & hunting

*(**Deer** have shipped as the forest's proper wild game — skittish grazers that bolt and must be stalked or
run down, spawning in forest/taiga/hill country, yielding venison and hide, counting toward overhunting, and
breeding only outside winter — on **placeholder (cow) art** until a real deer model is made. **Local
overhunting** and **seasonal breeding** shipped too: hunt a patch too hard and the game thins out until it
recovers, and animals won't breed in the dead of winter. See
[Hunting & Wildlife](./features/hunting.md#deer--the-forests-wild-game).)*
- ~~**The trapping ladder**~~ **shipped** — passive food you set and leave, the way people live on *Alone*:
  the **snare** (free, passive, low-odds small game), the **deadfall** (baited, one-shot, better returns),
  and the **fish trap / weir** (passive fishing on the finite fish stock). All three are placed traps that
  keep working while you're away and draw on the local game/fish population; see
  [Hunting & Wildlife](./features/hunting.md#trapping--the-food-that-works-while-you-dont).
- **More small game:** **grouse/ptarmigan** and other birds could join the wild game later. *(The vanilla
  **rabbit** and **chicken** and a new **squirrel** already behave as skittish wild game — bolt, tire fast,
  count toward overhunting, give a scrap of meat — and the squirrel **climbs trees** when you close on it;
  see [Hunting & Wildlife](./features/hunting.md#small-game--the-staple-you-actually-live-on).)*
- **A wilderness that's actually wild** — *mostly shipped.* The wild holds only wild animals now:
  **domestic livestock (cow, pig, sheep, chicken) no longer spawn in the open** — their natural spawns are
  held to **villages**, the same way hostiles are confined near structures — and two **dangerous wild grazers**
  fill the big-game niche: a **bison** that grazes plains/savanna in herds but **charges and gores** (and turns
  the whole herd on you) when provoked, and a **wild boar** that roots the forests and taiga and charges when
  cornered. So big-game meat and hide are *huntable in the wild*, but you earn them at real risk. See
  [Hunting & Wildlife](./features/hunting.md#dangerous-game--the-bison-and-the-wild-boar). *Still to come:*
  **more biome-correct fauna** — cold-country birds (grouse/ptarmigan) and other regional species — so the
  country you're in decides even more of what you can hunt.
- **More tracking sign:** footprints and snow tracking, to follow quarry you can't see. *(Persistence
  hunting, blood trails, and bleed-out have shipped, and now a **prevailing wind** — a steady direction
  each day that carries your scent, so a predator downwind finds you from far off and one upwind barely
  at all; see [Hunting & Wildlife](./features/hunting.md#the-wind-carries-your-scent).)*
- ~~**Rendered tallow → candles**~~ **shipped** — the last piece of full-animal use. A blade kill on big game
  now yields **raw fat**; **render it over a fire** (campfire or furnace) into **tallow**, then cast it around
  a cordage wick (tallow + string) for a **candle**. So meat, hide, bone, sinew, marrow, brains, *and* fat all
  find a use. See [Hunting & Wildlife](./features/hunting.md#a-blade-kill-butchers-the-carcass--hide-bone-and-sinew).
- ~~**Netting** as a second counter to insect pressure~~ **shipped** — a **bug net** woven from string + a
  fibre drawstring, worn on the head (in place of a helmet) to veil off the swarm anywhere, no fire needed.
  *(Insect pressure near standing water — countered by a smudge fire — and **insect-borne fever** (sustained
  unprotected exposure in humid wetlands can give you the sickness illness) shipped earlier; see
  [World & Seasons](./features/world-and-seasons.md#biting-insects).)*

## Building & shelter

- **The shelter ladder:** light stick-frame blocks (that weather and can be broken by a bear),
  through log and stone tiers.
- **A shown Shelter Quality rating** — a readout of how good your shelter is. *(The mechanic behind it
  has shipped: how enclosed you are grades your temperature protection, which in turn feeds sleep
  recovery; see [Body Temperature](./features/body-temperature.md#what-warms-and-cools-you).)*
- **Felling real trees:** cut through the base and the tree **falls as an entity** (and can crush
  you), with **sapling → young → mature → old-growth** age states that scale a tree's height, chop
  time, and yield (an old-growth giant worth several times a young one).

*(Structural gravity — loose soil falls and must be shored with timber or **rammed earth** (the
tamp tool) — and fire spread — an open campfire catches wood it touches — have all shipped. See
[Weight, Carrying & Blocks](./features/building-and-blocks.md#soil-has-no-cohesion--dig-with-care) and
[Fire](./features/fire.md#fire-is-a-hazard--keep-it-clear-of-wood).)*

## Transport

The backpack, the **travois/sledge** (a dragged cargo frame), and **rivers as highways** (loose logs and
light goods float downstream) exist; the rest of the logistics tree is planned.

- **Wheelbarrow**, **rafts and boats**, **pack animals**, and the big unlock — a **horse + cart** whose
  demands make **road-building emergent gameplay** — up to **rails** as the endgame.

## The world & navigation

- **Weather with teeth:** storms that fray builds. *(**Blizzards** — a winter storm freezes the exposed fast
  (see [Body Temperature](./features/body-temperature.md#what-warms-and-cools-you)) — and **telegraphed
  fronts** — the sky gives you a lead warning of a coming storm so you can get under cover, your fire
  covered, and your drying meat in — have both shipped. Drought is folded into the
  [farming](./features/world-and-seasons.md#farming-with-the-seasons) crops-dry-out mechanic.)*
- **Biome strategies** — each biome a different opening hand.
- ~~**A wind indicator on the HUD**~~ **shipped** — a north-up **wind vane** under the temperature square:
  a needle pointing the way the [wind](./features/hunting.md#the-wind-carries-your-scent) blows, its
  **length the wind's strength** (a stub on a calm day, a long needle in a gale). Read straight off the
  world clock client-side. The old "you feel the wind" chat notes are now **gone** — the vane replaces them.
- **Navigation without coordinates:** a craftable compass, hand-drawn maps that fill as you walk,
  celestial direction, and cairns. Getting lost becomes possible.

## Structures, loot & the old world

- **Structure-scoped loot** with **relics** — the uncraftable top of the ladder (artifacts that act
  like enchantments, true potions, golden apples).
- **Rationalized mob drops** (the good gear is in the chests, not the corpses).
- **Redstone gated** behind found engineering plans.
- **Villager barter and reputation**, and **hired villagers** you pay in food.
- **Village guards** — armed defenders (**bow and sword**) that hold a village. Steal from or strike
  *anything* in the village and **the whole place turns on you** — guards close in, tactically (this is the
  real home for the cover / advance-under-fire AI first tried on the iron golem). A village becomes a
  guarded settlement to raid or trade with, not a free larder.
- **Diamonds repurposed:** not armour (that's abolished) but **diamond-tipped picks and saws** that
  cut faster and cleaner, and an elite trade good in their own right.

## The body & combat

- **A real calorie model.** Today hunger is vanilla food + saturation, with fatigue/stamina and a
  cold-food-burn bolted alongside. A truer model would track **calories in vs out**: food carries an energy
  value, and you *spend* energy on a **basal rate** plus **activity** (walking < sprinting < mining/chopping <
  swimming) and **thermoregulation** (already gestured at — a cold body burns more). Then starving, freezing,
  and overworking all draw on the same honest ledger, and a hard day's labour genuinely needs feeding. Big,
  and it wants care so it deepens the survival loop without turning into spreadsheet micromanagement.
- **Digestion & sanitation:** a latrine/contamination system where fouling your own camp or water
  source sickens you. *(Dysentery — the acute dehydration emergency from drinking foul water — has
  shipped; see [Injuries & Conditions](./features/injuries-and-conditions.md#dysentery).)*
- **Combat depth:** deeper melee than trade-blows. *(Pieces shipped: the spear as king of the
  early game — a flint spear with real thrusting [reach](./features/tools-and-crafting.md#the-spear--reach-from-the-first-day);
  **shields defeated by large creatures**: bracing a shield against a bear, ravager, golem, or the
  warden gets it **bashed aside** — knocked down for a few seconds so the follow-up lands; and a **shield
  tier ladder** — a woven **wicker** shield (day-one) → a **wooden** plank shield → the metal vanilla shield,
  each sturdier and harder to bash aside, though no tier lets you turtle against a bear. See
  [Hunting & Wildlife](./features/hunting.md#the-brown-bear). You can turn a blade, not a charging bear.)*

## Crafting & labour

- **Work sessions — the honest speedup:** big items (a dugout canoe, a timber frame) are multi-day
  projects you *actively engage* to enter a time-compressed "spent the afternoon on it" mode — meters
  keep draining, and a nearby event (a predator, a storm) interrupts you. The engine that makes days
  of labour playable without either tedium or a free instant craft.

## Livestock & economy

- **Domesticated animals as bartered wealth**, priced in grain — earned through many harvests, never
  spawned or found wild.
- **Village livestock.** With domestic animals now gone from the open wild (done — see
  [Hunting & Wildlife](./features/hunting.md#the-wild-is-wild--livestock-live-at-villages)), the place
  you meet a cow, sheep, or chicken is a **village** — the settlement's own herd. So beef, wool, and cow-hide
  leather are things you **trade for, tend, or raid**, not free wilderness drops. Pairs with the planned
  **armed village guards** (steal or strike anything and the village turns on you), making a livestock raid a
  real, dangerous option rather than a free lunch.
- **Tanning — leather is made, not dropped** *(shipped).* Butchering a hide-bearing animal now yields a
  **green raw hide** (plus a lump of **brains**), not finished leather. Leather is worked on the **drying
  rack** — the very same lashed frame that cures jerky, doing double duty: load a raw hide + brains and it
  **brain-tans** into leather over ~three in-game days (about twice its meat-cure), not a hold-right-click hand
  chore. So a leather coat is a real investment, in keeping with the pack's "everything is real effort" rule. See
  [Hunting & Wildlife](./features/hunting.md#from-raw-hide-to-leather--tanning). *Possible refinements:*
  alternative tannins (bark liquor), a scraping/dehairing sub-step, and smoking to finish — deeper realism if
  it earns its keep.

## The start — extending the loadout

The **Alone-style start is built** — you wake with nothing, read your biome, and pick two items from an
approved list (see [The Start — Your Loadout](./features/the-start.md) for how it plays). What remains here
is ways to extend it:

- **Graphical selection screen** — a proper picker window over the existing command/clickable-chat loadout
  (same underlying picks, biome read, and one-time gate).
- **More list options to build:** ~~a **slingshot**~~ **shipped** — the low-power ranged weapon below the
  bow: draw and loose a foraged loose rock to drop small game (rabbit/chicken/squirrel), useless on anything
  big; see [Hunting](./features/hunting.md#the-slingshot--the-first-ranged-weapon). Still to build — and,
  deepening existing systems — a **frying pan** (better cook surface) and a **bar of soap** (better/faster
  washing). *(Shipped: the **gill net** (open-water fishing; see
  [Hunting](./features/hunting.md#the-gill-net--the-open-water-alternative)), the **hand saw** (fast
  log→plank; see [Tools & Crafting](./features/tools-and-crafting.md#boards--riving-by-hand-or-a-hand-saw)),
  the **towel** (instant dry-off; see
  [Body Temperature](./features/body-temperature.md#what-drives-your-temperature)), and the **grain→bread
  path** (grind → dough → bake; see [Food & Eating](./features/food-and-eating.md#bread--grind-dough-bake)).)*
- **Decided against:** a **personal photograph / morale system** — Alone's realism here is physical, and a
  happiness meter would be a HUD abstraction rather than a felt mechanic, so it's deliberately not built.

## Death, respawn & run modes

- **Hardcore-Alone run modes:** self-set victory conditions (survive the winter, survive a year, slay
  the Dragon) with a tap-out stat screen — the framing that turns the sandbox into a run. *(The
  carried-home death model has shipped — you wake days later, wounded, hungry and weak, with your gear
  left where you fell and its meat drawing scavengers, not a corpse-run.)*

## The endgame

- **The Dragon as a multi-season expedition:** the Nether as a heat-and-no-water death zone,
  obsidian hauling as real logistics, and a supply line running through structures you've cleared.

---

> Looking for the technical build log of what's shipped? It lives in the repository's Git history
> and the **[Engineering](./engineering/architecture.md)** section, not here.
