---
sidebar_position: 4.5
title: "Food Preservation"
---

# Food Preservation

Perishable food rots over time. Preservation converts it to a form with a much longer shelf life.

## Spoilage

Perishable foods (raw meat, fish, and other items in the perishable list) carry a **freshness
budget** that drains over time — but **how fast it drains depends on temperature**, because before
refrigeration, *where and when you keep food is the whole game*.

- A **fresh** perishable has enough budget for **~1 in-game day at comfortable temperature**. When
  it runs out, the item turns into **rotten flesh**.
- **Heat drains it faster, cold drains it slower**: the rate roughly **doubles for every +25°** and
  **halves for every −25°**. A warm summer hut rots meat fast; a cold biome, winter, or a cool
  cellar stretches the same piece for much longer (see [Cold storage](#cold-storage-the-root-cellar)).
- **Preserved** food (salted or dried) carries a much bigger budget — **~30 in-game days** — long,
  but not infinite; even jerky eventually goes rancid and turns to rotten flesh.

Spoilage only advances while the food is being **watched** — carried on you, or sitting in a
container in the chunks around you. **Leaving your base pauses** the food you left behind, so it
won't silently rot away while you're off exploring.

Non-perishable items (bread, golden apples, and similar keepers) are not on the list and do not
spoil. See [Food & Eating](./food-and-eating.md) for the risks of eating spoiled or raw food.

## Cold storage: the root cellar

Because spoilage is temperature-driven, **a cold place is itself a preservation method** — no salt,
no fire, just the cool of the earth.

Dig a storage space that is **covered (no open sky above it) and below sea level**, and it reads as
an **earth-cooled root cellar**: the food in any chest or barrel down there spoils at a deep-cold
rate — roughly **−20° just under the surface, down to about −50° in a deep cellar** — which stretches
a fresh perishable's ~1-day shelf life into **weeks**. The **deeper you dig, the colder** it gets.

The catch is that you have to **build the cellar honestly**. Loose soil caves when you undercut it
(see [Soil has no cohesion](./building-and-blocks.md#soil-has-no-cohesion--dig-with-care)), so a
real cellar means **shoring the roof with timber or stone** — or digging into solid rock. The full
loop is: **dig down, shore or line the space so it doesn't collapse, and store your harvest in the
cool dark.** An earned root cellar, not a free hole in the ground.

> Chests and barrels within the loaded chunks around you are swept for spoilage at their own local
> temperature — so the same barrel keeps food far longer sitting in a deep cellar than it would in a
> sunlit surface hut.

## Salting

Salting marks a food as **preserved** (the ~30-day shelf life):

1. Hold the perishable food in your **main hand**.
2. Hold **salt** in your **off hand**.
3. **Sneak + right-click.**

This consumes **one salt** and cancels the food's running spoil timer.

> **Salt currently has no obtainable source.** Boiling seawater does **not** produce salt (and does
> not desalinate it), and there is no salt recipe or drop in the current build. Until a salt source
> is added, salting can't be used in practice — the **drying rack** below is the working
> preservation method.

## The drying rack

The drying rack preserves meat without salt. It is crafted from **5 sticks**:

```
S S S
S . S
```

Place it, then **right-click it with a perishable food** to hang one piece. **Right-click the rack
empty-handed** to take the food back (whether it's finished or still drying); breaking the rack also
drops whatever is hung on it. The rack **keeps drying while you are away** — progress tracks elapsed
world time even while the area is unloaded.

Finished food becomes **jerky**: it is marked **preserved** (~30-day shelf life) and **dried**,
which also makes it **lighter to carry** (the water is gone).

### Drying speed depends on conditions

Air-drying a piece takes about **1.5 in-game days** in normal temperate, dry conditions. Temperature,
weather, and biome change that rate, and warm wet conditions **spoil the meat instead of drying it**:

| Condition | Result |
|---|---|
| **Smoked** (lit campfire two blocks below, with an air gap) | Fastest — about **0.4 in-game days**, and weather cannot stop it |
| **Warm, dry air** (hot biome, not raining) | About **0.75 in-game days** |
| **Temperate, dry** | About **1.5 in-game days** |
| **Cold, dry** (winter, or a cold biome) | Slow but safe — about **3.75 in-game days** (freeze-drying) |
| **Cold rain or snow** | Paused (frozen); no progress and no spoiling |
| **Warm and wet** (rain, or a warm humid jungle/swamp) | Does not dry; the meat **rots** after about **half an in-game day** of exposure, becoming rotten flesh |

### Smoking

Smoking requires a **lit campfire two blocks directly below the rack, with an air gap between them**
so cool smoke rises to the meat. A fire, lava, or lit campfire placed **directly beneath** the rack
instead sets the wooden rack alight — the rack and the food on it are destroyed.
