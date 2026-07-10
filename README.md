# Alone

A survival-realism overhaul modpack for **Minecraft 26.2** (Fabric). Design in
[`proposal.md`](proposal.md); engineering roadmap in
[`implementation-plan.md`](implementation-plan.md).

Vanilla Minecraft is a game about accumulation; Alone is a game about *maintenance* — water,
food, warmth, shelter, and a long-horizon goal (the Dragon) that takes seasons to attempt.

## Status

- **Phase 0 — Pipeline (done).** `Alone: Core` with the first grounding rule: **you fell a
  tree with a tool.** An axe chops normally; a crude blade (sword/pickaxe) barely works, very
  slowly; anything else (chicken, dirt, bare hands) can't make progress at all. Tool tiers are
  datapack tags (`#minecraft:axes` + `alone:crude_choppers`).
- **Survival depth (added incrementally).** Two-tier stamina (short-term + fatigue/soreness that
  lowers your ceiling; mining/chopping/combat/jumping/swimming all exert; crouch or sleep to
  recover). **Fire tech** — friction fire (crouch + hold right-click a stick on the ground) lights a
  **campfire** you feed with sticks/logs and that burns down. **Injuries as conditions** — bleeding
  (from claws/arrows; bandage with cloth) and sprains (from falls), shown on the HUD. **Sleep on the
  ground** (night + dry only, worst quality). Eating discipline (no eating on the run), swim-by-weight
  (too heavy = you sink), tiered raw-food risk. Full detail + backlog in [`TODO.md`](TODO.md).
- **Core survival systems (Phase 2, in progress).** The spine everything hooks into lives in
  `alone-core`, server-authoritative via data attachments, with a **corner HUD** (colored bars,
  synced server→client): **stamina** (green — drains sprinting, recovers resting), **thirst**
  (blue — only drains; drink water to refill), **body temperature** (square: cold→blue,
  comfortable→green, hot→red, with darker shades at the dangerous extremes), and a **sickness**
  marker. Temperature is driven by biome + weather (rain/storms chill; wetness makes cold worse)
  and *bites* — cold pushes you toward **hypothermia** (slow, then freezing damage), heat toward
  **heatstroke**. A **trend arrow** next to the square shows whether you're warming (orange ↑) or
  cooling (blue ↓). Plus the **condition system** (`Conditions`), **realistic reach** (~2.75 blocks),
  and **honest break times** — stone/ore take tens of seconds and cost stamina; dirt stays fast.
- **Phase 1 — Grounded (in progress).** `Alone: Table` (`alone-food`): eating raw meat/fish
  can give you **foodborne illness** — a lingering sickness condition (weak, unproductive,
  feverish for minutes; survives relog), the first use of Core's persistent condition framework
  (`Conditions`, the seed of the Phase 2 condition panel). Which foods are risky is a **bundled
  datapack tag** (`.../data/alone/tags/item/dangerous_raw.json`), hot-reloadable with `/reload`.

## Toolchain

Minecraft 26.2 · Fabric Loader 0.19.3 · Fabric API 0.152.1+26.2 · **JDK 25** ·
non-remapping Loom (`net.fabricmc.fabric-loom` 1.17) · Gradle 9.5.1 · **no mappings** —
26.1+ ships unobfuscated (Yarn *and* Mojang-mapping remap retired).

## Dev quickstart (NixOS)

```bash
nix develop                              # JDK 25 + gradle + graphics libs + Prism Launcher
gradle wrapper --gradle-version 9.5.1    # one time: generate ./gradlew
./gradlew :core:runClient                # launch the dev client with the mod loaded
```

**Load the whole pack in one client** (recommended — the `dev` project aggregates every module):

```bash
./gradlew :dev:runClient      # every Alone module, hot-loaded, no launcher
```

Test in that one **survival** world:
- **HUD:** now in the **top-left corner** — stacked bars for **stamina** (green), **thirst**
  (blue), and carried **volume** (grey→red), with a **temperature** square + warming/cooling
  arrow to their right and a **sickness** marker below it.
- **Stamina:** sprint → the green bar drains (~10s to empty); near-empty you're Slowed + Mining-
  Fatigued and can't sustain a sprint. Stop and rest → the bar refills over ~25s.
- **Thirst:** the blue bar drains over time (faster sprinting/in heat). **Right-click water
  bare-handed** to drink and refill it — but untreated water has a ~15% chance to make you sick
  (boiling/vessels come with §2). Low thirst → Weakness, then Slowness/Fatigue at empty.
- **Temperature:** linger in snow/taiga (worse if raining, or you're wet) → blue → dark blue,
  you slow down, then take freezing damage. A desert overheats you (red → burning). **Warm up by
  a heat source** — stand near lava, a lit campfire, fire, or even torches and the square climbs
  back toward green (lava is strong enough to overheat you). Or just retreat to a temperate biome.
  (Clothing/shelter counters and season come later — §3/§10.)
- **Reach:** you can no longer break/place blocks ~4.5 blocks away — it's ~2.75 now (a constant
  in `SurvivalMeters`, easy to tune). Creative keeps vanilla reach for building.
