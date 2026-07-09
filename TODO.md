# Alone — Feature Backlog & Progress

Autonomous build session (2026-07-08 night). Every item below compile-checked (`./gradlew build`)
and the whole pack headless boot-checked (`:dev:runServer` → both mods load, no mixin failures).
**Not** in-game tested — the user tests feel later. All numbers are tunable constants.

## Done this session

- [x] **Campfire fuel system** (§3) — friction fire lights a *campfire*; feed it sticks/logs/planks
  (right-click) to burn longer; it burns down and goes out; warmth dwindles as fuel runs low.
  (`Campfires`, `CampfireCookTickMixin`.) Every campfire now consumes fuel.
- [x] **Swim by weight** (§5.1) — carry more than ~35 kg and you can't stay afloat; you sink.
- [x] **Sleep on grass** (§1.4/§5.2) — crouch + right-click bare ground = worst rest, night-only,
  no rain on you, on a cooldown; sheds fatigue + stamina. (`Sleeping`.)
- [x] **Jump & swim cost stamina** (§1.4) — plus the earlier swing-drain (mining/chopping/combat).
- [x] **Eating discipline** (§1.1) — can't eat while sprinting. (`Eating`.)
- [x] **Damage as conditions** (§1.5/§8.6) — claw/bite/arrow → **bleeding** (drains you until it
  clots or is bandaged); bad fall → **sprain** (limping). (`Conditions`.)
- [x] **Bandage treatment** — sneak + right-click with cloth (string/wool/paper) to stop bleeding.
- [x] **Condition HUD markers** — sick (green) / bleeding (red) / sprained (orange) below the temp square.
- [x] **Tiered raw-food risk** (§4.2) — chicken high (80%), red meat medium (45%), fish low (20%);
  three datapack tags. (`alone-food`.)
- [x] **Persistent wetness** (§1.3) — you stay wet & cold after leaving water/rain until you dry;
  a fire dries you ~5x faster.
- [x] **Drinks restore thirst** (§1.2) — milk, honey bottle, water bottle/potion.
- [x] **Hunger gates stamina recovery** (§1.4) — below 6 hunger, stamina recovers poorly.
- [x] **Water quality + boiling** (§2) — waterskin holds raw/clean/tainted water; fill from a source
  is raw (~15% sickness), boil it over a campfire for clean (safe). `WaterskinItem`, `Campfires`.
- [x] **Vessel contamination** (§2) — sipping raw/tainted water dirties the vessel; a dirty vessel
  taints the next fill (tainted ~45% sickness); boiling sterilises it. (`VESSEL_DIRTY` component.)
- [x] **Food spoilage** (§4.2) — perishable foods (datapack tag) rot to rotten flesh after ~1 in-game
  day; eating rotten is already high-risk. `Spoilage` (`SPOILS_AT` component).
- [x] **Surface/hand contamination** (§5.6) — butchering dirties your hands; eating with dirty hands
  adds ~25% sickness to any food; wash by drinking from water. `Hygiene`.
- [x] (earlier) two-tier stamina (short-term + fatigue), cozy/gradual fire warmth, rain chills fast,
  fixed item weights (seeds light).

## Done (later — this continuation; user is playtesting live)

- [x] **Bedroll is a real bed block** — placeable `BedBlock` (`AloneBlocks.BEDROLL` + `BedItem`), sleeps
  / sets spawn / skips night natively; blockstate points both halves at `minecraft:block/white_bed_foot`
  (head +180°) → a flat mat. In `#minecraft:beds`, loot-table drops on `part=head`. Replaced the old
  item-based fake-sleep (deleted the mixin/flag). Confirmed working in-game.
- [x] **Digging progress saved — reworked, two-sided.** Client `MultiPlayerGameModeMixin` stashes crack
  progress on release/switch and restores it in `continueDestroyBlock`; server `ServerPlayerGameModeMixin`
  winds `destroyProgressStart` back by the saved ticks so the break lands at the resumed point. User
  confirms it breaks correctly + crack resume fix applied (pending final visual confirm).
- [x] **Items never despawn** — `ItemEntityDespawnMixin` (`@ModifyConstant` 6000 → MAX). Ground stashes.
- [x] **Crops pause in winter (§4.1)** — `CropGrowthMixin` cancels `CropBlock.randomTick` in winter;
  bonemeal still works. Ties farming to `Seasons`.
- [x] **Crop death + weeds (§4.1)** — per-tick death chance: baseline ~0.2%, winter +2%, +0.6% per adjacent
  weed; crops also sow `short_grass` weeds onto nearby soil over time. (`CropGrowthMixin`.)
- [x] **Torches burn out as a fuel item (§5.6)** — `alone:torch`/`alone:torch_lit` (durability = fuel),
  craft coal+stick, light via fire source or flint&steel, burns down to a spent torch. Shader = glow.
- [x] **Block-break progress saved — CONFIRMED WORKING in-game** — client stash/resume (crack resumes +
  persistent chipped-crack marker on saved blocks) + server `destroyProgressStart` offset. Root cause of
  the long failure: Mixin doesn't run `@Unique` instance-field initialisers → maps were null; lazy-init fixed it.
