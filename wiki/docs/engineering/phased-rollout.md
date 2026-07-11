---
sidebar_position: 3
title: "Phased Rollout"
---

Mapped to the proposal's own Tier 1→3 (§14), but **re-sequenced by build difficulty** so we
always ship the framework before the systems that need it. Each phase is playtestable.

## Phase 0 — Pipeline *(this step — done)*
Prove the toolchain end-to-end on your NixOS machine before writing any real systems.
- `flake.nix` dev shell (JDK 25 + graphics libs + Prism).
- `Alone: Core` skeleton that loads.
- **First rule: no punching trees** (§8.1) — bare hands can't break logs. One event, zero
  persistent state, instantly visible in-game. This is the smallest change that exercises the
  *entire* pipeline: nix → gradle → Loom → Fabric → in-world behavior.
- **Test:** `./gradlew :core:runClient`, enter a survival world, punch a log → nothing; hold an
  axe → it breaks.

## Phase 1 — "Grounded" (Tier 1 — cheap, event/datapack-only, no HUD state)
The world feels different immediately, with no new framework.
- Raw-food sickness risk (§4.2) — food component + effect, mostly datapack + a hook.
- Timed, stationary crafting (§8.2) — craft delay + "rooted in place."
- Expand no-punching-trees into the day-one gate (need a knapped edge, not just any tool).
- *(Water currents deferred to Phase 3 — needs the fluid framework.)*

## Phase 2 — "The Body" (the hard core; built early because everything hooks it)
- **Core:** player-data attachment framework + the **accelerated-tick engine** (shared by work
  sessions §8.3 and sleep-as-time-skip §1.4, incl. event interruption).
- **Alone: Body:** the four meters + **condition panel replacing hearts** (§1.5) + corner HUD.
- **Test:** meters drain with activity; a bandaged cut stops bleeding; death has a *cause*.

## Phase 3 — "Fire & Water"
- **Alone: Fire:** friction fire, ember carrying, furnace/kiln tiers, ash economy (§3).
- **Alone: Water:** vessel ladder, contamination states, hot-rock boiling, purification; finite/flowing water (§2). Ties into Body's thirst/temperature.

## Phase 4 — "The Table"
- **Alone: Table:** nutrition groups + food fatigue, freshness/spoilage, preservation (smoke/salt/dry), **scent** as a value on inventory/containers (§4). Feeds Wild in Phase 5.

## Phase 5 — "The Wild" (heaviest AI work)
- **Alone: Wild:** scent-driven predator AI (bears §4.3), deer/rabbit populations w/ seasonal breeding & local overhunting, hunting/tracking/persistence (§7.3), butchering, **structure-scoped spawn override** (§7.1), insects.

## Phase 6 — "Craft & Ground"
- **Alone: Craft:** knapping, timed crafting maturity, tool/material ladder, **masterwork replaces enchanting**, sharpening, **combat-as-conditions**, skill-by-doing (§8).
- **Alone: Ground:** weight tiers + movement penalties, stick-block family w/ degradation, shelter quality, tree felling + age states, gravity/support (§5).

## Phase 7 — "World"
- **Alone: World:** four seasons, weather-with-teeth, biome strategies, **navigation** (no coordinates, craftable compass, hand-drawn maps, celestial direction) (§10).

## Phase 8 — "Road"
- **Alone: Road:** the transport tech tree — backpack → wheelbarrow → travois → rafts/boats → pack animals → horse+cart (emergent roads) → rails (§6).

## Phase 9 — "Relics"
- **Alone: Relics:** structure loot & relics, rationalized mob drops, redstone gating behind engineering plans, villager barter + reputation (§12, §7.4).

## Phase 10 — "The Summit"
- Endgame integration (§13): Nether-as-death-zone prep, obsidian logistics, the Dragon as a
  multi-season expedition. Mostly config/datapack tuning *across* the systems built above — the
  injury system does the balancing, so little new code.
