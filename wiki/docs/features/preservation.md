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

**Food keeps rotting while you're away.** The budget drains by the **in-game time that actually
elapses** — measured against the world clock, not real seconds — so leaving your base does **not**
pause it. Leave meat in a warm hut and go exploring, and you come home to spoilage; leave it in a
cold cellar and it barely ticks. (Behind the scenes a stack is only re-checked when it's near you
again — carried, or in a loaded container nearby — but the drain then covers the **whole** stretch of
world-time since it was last seen, so nothing is forgiven by walking away.)

### How to tell how fresh it is

Hover any perishable and its **tooltip shows the state**:

- **Freshness: Fresh / Beginning to turn / Going off — eat it soon**, coloured green → yellow → red
  as the budget drains.
- Preserved food also shows a **"Dried jerky"** or **"Salted — keeps for weeks"** line.

Preservation is stored as **hidden data on the food itself** (a freshness budget plus
preserved/dried flags) — a dried piece is the **same item**, just marked, so it stacks and cooks like
any other. There is no separate "jerky" item; the tooltip is how you read its state.

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

## The ice house

When you can't dig a cellar — or you're in a warm climate where the ground isn't cold — you can make
your own cold with **ice**, exactly as people did before refrigeration.

**Harvest ice.** Breaking plain ice normally just leaves a puddle of meltwater. Cut it with a
**pickaxe or axe** instead and you lift out a **whole block of ice** to carry. Ice only forms in the
cold (frozen lakes and rivers, cold biomes, winter), so this is a **seasonal harvest** — and blocks
of ice are **heavy** to haul.

**Pack it around your store.** Ice or snow placed **around a chest or barrel** chills it like a
cellar: one block against it is about **−12°**, and packing it in on **every side** approaches
**−40°** — enough to keep food for **weeks**, above ground and even in a hot biome.

**Keep it dark and enclosed.** Plain ice **melts near light** (torches, lava), so a working ice house
must be **shuttered and dark**, its ice packed and out of the sun — just like the real thing. In a
warm biome no ice forms locally, so you must **haul it in** from the cold and race to get it walled up
before it melts. That hauling and insulating **is** the ice house.

**And it doesn't last forever.** Even shuttered in the dark, ice packed into a **warm or temperate**
biome **slowly melts** from the heat leaking in — so a lowland ice house is a **maintenance store** you
**restock** each winter with fresh-cut blocks, not a one-time build. The colder the climate, the longer
it keeps: in a **below-freezing biome** (a boreal or arctic store) the ice **doesn't melt at all** and
the cold is effectively free. So *where* you dig your ice house matters as much as how you build it.

## Salting

Salting marks a food as **preserved** (the ~30-day shelf life):

1. Hold the perishable food in your **main hand**.
2. Hold **salt** in your **off hand**.
3. **Sneak + right-click.**

This consumes **one salt** and cancels the food's running spoil timer.

### Getting salt — boil down seawater

Salt comes from the sea, the old way. **Fill a fire-safe pot (clay or iron) from the ocean** — it fills
as **salt water** — then **stand it in a lit campfire** (right-click the fire with the pot). Fresh water
boils *clean*, but **seawater boils down to dryness and leaves a crust of salt**: right-click the fire
empty-handed to take the pot back and you get about **2–3 salt** (roughly half the pot's capacity), the
water gone as steam and the pot empty again.

That's **evaporation, not distillation** — you get the salt, not drinkable water back (reclaiming the
fresh water with a condenser is a separate, later thing). But it makes **salting a working preservation
method**: a trip to the coast, a pot, and a fire is all it takes to lay in salt for the winter.

## The drying rack

The drying rack preserves meat without salt. It is crafted from **5 sticks**:

```
S S S
S . S
```

Place it, then **right-click it with a perishable food** to hang one piece — **the piece is shown
sitting on the rack** so you can see what's curing. **Right-click an occupied rack** (empty-handed or
not) to take the food back, whether it's finished or still drying; breaking the rack also drops
whatever is hung on it. The rack **keeps drying while you are away** — progress tracks elapsed world
time even while the area is unloaded.

Finished food becomes **jerky**: it is marked **preserved** (~30-day shelf life) and **dried**,
which also makes it **lighter to carry** (the water is gone).

**You dry meat *raw* — you don't cook it first.** Jerky *is* dried raw meat, and cold-smoking over the
campfire is the same: the drying (and the smoke) are what make it keep, and what make it **safe to eat**.
So hang raw beef straight on the rack, and eating the finished jerky carries **no raw-meat sickness** — the
drying dealt with that. (Old jerky can still turn once it's deep into its 30-day shelf life; that's the
freshness gamble, not a raw-meat one.)

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

## Fresh meat draws predators

Preserving food isn't only about shelf life — **raw meat you carry reeks**, and the wilderness has a
nose for it.

- Carrying **loose, unpreserved perishable food** (raw or cooked meat, fish) draws a nearby **wolf or
  polar bear** toward you — from about **14 blocks**, and **further the more you carry** (up to ~30).
- If one **reaches you**, it **snatches a piece of raw meat and bolts** with it — a hungry raid that
  costs you **food, not health**. It takes no more than **one piece every ~8 seconds**, so a pack can't
  strip you all at once, but standing in the open with a full load of venison is a losing game.
- **Salted or dried food doesn't reek** — preservation seals the smell, so a pack of jerky travels
  clean where a pack of raw meat would trail predators (and a raider can't take what it can't smell).
- Food **stashed in a chest or backpack** — rather than loose on your body — isn't on you to smell, so
  **caching your meat** is the other way to travel unnoticed.
- **Foxes stay skittish** and keep their distance; only bold predators track you. Wolves and polar
  bears no longer flee you, either — they hold their ground.
- The same nose finds **carrion**: fresh meat **dropped on the ground** — a kill left behind, or the
  pile where you **died** — draws predators that **eat it where it lies** (a piece at a time). Don't
  dawdle recovering a meat-laden death pile, and don't expect dropped raw meat to wait for you.

So the trade is real: **haul raw meat and attract danger, or preserve/cache it and travel clean.**

*(Dedicated sealed barrels, hung caches, and smokehouses — purpose-built stores that beat the scent
better than a plain chest — are still planned.)*
