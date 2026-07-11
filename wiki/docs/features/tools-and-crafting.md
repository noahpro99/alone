---
sidebar_position: 7
title: "Tools & Crafting"
---

Crafting is real work: trees can't be punched, crafting takes time and roots you in place, and metal gear must be forged and tempered by hand. This page covers the progression from bare hands to a full toolkit.

## No punching trees

Bare hands don't fell timber:

- **Bare hands, a chicken, or a block of dirt** do nothing to a log.
- A **sword, pickaxe, or flint knife** (a crude chopper) hacks a log apart **very slowly** (~75 seconds).
- An **axe** fells a log slowly but functionally (~30 seconds).

## Flint tools

Before any metal, flint is knapped into primitive tools:

- **Knapping** is a real action, not a grid recipe: hold **flint** in one hand and a **rock** (a hammerstone) in the other, then **sneak and hold right-click** to strike it. After about ten strikes a sharp **flint shard** flakes off — but a **raw novice shatters** nearly half their flint. That improves with practice (see [Skill by doing](#skill-by-doing) below): a **skilled** knapper wastes far less.
- Flint shards are then crafted (with sticks and plant fiber) into a **flint hatchet** (fells trees), a **flint pick** (mines stone, coal, and **copper ore** — but **not iron ore**, which shatters flint; that needs a copper pick, gating the flint → copper → iron ladder), a **flint knife** (a crude cutting blade that also strips plant fiber, below), a **flint hoe** (to till a field — the stone-age hoe that lets you farm long before any metal, as the Neolithic did), or a **flint spear** (below).

### The spear — reach, from the first day

The spear was humanity's first great weapon — and the **very first spears were plain fire-hardened wood**, older than any stone tip (the ~300,000-year-old Schöningen spears are sharpened wood, nothing more). So in Alone the spear line starts crude and runs the full ladder:

- A **wooden spear** (3 sticks) is the **day-one** weapon — a sharpened pole with real **thrusting reach**, so from your first minutes you can strike game or an attacker **before it reaches you**. It's fragile and weak (wood-tier), and a blunt wooden point **can't cleanly skin a kill** — but it costs nothing but sticks, which is exactly why it came first.
- The **flint spear** (flint shard + plant fiber + 2 sticks) is the upgrade: the same reach, but a **sharp knapped tip** that hits harder, lasts longer, and — being a true blade — **salvages the hide** off a kill.

Both carry the full spear feel — a **charged thrust** and **piercing** — the reach being why you can hit game or an attacker before it closes. Above flint the metal tiers run **copper → iron → steel**, each sharper and tougher. (The **stone, gold, and diamond** spears aren't craftable — they're off the pack's tiers.)

Flint comes from **digging gravel** — you don't get it in a lump when the block finally breaks, you **sift it loose bit by bit as you work through** the gravel: nodules shake out at intervals across the (slow) dig, so even a half-finished dig keeps what you've already turned up. A finished cubic metre yields **a couple on average**, sometimes none, sometimes a few. The break itself just turns up the odd **loose rock** (rocks are also foraged off the ground).

## Skill by doing

You get better at a craft by **doing it**, not by spending experience — there's no XP bar and nothing to
level up on a menu. Every attempt teaches your hands a little, on a **learning curve** that's fast at first
and then a long plateau toward mastery. It shows up only as the occasional note that you've stepped up a
tier: **Novice → Apprentice → Skilled → Expert → Master**. Your skills **stay with you through death** —
you don't forget a trade because you drowned.

All six crafts respond to skill:

- **Flintworking** — knapping succeeds ~**55%** of the time as a raw novice, climbing to ~**92%** at
  mastery (it was a flat ~65%). You learn from every strike, whether it flakes or shatters.
- **Firecraft** — a practised firemaker coaxes a friction fire alight **sooner** (the catch chance scales
  up to ~**1.3×** at mastery, and a novice is a touch slower). Learned each fire you light.
- **Mining** — a practised miner **quarries faster**: dig speed on stone and ore rises up to ~**1.5×** at
  mastery. Learned by breaking stone and ore.
- **Smithing** — a practised smith **draws better steel**: the random quality of a forged piece is nudged
  toward masterwork the more skilled you are. Learned each piece you forge.
- **Tracking** — a practised tracker hunts better both ways: [persistence hunting](./hunting.md#persistence-hunting--run-it-down) chase-fatigue builds up to ~1.6× as fast (quarry tires in fewer seconds), and wounded game leaves a **longer, more followable [blood trail](./hunting.md#blood-trails--track-a-wounded-animal)** (up to ~1.5× at mastery). Learned by taking wild game.
- **Pottery** — a practised potter **fires ware faster**: the kiln's firing time for a batch drops up to ~**35%** at mastery. Learned by loading ware into a kiln.

Skills otherwise stay quiet — you only hear about them when you step up a tier — so to check your progress, run **`/skills`**: it lists all six with their tier and how far along you are.

## Plant fiber and string

String does not require spiders:

- **Stripping plant fiber** is a timed pull (hold right-click on grass, ferns, vines, or dead bushes for a couple of seconds). Bare hands yield **1–2 strands** (a second on about half the strips); a **cutting blade** (sword, axe, hoe, or flint knife) strips **2–3 clean lengths** at once.
- **Twisting 3 fiber makes 1 string.**

String feeds rope, bandages, bows, and more.

## Timed, stationary crafting

A craft is not instant. Once a valid recipe sits in the grid, it must be **worked at** before the result can be taken:

- A **green progress bar** fills along the result slot (with a percentage on the action bar). The result cannot be picked up until it is done.
- Times scale with the effort involved:

| Craft type | Time |
| --- | --- |
| Simple items (sticks, torch) | ~2s |
| Planks & string (riven / twisted by hand) | ~12s |
| Food | ~5s |
| Tools & weapons | ~15s |
| Working an iron bloom into an ingot | ~30s |
| Stations (chest, furnace) | ~60s |
| Armor | ~2 min |

- Crafting is **resumable** — progress is kept per item and survives closing the screen or switching recipes and back.
- **Taking a result also costs stamina.**

## Forge & temper — metal gear

Metal tools and armor aren't finished in the crafting grid. A grid-crafted iron or steel piece comes out a **brittle unforged blank** (barely usable) until it is worked hot:

1. **Heat it** — holding the blank **by a lit forge** (blast furnace, furnace, smoker, campfire, or lava) makes it glow hotter. Stepping away cools it. It can only be worked while hot.
2. **Hammer it** — with a **smithing hammer** in the pack, **right-clicking an anvil** lands a blow. A piece takes **many blows across several heats** — a dozen for a tool, up to twenty for a chestplate.
3. **Quality** — when it finishes it rolls a **random quality**, from **crude → masterwork**, which sets its durability.
4. **Re-temper** — reheating and rehammering a finished piece **rerolls its quality**. Each rework **lowers the ceiling** on how good it can get, and also **fully repairs** the piece.

The forge state and quality grade show in the item's tooltip; heat and blows show on the action bar during work.

## Whetstone — maintaining an edge

A whetstone hones tools:

- A **whetstone** is crafted from two smooth stone and a stick.
- Holding a worn tool or weapon, keeping the **whetstone in the off hand**, and **sneak + right-click** re-hones the edge.
- Each pass **restores ~25% durability**, wears the whetstone a little, and has a short cooldown.
- Armor has no edge and can't be honed — only a forge **re-temper** repairs armor.

Honing restores durability; only a forge re-temper changes a blade's underlying quality.

## Steel — the top tier

Steel replaces diamond as the best gear:

- **Blasting an iron ingot** (in a blast furnace) makes a **steel ingot**.
- **Steel tools** mine at diamond level with high durability, and **steel armor** sits above iron.
- The set includes a **steel spear** (1 steel ingot + 2 sticks) — the top-tier reach weapon, so the spear runs the full ladder alongside the sword and axe.

## Diamond gear is abolished

Diamond and netherite tools and armor **can no longer be crafted** — diamond would shatter as gear, so it is simply not craftable. **Diamonds remain as items** (for blocks, trade, and other uses); they are no longer the endgame material. **Steel is the top tier instead.**

## Related pages

- [Fire](./fire.md) — the campfire and forge heat you need for smithing.
- [Water, Thirst & Vessels](./water-and-drinking.md) — crafting the waterskin and iron pot.