- **Break times:** mining stone with an iron pick now takes ~20s (deepslate/ore longer, capped
  ~60s), and each block costs stamina — quarrying is real work. **Dirt/sand/gravel take a few
  seconds too** (digging a cubic metre of earth is real work, even with a shovel — shovel faster
  than bare hands). Creative instabreak is unaffected.
- **Weight & volume (§5.1):** every item has a weight and a volume (a full block = **1 m³**, from
  its real shape; small items tiny). **Volume is a hard 1 m³ cap on your person** — so you can
  carry **one full block** OR a lot of small things; when full, extra items simply aren't picked
  up (they stay in the world, exactly like vanilla's full inventory — nothing is dropped). **Weight
  is a soft penalty: heavier load → slower movement** — one heavy block makes you trudge. Third HUD
  bar (grey → amber → red) shows how full you are. *One block per trip is punishing on purpose —
  that's what the transport tree (backpack/wheelbarrow/cart, §6) will relieve. **Chests are also
  capped** — a 27-slot chest = 1 m³ (scales: double chest 2 m³, barrel 1, etc.); functional blocks
  (furnaces/crafting) are uncapped. Personal cap + per-item values are tunable in `Carry`. Known
  rough edge: menu placement is all-or-nothing — split a stack to place a partial into a near-full chest.*
- **Tree felling:** bare hand / chicken / dirt on a log → nothing happens. Sword or pickaxe →
  chops *very* slowly (~30s). Axe → slow but functional (~10–15s). Creative is exempt.
- **Raw-food risk → foodborne illness:** eat raw chicken/beef/fish → ~60% of bites make you
  *sick*. A brief acute hit (Nausea + Poison), then a **lingering illness** (~4 min) that keeps
  you Weak + Mining-Fatigued + hungry and **persists through relog** — a real condition, not a
  potion tick. A *cooked* meal is safe. (Ignore the vanilla Hunger raw chicken adds on its own.)
  Tweak `dangerous_raw.json` and `/reload` to see the datapack loop live.

To iterate on a single module in isolation, each also has its own run, e.g.
`./gradlew :food:runClient` — but that loads *only* that module.

- **Fast dev loop:** `./gradlew :dev:runClient` launches the real game with the pack hot —
  no launcher needed.
- **Playtesting the assembled pack:** `./gradlew build`, then load the jars from
  `modules/*/build/libs/` into a Fabric instance in **Prism Launcher** (`prismlauncher`, in
  the dev shell). Lunar Client doesn't run Fabric packs — use Prism for actually playing.

## Building the mrpack

The distributable is an **`.mrpack`** (Modrinth format) — the Alone mods bundled with the curated
external mods (Sodium, Iris, JEI, sounds, animations, …) resolved from Modrinth by URL+hash.

```bash
# 1. build the alone-core / alone-food jars at the current version
nix develop -c ./gradlew build

# 2. build the mrpack (needs curl, jq, zip on PATH; hits the Modrinth API)
nix-shell -p jq zip curl --run "bash scripts/build-mrpack.sh"
```

Output: **`dist/Alone-<version>.mrpack`**. The version comes from `PACK_VER` in
`scripts/build-mrpack.sh`, and the script bundles `modules/{core,food}/build/libs/alone-*-$PACK_VER.jar`,
so step 1 must have produced jars at that same version. **To cut a new version**, bump both first:

```bash
sed -i 's/mod_version=0.3.0/mod_version=0.4.0/' gradle.properties
sed -i 's/PACK_VER="0.3.0"/PACK_VER="0.4.0"/' scripts/build-mrpack.sh
```

Install the resulting `.mrpack` with Prism/Modrinth/ATLauncher (Minecraft 26.2 / Fabric). The mod
jars alone (for a manual Fabric install) are in `modules/*/build/libs/` and need Fabric API 0.152.2+.

## Layout

```
modules/core/     Alone: Core   — foundation + "no punching trees"
modules/food/     Alone: Table  — raw-food risk (mixin + datapack tag)
flake.nix         NixOS dev environment
build.gradle      shared Loom/Fabric config for every module
gradle.properties pinned versions
```

## Adding a module later

`mkdir -p modules/<name>/src/main/{java,resources}`, add a `build.gradle` with its
`archivesName`, a `fabric.mod.json`, then two lines in `settings.gradle`:

```groovy
include '<name>'
project(':<name>').projectDir = file('modules/<name>')
```

> **Note:** if you `git init` this repo, Nix flakes only see git-tracked files — remember to
> `git add` new files or `nix develop` won't pick them up.
