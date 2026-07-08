# ALONE — Implementation Plan
### Turning `proposal.md` (v1.1) into a Fabric modpack for Minecraft 26.2

This is the engineering companion to `proposal.md`. The proposal says *what the game
should feel like*; this document says *what we build, in what order, and how we test it*.

Guiding rule for sequencing: **easy → hard, and build the load-bearing framework before
the systems that lean on it.** We ship the smallest thing that runs, prove the dev loop on
real hardware, then grow one module at a time. Every phase must be independently testable.

---

## 0. Toolchain (researched & pinned — July 2026)

Minecraft moved to **calendar versioning** in 2026. `26.2` = the *second game drop of 2026*
(what the old scheme would have called ~1.21.11). This changes our stack in three important ways:

| Thing | Value | Why it matters |
|---|---|---|
| Mod loader | **Fabric** | Lightweight, mixin-based; best fit for a deep-systems overhaul (proposal §14). |
| Java | **JDK 25** | 26.1+ moved the target off Java 21. The flake pins this. |
| Mappings | **None — unobfuscated** | Since 1.21.10 Minecraft ships *unobfuscated with parameter names*; Fabric dropped Yarn **and** Mojang-mapping remap. There is **no `mappings` line** — class/method names are the real ones (`net.minecraft.world.level.block.Block`, `state.is(...)`). |
| Loom (Gradle plugin) | **`net.fabricmc.fabric-loom` 1.17-SNAPSHOT** | The **non-remapping** Loom plugin for 26.1+. The old `fabric-loom` id is the remapping variant for obfuscated ≤1.21.11 — using it fails with *"Failed to find official mojang mappings for 26.2."* Deps use plain `implementation`, not `modImplementation`. |
| Gradle | **9.5.1** | Required by Loom 1.17. Pinned via the wrapper. |
| Fabric Loader | **0.19.3** | Runtime. New enum-extension API (0.19.0+). |
| Fabric API | **0.152.1+26.2** | Tag-removal API, client-command API, events. |

**Breaking changes in 26.2 to keep in mind** (from the Fabric 26.2 notes):
- Rendering moved toward **Vulkan/Blaze3D** — avoid raw OpenGL; go through Blaze3D. (The flake ships `vulkan-loader`.)
- **Block IDs and item IDs are stored separately now** — registration code must separate IDs from `Block`/`Item` instances (no more `valueLookupBuilder`).
- Several screen methods moved from `Minecraft` to `Minecraft.getInstance().gui`.

**Ecosystem caveat:** 26.2 is brand new. Do **not** assume Serene Seasons / Tough As Nails /
finite-fluid / falling-tree mods are ported yet. Anything on our critical path we build
custom (the proposal already flags most of these as "genuinely custom work" in §14). We adopt
a third-party mod only once we confirm a live 26.2 build.

---

## 1. Architecture: one modpack, many `Alone:` mods

Per proposal §14: a Fabric modpack built around a custom **core**, split into feature mods
"where it makes sense." Separation criteria: a system becomes its own mod when it (a) owns a
distinct slice of gameplay, (b) can be developed/tested in relative isolation, and (c) could
in principle be toggled off. Shared plumbing lives in **core** so modules don't depend on each other sideways.

### 1.1 The module roster

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

### 1.2 Dependency shape

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

### 1.3 Repo layout (Gradle multi-project monorepo)

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

---

## 2. Phased rollout

Mapped to the proposal's own Tier 1→3 (§14), but **re-sequenced by build difficulty** so we
always ship the framework before the systems that need it. Each phase is playtestable.

### Phase 0 — Pipeline *(this step — done)*
Prove the toolchain end-to-end on your NixOS machine before writing any real systems.
- `flake.nix` dev shell (JDK 25 + graphics libs + Prism).
- `Alone: Core` skeleton that loads.
- **First rule: no punching trees** (§8.1) — bare hands can't break logs. One event, zero
  persistent state, instantly visible in-game. This is the smallest change that exercises the
  *entire* pipeline: nix → gradle → Loom → Fabric → in-world behavior.
- **Test:** `./gradlew :core:runClient`, enter a survival world, punch a log → nothing; hold an
  axe → it breaks.

