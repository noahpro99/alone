---
sidebar_position: 5
title: "Water, Thirst & Vessels"
---

Thirst only ever drops, and the water you find can cause sickness unless it is treated.

## The thirst meter

The blue bar in the HUD is thirst. Unlike hunger, it has no natural way to climb on its own — it **only drains**, and it is topped back up by drinking.

- It falls steadily over time.
- It falls **faster in the heat** and during exertion (sprinting, hard work). A hot body sweats, and sweat costs water.
- **Weakness** sets in as it gets low, then **Slowness** and fatigue when it hits empty.

## Water quality

Every drink and every vessel has a quality. It decides how likely a sip is to cause illness.

| Quality | Where it comes from | Sickness chance |
| --- | --- | --- |
| **Raw** | Straight from a river, lake, or pond | source-dependent (see below) |
| **Tainted / murky** | A dirty vessel, **or warm stagnant swamp/jungle water** | ~45% |
| **Clean** | Boiled, or caught as rainwater | Safe (0%) |
| **Salt** | Ocean, beach, or shore water | Dehydrates instead of quenching |

## Drinking from a source

Pointing at a water source and **right-clicking bare-handed** drinks straight from it. It refills thirst, but untreated water carries risk — and **where you drink matters**, following the real rule that cold, clear, moving water is far safer than warm, still water:

| Source | Bare-hand sickness chance |
| --- | --- |
| **Swamp / jungle / mangrove** (warm, stagnant, murky) | **~45%** — a bad idea raw |
| **Warm, dry country** (savanna, desert oasis) | ~25% |
| **Temperate** (plains, forest) | ~15% |
| **Cold biomes** (mountains, tundra, snowy) | **~6%** — much safer, but never zero |

- No source is ever *truly* safe raw — even a clear cold stream keeps a small floor of risk (giardia doesn't care how pretty the water is). **Boiling is the only sure cure.**
- A **vessel filled from swamp/jungle water fills murky** (tainted, the ~45% tier), so you must boil it before it's safe — via a fire-safe pot, or **[hot-rock boiling](#hot-rock-boiling--for-the-waterskin)** for a hide waterskin.
- Sickness is a lingering foodborne illness.

## Salt water

Ocean, beach, and shore water is **salt water**. Drinking it — bare-handed *or* from a vessel — **dehydrates you** instead of quenching thirst. Rivers and lakes stay fresh; only the sea is salt. **Boiling does not desalinate it** — salt water cannot be made drinkable. But it isn't useless: boiled to dryness, **seawater leaves a crust of salt** (see [Getting salt](./preservation.md#getting-salt--boil-down-seawater)) — the way to stock salt for preserving food.

## Vessels

A vessel carries water for drinking away from a source. There are three:

- **Waterskin** — crafted from leather and string. Holds **3 sips**. Not fire-safe: it can't go over a flame to boil.
- **Clay pot** — fired from clay. Holds **4 sips**, and is **fire-safe** (can be boiled over a fire).
- **Iron pot** — crafted from iron. Holds **6 sips**, and is **fire-safe**.

Both work the same way:

- **Right-click a water source** to fill the vessel.
- **Right-click while holding it** to drink. Drinking from a vessel is a **timed action (~1.6 seconds)**, not instant.
- Each vessel shows a **water gauge** (like a durability bar) tinted by quality: **blue** for clean, **murky** for raw, **sickly** for tainted, **teal** for salt.

## Vessels remember dirt

Vessels track whether they are dirty, and dirt spreads:

- **Sipping raw or tainted water dirties the vessel.**
- A **dirty vessel taints the next fill** — even clean-looking water becomes tainted (~45% sickness).
- **Boiling sterilises the vessel**, clearing the dirt.

## Boiling water clean

Boiling is the way to make untreated water safe.

- A **fire-safe vessel** (the clay pot or iron pot), filled with raw or tainted water, is set on a **lit campfire** by right-clicking the fire with it; right-click empty-handed to lift it back off.
- It takes about **15 seconds** on the fire to come to a boil, and then its whole load turns **clean** — raw or tainted water becomes safe to drink.
- Boiling also **sterilises the vessel itself**, so a dirty vessel comes out clean.
- Boiling **does not remove salt** — salt water can't be boiled *clean*. But boiling a pot of seawater **all the way to dryness** leaves **salt** behind (~2–3 per pot) — see [Getting salt](./preservation.md#getting-salt--boil-down-seawater). Only fresh raw or tainted water boils safe to drink.

## Hot-rock boiling — for the waterskin

The **waterskin can't go over a flame**, so you boil it the way people actually did with a hide or bark
container: **heat stones in the fire and drop them in.**

1. **Heat a stone.** Hold a **rock** and **sneak + right-click a lit campfire** — it comes out a glowing
   **Hot Rock** (this costs the fire a little fuel). A hot rock **cools as you carry it** (its bar is the
   heat left) and turns back into a plain rock after about **2 minutes**.
2. **Drop it in.** Hold the **waterskin in your off hand** and the **hot rock in your main hand**, then
   **sneak + right-click**. The stone hisses into the water, brings it to a boil, and the whole skin turns
   **clean** (and the skin is sterilised). The stone gives up its heat and cools to a plain rock.

The stone must still hold most of its heat to work — a **nearly-cold one won't boil** and asks to be
reheated. Like fire-boiling, it **does not desalinate** seawater. (A metal or clay pot ignores all this —
it just goes straight over the fire.)

> One good stone boils a skinful in a go; real hot-rock boiling cycles several cooling stones, which this
> smooths over.

## Rain catch — clean water without fire

A **water cauldron** fills from rain (as it does in vanilla) and is a **fuel-free source of clean water**:

- **Right-click it bare-handed** to drink clean straight from it — no sickness; spends a level.
- **Right-click it with a vessel** to fill the vessel **clean** and drain the cauldron.

## Related pages

- [Fire](./fire.md) — lighting and keeping the campfire you boil over.
- [Food & Eating](./food-and-eating.md) — food that dehydrates you, and washing dirty hands by drinking.
- [Body Temperature](./body-temperature.md) — heat makes you sweat out water faster.
