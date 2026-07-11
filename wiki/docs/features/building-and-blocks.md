---
sidebar_position: 8
title: "Weight, Carrying & Blocks"
---

Everything you carry has a weight and a volume, and breaking or moving blocks costs time and stamina.

## Weight and volume

Every item in the game has two properties: a **weight** (how heavy it is) and a **volume** (how much space it takes up). A full solid block is **1 cubic metre (1 m³)**; small items like seeds, bones, or torches are tiny.

These two properties do very different things.

### Volume is a hard cap

You can carry a hard limit of about **1 m³ on your person**. That's the whole rule: one full block *or* a big pile of small things — never both at once.

- When you're at the cap, **extra items simply aren't picked up**. They stay on the ground in the world, exactly like a full vanilla inventory — nothing is thrown away or destroyed.
- A single full block fills your entire personal volume, so hauling raw blocks is a one-at-a-time affair.

To carry more, see [Storage & Transport](./storage-and-transport.md) — a backpack raises this cap.

### Weight is a soft penalty

Weight never stops you cold; it just wears you down.

- The **heavier your load, the slower you move**. One heavy block makes you trudge.
- **Overloading disables sprint, jump, and swim.** Walking and crawling remain available at any load.

### The volume bar

A third HUD bar (below stamina and thirst) shows how full you are:

- **Grey** — plenty of room.
- **Amber** — getting full.
- **Red** — at the cap; extra items won't be picked up.

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

- **Dropping a block item is refused** — whether you press the drop key or drag it out of the inventory window, it's **handed straight back** to you.
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
- Even **dirt, sand, and gravel** take a few seconds — digging a cubic metre of earth is work. A **shovel is faster than bare hands**.
- **Progress is saved if you stop partway.** Leave a dig and return, and the crack resumes where you left off. A barely-scratched dig (under ~10% done) **heals back** to a pristine block instead of leaving a permanent chip.

## Realistic reach

You can only break or place blocks within about **2.75 blocks** (down from vanilla's ~4.5). (Creative keeps the long vanilla reach.)

## Trees and leaves

**Felling trees** depends on your tool tier — the wrong tool barely bites. Full details are on [Tools & Crafting](./tools-and-crafting.md).

**Leaves feel like real foliage** and are slow, tugging work either way:

- Breaking leaves **by hand** yields snapped **sticks and leaf litter** — not a tidy leaf block (hand is roughly **12× slower** than normal).
- An **axe or hoe shears the whole leaf block free** (roughly **3× slower** than normal).
- Either way, vanilla **saplings and apples still drop**.

Leaf litter is a fire tinder.
