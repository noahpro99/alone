---
sidebar_position: 4.5
title: "Food Preservation"
---

# Food Preservation

Perishable food rots over time. Preservation converts it to a form with a much longer shelf life.

## Spoilage

Perishable foods (raw meat, fish, and other items in the perishable list) carry a shelf-life timer:

- **Fresh** perishable food spoils after **~1 in-game day** carried in your inventory. When the
  timer runs out, the item turns into **rotten flesh**.
- **Preserved** food (salted or dried) lasts **~30 in-game days** — long, but not infinite; even
  jerky eventually goes rancid and turns to rotten flesh.

Non-perishable items (bread, golden apples, and similar keepers) are not on the list and do not
spoil. See [Food & Eating](./food-and-eating.md) for the risks of eating spoiled or raw food.

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
