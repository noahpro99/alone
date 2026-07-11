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

The campfire exists; the rest of the heat-tech tree does not yet.

- **Furnace tiers** above the campfire: a clay kiln (fires pottery, makes charcoal), a charcoal
  clamp (bulk charcoal), a bellows-fed bloomery/forge for smelting, and a glass furnace.
- **The ash economy:** hardwood ash → lye → potash, feeding soap and glass flux.
- **Glass that gates itself** behind forge heat + flux + lime, arriving late as the trophy it
  historically was.
- **A flint-and-pyrite fire tier** — a faster, rain-tolerant strike fire (flint + found pyrite)
  between friction fire (shipped) and forge-era flint-and-steel.
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

- **Deeper farming:** fishing as a primary food source with finite, slow-recovering stocks. *(Salting,
  drying racks, smoking, root cellars, and ice houses have shipped — see
  [Food Preservation](./features/preservation.md).)*
- **Purpose-built food stores:** **sealed barrels, hung caches, and smokehouses** that beat the scent
  better than a plain chest. *(The scent system has shipped — carried fresh meat draws wolves and polar
  bears, which will **snatch it and bolt** if they reach you, countered by preserving or caching it; see
  [Food Preservation](./features/preservation.md#fresh-meat-draws-predators).)*
- **Weeds & field tending:** weeds spawn on farmland, slow and choke crops, and spread to neighbours
  unless cleared by hand/hoe; untended fields fail, trampled crops die, farmland erodes back to dirt.
- **Mushroom toxicity & identification:** some foraged mushrooms are poisonous, learned by trial or
  study and then permanently marked in a foraging journal.

## Wildlife & hunting

- **Scent-driven predator AI** — bears above all — that raid unsecured food.
- **Deer and rabbit populations** with seasonal breeding and local overhunting.
- **More tracking sign:** footprints, wind direction, and snow tracking, to follow quarry you can't see.
  *(Persistence hunting, blood trails, and bleed-out have shipped; see
  [Hunting & Wildlife](./features/hunting.md#persistence-hunting--run-it-down).)*
- **Fat from a carcass** (→ rendered tallow → candles) — the last piece of full-animal use. *(A blade
  kill already yields meat, hide, bone, and sinew; see
  [Hunting & Wildlife](./features/hunting.md#a-blade-kill-butchers-the-carcass--hide-bone-and-sinew).)*
- **Netting** as a second counter to insect pressure, and insect-borne disease. *(Insect pressure near
  standing water, countered by a smudge fire, has shipped — see
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

The backpack exists; the rest of the logistics tree is planned.

- **Wheelbarrow**, **travois/sledge**, **rafts and boats**, **rivers as highways** for floating
  logs, **pack animals**, and the big unlock — a **horse + cart** whose demands make **road-building
  emergent gameplay** — up to **rails** as the endgame.

## The world & navigation

- **Weather with teeth:** storms that fray builds, blizzards, droughts, telegraphed fronts.
- **Biome strategies** — each biome a different opening hand.
- **Navigation without coordinates:** a craftable compass, hand-drawn maps that fill as you walk,
  celestial direction, and cairns. Getting lost becomes possible.

## Structures, loot & the old world

- **Structure-scoped loot** with **relics** — the uncraftable top of the ladder (artifacts that act
  like enchantments, true potions, golden apples).
- **Rationalized mob drops** (the good gear is in the chests, not the corpses).
- **Redstone gated** behind found engineering plans.
- **Villager barter and reputation**, and **hired villagers** you pay in food.
- **Diamonds repurposed:** not armour (that's abolished) but **diamond-tipped picks and saws** that
  cut faster and cleaner, and an elite trade good in their own right.

## The body & combat

- **Digestion & sanitation:** a latrine/contamination system where fouling your own camp or water
  source sickens you. *(Dysentery — the acute dehydration emergency from drinking foul water — has
  shipped; see [Injuries & Conditions](./features/injuries-and-conditions.md#dysentery).)*
- **Combat depth:** shields drained and defeated outright by large creatures. *(The spear as king of
  the early game has shipped — a flint spear with real thrusting reach; see
  [Tools & Crafting](./features/tools-and-crafting.md#the-flint-spear--reach).)*

## Crafting & labour

- **Work sessions — the honest speedup:** big items (a dugout canoe, a timber frame) are multi-day
  projects you *actively engage* to enter a time-compressed "spent the afternoon on it" mode — meters
  keep draining, and a nearby event (a predator, a storm) interrupts you. The engine that makes days
  of labour playable without either tedium or a free instant craft.
- **Sleep as a gradual time-skip:** sleeping runs the clock *fast* rather than teleporting to dawn, so
  the night can be **interrupted** — a scent or storm event wakes you "a second later, but hours later in
  game time." *(The metabolism half has shipped — you now wake having burned the night's food and water;
  see [Sleep & Rest](./features/sleep-and-rest.md#sleep-isnt-free--you-wake-hungry-and-dry).)*

## Livestock & economy

- **Domesticated animals as bartered wealth**, priced in grain — earned through many harvests, never
  spawned or found wild.

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