### Phase 1 — "Grounded" (Tier 1 — cheap, event/datapack-only, no HUD state)
The world feels different immediately, with no new framework.
- Raw-food sickness risk (§4.2) — food component + effect, mostly datapack + a hook.
- Timed, stationary crafting (§8.2) — craft delay + "rooted in place."
- Expand no-punching-trees into the day-one gate (need a knapped edge, not just any tool).
- *(Water currents deferred to Phase 3 — needs the fluid framework.)*

### Phase 2 — "The Body" (the hard core; built early because everything hooks it)
- **Core:** player-data attachment framework + the **accelerated-tick engine** (shared by work
  sessions §8.3 and sleep-as-time-skip §1.4, incl. event interruption).
- **Alone: Body:** the four meters + **condition panel replacing hearts** (§1.5) + corner HUD.
- **Test:** meters drain with activity; a bandaged cut stops bleeding; death has a *cause*.

### Phase 3 — "Fire & Water"
- **Alone: Fire:** friction fire, ember carrying, furnace/kiln tiers, ash economy (§3).
- **Alone: Water:** vessel ladder, contamination states, hot-rock boiling, purification; finite/flowing water (§2). Ties into Body's thirst/temperature.

### Phase 4 — "The Table"
- **Alone: Table:** nutrition groups + food fatigue, freshness/spoilage, preservation (smoke/salt/dry), **scent** as a value on inventory/containers (§4). Feeds Wild in Phase 5.

### Phase 5 — "The Wild" (heaviest AI work)
- **Alone: Wild:** scent-driven predator AI (bears §4.3), deer/rabbit populations w/ seasonal breeding & local overhunting, hunting/tracking/persistence (§7.3), butchering, **structure-scoped spawn override** (§7.1), insects.

### Phase 6 — "Craft & Ground"
- **Alone: Craft:** knapping, timed crafting maturity, tool/material ladder, **masterwork replaces enchanting**, sharpening, **combat-as-conditions**, skill-by-doing (§8).
- **Alone: Ground:** weight tiers + movement penalties, stick-block family w/ degradation, shelter quality, tree felling + age states, gravity/support (§5).

### Phase 7 — "World"
- **Alone: World:** four seasons, weather-with-teeth, biome strategies, **navigation** (no coordinates, craftable compass, hand-drawn maps, celestial direction) (§10).

### Phase 8 — "Road"
- **Alone: Road:** the transport tech tree — backpack → wheelbarrow → travois → rafts/boats → pack animals → horse+cart (emergent roads) → rails (§6).

### Phase 9 — "Relics"
- **Alone: Relics:** structure loot & relics, rationalized mob drops, redstone gating behind engineering plans, villager barter + reputation (§12, §7.4).

### Phase 10 — "The Summit"
- Endgame integration (§13): Nether-as-death-zone prep, obsidian logistics, the Dragon as a
  multi-season expedition. Mostly config/datapack tuning *across* the systems built above — the
  injury system does the balancing, so little new code.

---

## 3. Dev & test workflow

**Two loops, two tools:**

1. **Iterating on a mod (fast loop):** `nix develop` → `./gradlew :core:runClient`. Loom
   launches the actual game with the mod hot-loaded — **no launcher involved.** This is where
   90% of development happens.
2. **Playtesting the assembled pack:** build all jars (`./gradlew build`), drop them into a
   **Prism Launcher** Fabric instance (Prism is in the flake). Lunar is a closed PvP-oriented
   client and does not run Fabric modpacks — Prism is the right tool for actually playing the
   pack; use Lunar only if we later ship an optimized-vanilla-adjacent variant.

**First-time setup:**
```bash
cd alone
nix develop                              # JDK 25 + gradle + graphics libs + prism
gradle wrapper --gradle-version 9.5.1    # generate ./gradlew once
./gradlew :core:runClient                # launch the dev client
```

See `README.md` for the quickstart.

---

## 4. Open questions / decisions to revisit

- **Third-party adoption:** re-check each release whether Serene Seasons / Tough As Nails / a
  finite-fluid mod has a live 26.2 build before we build a custom equivalent (Phases 3, 7).
- **Config surface:** proposal §11 wants a slider for nearly every harsh system. Core should
  own one unified config from the start so modules register into it rather than each shipping
  its own file.
- **Where `no-punching-trees` finally lives:** it's seeded in Core now for Phase 0; it moves to
  Alone: Craft in Phase 6 alongside knapping.
- **Multiplayer:** proposal is single-player-flavored but nothing precludes co-op; keep all
  player state server-authoritative (data attachments) so it "just works" on a server.
```
