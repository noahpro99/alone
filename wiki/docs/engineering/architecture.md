---
sidebar_position: 2
title: "Architecture"
---

Per proposal §14: a Fabric modpack built around a custom **core**, split into feature mods
"where it makes sense." Separation criteria: a system becomes its own mod when it (a) owns a
distinct slice of gameplay, (b) can be developed/tested in relative isolation, and (c) could
in principle be toggled off. Shared plumbing lives in **core** so modules don't depend on each other sideways.

## 1.1 The module roster

| Mod (id) | Owns | Depends on |
|---|---|---|
| **Alone: Core** (`alone-core`) | Config, networking, registry helpers, **player-data attachments**, the **accelerated-tick engine** (work sessions + sleep, proposal §1.4/§8.3), shared events, HUD framework. *Currently also hosts the first grounding rule.* | — |
| **Alone: Body** (`alone-body`) | Survival meters (hunger/thirst/temperature/stamina/sleep) + the **condition panel that replaces the health bar** (§1). The spine everything hooks into. | Core |
| **Alone: Fire** (`alone-fire`) | Friction fire, ember carrying, furnace/kiln tiers, charcoal clamp, the ash economy (§3). | Core |
| **Alone: Water** (`alone-water`) | Vessel ladder + contamination + hot-rock boiling; finite/flowing water rules (§2). | Core, Fire |
| **Alone: Table** (`alone-food`) | Nutrition groups & food fatigue, spoilage/freshness, preservation, **scent emission** (§4). | Core, Body, Fire |
| **Alone: Wild** (`alone-wild`) | Scent-driven wildlife AI, bears/predators, deer/rabbit populations, hunting/tracking/persistence, butchering, **structure-scoped spawning**, insects (§7). | Core, Table, Body |
| **Alone: Craft** (`alone-craft`) | Knapping, **no punching trees**, timed stationary crafting, tool/material ladder, masterwork quality, sharpening, combat-as-conditions, skill-by-doing (§8). | Core, Body |
| **Alone: Ground** (`alone-build`) | Weight & carry tiers + movement penalties, stick-block family w/ degradation, shelter quality, tree felling + age states, gravity/support, fire spread (§5). | Core, Body |
| **Alone: Road** (`alone-transport`) | Backpack, wheelbarrow, travois, rafts/boats, pack animals, horse+cart w/ emergent roads, rail gating (§6). | Core, Ground |
| **Alone: World** (`alone-world`) | Seasons, weather-with-teeth, biome strategies, navigation (no coords, compass, hand-drawn maps, celestial) (§10). | Core, Body |
| **Alone: Relics** (`alone-loot`) | Structure-scoped loot, relics, rationalized mob drops, redstone gating, villager barter/reputation (§7.4, §12). | Core, Wild |

> **Architecture update (2026-07-08):** the survival *body* — meters, conditions, the
> condition panel, and the shared tick engine — is being built **inside `alone-core`**, not as a
> separate `Alone: Body` mod. Rationale: these systems are the spine that every content mod
> (`food`, `fire`, `water`, `wild`, …) hooks into, and they interlock too tightly to split
> cleanly. Keeping them in core means content mods depend on one thing (core) instead of a web of
> body sub-mods. `Conditions` (sickness) and `SurvivalMeters` (stamina) are the first pieces.

Datapack territory (per §14), layered on top rather than as mods: loot tables, food-poison
odds, break-time multipliers, tag edits.

## 1.2 Dependency shape

```
                         alone-core
        ┌──────────┬─────────┼──────────┬──────────┬─────────┐
   alone-body  alone-fire  alone-craft  alone-ground alone-world (…)
        │          │           │            │
   alone-water ────┘      (combat/tools) (weight/felling)
        │
   alone-food ── alone-wild ── alone-loot ── alone-road
```

Core is the only thing everyone imports. Modules never depend sideways on a peer's *internals* —
only on core's shared API (data attachments, events, the tick engine).

## 1.3 Repo layout (Gradle multi-project monorepo)

```
alone/
├── flake.nix                 # NixOS dev environment (JDK 25, gradle, graphics libs, Prism)
├── settings.gradle           # declares each module as a Gradle subproject
├── build.gradle              # shared Loom/Fabric config applied to every module
├── gradle.properties         # pinned versions (MC 26.2, loader, fabric-api)
├── modules/
│   └── core/                 # ← Alone: Core (the only module today)
│       ├── build.gradle
│       └── src/main/{java,resources}
│   └── body/ …               # future modules drop in here; one `include` line, no root changes
├── proposal.md
└── implementation-plan.md
```

One `nix develop`, one Gradle build, each module compiles to its own jar. Adding a module =
`mkdir modules/<name>`, add one `include` line, done.
