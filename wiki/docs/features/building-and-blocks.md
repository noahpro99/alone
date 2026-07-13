---
sidebar_position: 8
title: "Weight, Carrying & Blocks"
---

Everything you carry has a weight and a volume, and breaking or moving blocks costs time and stamina.

## Weight and volume

Every item in the game has two properties: a **weight** (how heavy it is) and a **volume** (how much space it takes up). A full solid building block is bulky — one nearly fills your hands on its own; small items like seeds, bones, or torches are tiny.

These two properties do very different things.

### Volume is a hard cap

Your body carry is split into **two separate budgets**:

- A **hands budget of about 1 m³** for **bulky items** — anything bigger than pocket size: a block, tools, boards. A full building block nearly fills it on its own, so you can manage roughly one at a time.
- A **pockets budget of about 0.30 m³** for **small items** — each one **0.05 m³ or smaller** (seeds, flint, nuggets, food, a coil of rope). The two are additive: filling your pockets never eats into your ability to carry a bulky thing, and vice versa.

When a budget is full, **extra items of that kind simply aren't picked up**. They stay on the ground in the world, exactly like a full vanilla inventory — nothing is thrown away or destroyed.

To carry more, see [Storage & Transport](./storage-and-transport.md) — a woven basket raises your hands budget, and a backpack adds separate storage.

### Weight is a soft penalty

Weight never stops you cold; it just wears you down.

- The **heavier your load, the slower you move**. There's no penalty up to about **8 kg**; from there the slowdown scales down to the **slowest crawl — roughly 35% of normal speed — at about 45 kg**.
- **Jumping under a load costs extra stamina** — the heavier you are, the more each hop up a hill or a step drains.
- A heavy load also makes swimming harder — see [Weight in the water](#weight-in-the-water) below.

### The carry bars

Three bars — **hands**, **pockets**, and **weight** — appear **only while your inventory is open**, not on the always-on HUD. Each fills as its budget is used and turns amber, then red, as it approaches the limit.

## Chests are volume-capped too

Storage containers hold a real volume, not a fixed slot count:

| Container | Capacity |
|---|---|
| Chest (27 slots) | 1 m³ |
| Double chest | 2 m³ |
| Barrel | 1 m³ |

- **Functional blocks** like furnaces, blast furnaces, and crafting stations are **uncapped** — their inventories work normally.
- **Partial placement:** dropping a big stack into a near-full chest deposits **what fits** and leaves the rest in your hand, rather than refusing the whole stack.

## You can't throw blocks away

Block items are meant to be built with or stored, not littered:

- **Dropping a solid building block is refused** — whether you press the drop key or drag it out of the inventory window, it's **handed straight back** to you. (Small placeable things like seeds, saplings, and torches drop normally.)
- **Heavy items barely toss.** The heavier a thing is, the weaker the throw; by around **20 kg** an item just plops at your feet with no horizontal travel.

(Death drops are untouched — dying still scatters your things normally.)

## Weight in the water

A heavy load affects swimming:

- Carry more than about **22 kg** (one full block weighs roughly **30 kg**) and you **sink slowly** — a steady, unstoppable descent, not a plummet.
- The more you haul, the **faster your stamina drains** while you swim.
- Being **submerged also chills you** — extra cold on top of the effort. See [Body Temperature](./body-temperature.md).

## Break times

Every block you break **costs stamina** and takes real time:

- **Stone** with an iron pick takes about **20 seconds**; **deepslate and ore** run up to **~60 seconds**.
- **Packed earth** is slow, heavy work: **dirt** takes around **three in-game minutes with a shovel** and is near-hopeless by hand, and **dense clay** is slower still. **Loose sand and gravel** scoop faster — roughly a minute a block. A **shovel is far faster than bare hands** (bare hands are fixed-speed, so no other tool beats them at digging).
- **Grass has to be cleared first.** Breaking a **grass block** doesn't hand you a block of dirt — it strips the **turf** off, leaving **bare dirt in place** that you then dig out like any packed earth. Clearing the grass is its own job: **laborious by hand** (pulling grass and roots, ~26s) and **quicker with a hoe or shovel** (~9s). So turning a grassy field into a pit is honestly two stages — de-turf, then excavate.
- **Progress is saved if you stop partway.** Leave a dig and return, and the crack resumes where you left off. A barely-scratched dig (under ~10% done) **heals back** to a pristine block instead of leaving a permanent chip.

## Realistic reach

You can only break or place blocks within about **3 blocks** (down from vanilla's ~4.5); reaching entities is shorter still, about **2.5 blocks**. (Creative keeps the long vanilla reach.)

## Trees and leaves

**Felling trees** depends on your tool tier — the wrong tool barely bites. Full details are on [Tools & Crafting](./tools-and-crafting.md).

**Leaves feel like real foliage** and are slow, tugging work either way:

- Breaking leaves **by hand** yields snapped **sticks and leaf litter** — not a tidy leaf block (hand is roughly **12× slower** than normal).
- An **axe or hoe shears the whole leaf block free** (roughly **3× slower** than normal).
- Either way, vanilla **saplings and apples still drop**.

Leaf litter is a fire tinder.

## Soil has no cohesion — dig with care

Loose soil doesn't float. **Dirt, grass, coarse dirt, podzol, mud, and the like fall like gravel** the moment nothing holds them up — undercut a dirt wall or knock out the block beneath one and it collapses. (Sand and gravel already did this in vanilla; clay is cohesive and stays put.) You can't build a floating dirt bridge or leave an overhang hanging in the air.

### Timber holds up the roof

Real diggers don't tunnel through bare earth — they **shore it**. Undercut soil stays put as long as there's a **grounded structural support within about 4 blocks horizontally**: a placed **beam, post, plank, log, or stone wall** — anything solid that stands on something. More loose soil doesn't count; only real timber or masonry bears the earth.

So a proper dugout is buildable:

- **Set posts or beams** as you dig and the earth **between supports holds** — you can hollow out a room or run a tunnel without the ceiling caving.
- **Dig past the reach of any support** (more than ~4 blocks from timber) and the unsupported span **still collapses**.
- A **bare-earth room with no supports caves in**, because soil can't hold soil. Keeping a dugout open is an investment in timber or stone, exactly as real excavation demands.

This is what makes a dug **[root cellar](./preservation.md#cold-storage-the-root-cellar)** an earned build rather than a free hole: you shore the roof, and the cool covered space it creates keeps your food for weeks.

> One current limitation: pulling out a support doesn't *instantly* drop the roof it was holding — collapse near a removed beam is checked the next time that spot is disturbed, so it can lag rather than cave the moment the timber is gone.

### Rammed earth — shore with dirt, not timber

If you have no timber to spare, there's an older way: **ram the earth firm.** Hold a **rock** <img class="mc-icon" src="/alone/item/rock.png" alt="Rock"/> or a
**smithing hammer** <img class="mc-icon" src="/alone/item/smithing_hammer.png" alt="Smithing hammer"/> and **sneak + right-click a block of soil** to tamp it. It's **hard, tiring work**
(each tamp costs stamina), but packed **rammed earth**:

- **won't cave** on its own, and
- **bears load like a post** — a tamped column shores the soil around it within the same ~4-block span a
  timber would.

So you can keep a dugout open by ramming a few **earth pillars** instead of felling trees — paid for in
**sweat instead of timber**. A tamped block is also **committed to structure**: no longer a loose
set-down block, so it breaks slow again (you can't quick-pull it back up). Only the dirt-family soil that
actually caves can be tamped.
