---
sidebar_position: 1
title: "Survival Meters & HUD"
---

Your body is tracked by a small stack of bars and markers in the top-left corner of the screen — it stays quiet until something is wrong, then it tells you exactly what.

## Reading the HUD

The corner readout stacks three bars, each with its own icon, plus a temperature square and a paper-doll injury figure off to the side. There is **no health or vitality bar** — the body is a set of separate systems (you bleed out, freeze, dehydrate, starve, or die of trauma), never one HP pool. See [Injuries, Conditions & Health](./injuries-and-conditions.md). From top to bottom the bars are:

- **Stamina** (golden pickaxe) — green. Your short-term wind for hard effort.
- **Endurance reserve** (golden carrot) — how rested you are. Full means fresh; it drains as you tire over the day.
- **Thirst** (water bucket) — blue. Covered on [Water & Drinking](./water-and-drinking.md); summarized below.

To the right sit the **temperature square** with a warming/cooling arrow and the **paper-doll injury figure** (see below). Carry weight and volume are **not** on this always-on HUD — they appear only while your inventory is open.

## Stamina (the green bar)

Stamina is your immediate wind — the effort you can spend before you have to stop and breathe.

- It **drains** when you sprint, jump, swim, mine, chop wood, fight, and climb.
- A hard sprint empties the bar in about **40 seconds** — this is a real run, not a quick dash.
- It **recovers** when you rest and stop exerting, refilling over roughly **25 seconds** of standing still.
- When it runs **near-empty**, your body gives out: you're **slowed**, hit with **mining fatigue**, and **can't sustain a sprint** until you've caught your breath.
- If your stamina runs out while climbing, **you fall**.

### Hunger gates recovery

Stamina only rebuilds properly when you're fed. **Below 6 hunger, stamina recovers poorly.**

## Endurance reserve (fatigue)

Below the stamina bar is your **endurance reserve** — a slower, deeper gauge. Full means well-rested; it drains steadily as you tire across the day and through hard exertion. You shed fatigue and refill this reserve by **resting and sleeping** — see [Sleep & Rest](./sleep-and-rest.md).

## Thirst (the blue bar)

Thirst drains over time, faster when you're sprinting or your body runs hot. Drink to refill it, but **untreated water can make you sick** (roughly a **15% chance** from a raw fill, higher from a dirty vessel). Boiling water over a campfire makes it safe. Full details — vessels, water quality, salt water, and boiling — are on [Water & Drinking](./water-and-drinking.md).

## Carry — volume and weight (inventory only)

Carry isn't shown on the always-on HUD — it appears only **while your inventory is open**, where it's split into three bars: a **hands** volume bar (bulky items carried in your arms), a **pockets** volume bar (small items), and a **weight** bar.

- The volume bars run **grey → amber → red** as they fill; at the cap, extra items simply won't be picked up (they stay in the world, nothing is dropped).
- Weight is separate and softer: the heavier your load, the **slower you move**. One heavy block makes you trudge.

The full weight-and-volume system — hands vs. pockets, chest limits, transport, building loads — is covered on the Building page.

## Realistic reach and honest work

- **Reach** is shortened to about **3 blocks** (down from vanilla's ~4.5; reaching entities is shorter still, about 2.5). You have to be close to what you break or place. Creative keeps the long vanilla reach for building.
- **Break times are honest.** Mining stone with an iron pick takes around **20 seconds** (ore and deepslate longer, capped near 60s), and digging dirt, sand, or gravel takes a few seconds even with a shovel. Every block you break also **costs stamina** — quarrying is genuine labour, not a formality.

## The paper-doll injury figure

Beside the temperature square, a small paper-doll figure of your body colors the affected part when a condition is active:

- **Bleeding** — red torso
- **Sprain** — orange legs
- **Dirty hands** — brown hands
- **Infection** — purple arms
- **Sickness** — green body

These are explained in full on [Injuries, Conditions & Health](./injuries-and-conditions.md).
