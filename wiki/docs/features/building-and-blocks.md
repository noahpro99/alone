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