- [x] **Heavy items don't throw (§5.1)** — drop velocity scales to zero horizontal by ~20 kg. (`LivingEntityDropMixin`.)
- [x] **Can't throw block items (§5.1)** — Q-key and GUI drop-outside of any `BlockItem` are refused and handed back; you must place them or store them in a chest. Player throws only (`thrownFromHand`); death drops untouched. (`LivingEntityDropMixin`.)
- [x] **Ghost items fixed** — never-despawn was keeping vanilla's fake `/give` pickup-animation items (setNeverPickUp + age 5999) forever; now exempts fake/never-pickup items. (`ItemEntityDespawnMixin`.)
- [x] **Torches placeable + fuel-conserving** — lit torch plants a torch/wall-torch (`TorchBlockItem`); placed torches burn down and gutter to a spent torch; mining returns the remaining fuel (no free refill). (`Torches`.)
- [x] **Nutrition variety / food fatigue (§1.1)** — foods tagged protein/veg/grain/fruit; eating one group repeatedly raises its fatigue and shrinks usable max hunger (cap down to ~12); variety recovers it. (`Nutrition` + `nutrient_*` tags.)
- [x] **Condition panel MVP (§1.5)** — vanilla hearts hidden (`HudElementRegistry.removeElement(HEALTH_BAR)`); HUD shows a **vitality bar** (top, blood-red → amber → alarm) + the paper-doll injury figure. xvfb-verified in-world. *Depth to add later: death only from conditions (damage → conditions draining vitality), slow multi-day vitality regen.*
- [x] **HUD icons + endurance bar flip** — game icon per bar (heart/feather/golden-carrot/water-bucket/bundle); the old "fatigue" bar flipped to an **endurance reserve** (full = rested, drains as you tire) so it reads like the others.
- [x] **Infection condition (§1.5)** — zombie-type bites have a ~30% chance to fester: fever (weakness + nausea), and a compounding/deep infection turns septic and drains vitality (can kill). Dress it with cloth (sneak + right-click) to stop bleeding and clean the infection back down. Paper-doll arms turn purple when infected. (`Conditions`.)
- [x] **Salt water (§1.2)** — ocean/beach/shore water is salt: drinking it bare-handed *or* from a waterskin **dehydrates** you instead of quenching (rivers/lakes stay fresh). Filling a waterskin from the sea stores it as `SALT`; **boiling it over a campfire desalinates it to clean**. (`Drinking.isSaltWater`, `WaterskinItem`.)
- [x] **Shelter insulation (§5.5)** — being under cover (no sky access) shields you from rain and the cold night sky and pulls ambient temperature ~40% toward comfortable; deep underground stays a stable cave. Building a roof + a fire is now how you beat winter cold. (`SurvivalMeters.ambientTemperature`.)
- [x] **Salt + food preservation (§2/§4.2)** — boiling a **salt** waterskin over a campfire now yields an `alone:salt` item (desalination byproduct). Hold a perishable food, keep salt in the off hand, **sneak + right-click** to salt it → marked `PRESERVED` so spoilage skips it (keeps through winter), costing 1 salt. Ties seawater → salt → winter stores. (`Campfires`, `Spoilage.PRESERVED`, `Preserving`.)
- [x] **Rain-catch clean water (§1.2)** — a **water cauldron** (fills from rain in vanilla) is now a fuel-free clean source: bare-hand right-click to drink clean (no sickness, spends a level); right-click with a waterskin to fill it **CLEAN** and drain the cauldron. (`Drinking`, `WaterskinItem.useOn`.)
- [x] **Slow vitality regen (§1.5)** — vanilla fast food-regen disabled (`FoodDataRegenMixin` no-ops the `heal` calls in `FoodData.tick`, keeps starvation); health now mends **~1 HP/10s only when fed (≥14), hydrated (≥30), and not bleeding/infected/sick** — so wounds must be treated before you recover. Completes the condition panel.
- [x] **Swim-by-weight retune** — sink threshold 22 kg (one full block ≈ 30 kg sinks you); slow
  unstoppable sink (not a plummet); stamina drains faster the more you haul in water.
- [x] **Being submerged chills you** — extra cold-push + faster cooling while in water.
- [x] **Sprint recalibrated** — ~40 s to empty (was 10 s); MC sprint ≈ a hard run, not a dash.
- [x] **Food weighs a real portion (0.35 kg); seeds fixed** — small block-items (seeds/saplings/torches)
  no longer get block-weight, so seeds == bones (0.05 kg).
- [x] **HUD** — fatigue/exertion second bar + paper-doll figure (bleeding/sprain/dirty-hands/sick).
- [x] **Ground rest feedback** — grass/dirt/sand rest now tells you why it fails (time/rain/cooldown).
- [x] **Partial menu placement** — `SlotVolumeMixin` now allows a slot if one unit fits and caps the
  inserted count by volume (`safeInsert` via `@ModifyVariable`). So a big stack into a near-full chest
  deposits what fits, leaves the rest. *Watch for: odd counts when dropping stacks into near-full chests.*

## Later phases (need assets/models or large scope)
Custom items/blocks (knapping tools, bedroll, vessels — need textures); timed crafting (§8.2);
torches burning out (§5.6); seasons (§10); transport tree (§6); structures/loot/relics (§12);
scent-driven wildlife AI + hunting (§7); the full condition-panel-replaces-health UI (§1.5).
