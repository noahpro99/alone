---
sidebar_position: 1
title: "Dev Quickstart"
---

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
