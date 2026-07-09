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
- [x] **Steel tier (§8.5)** — the new top tier replacing diamond. `alone:steel_ingot` from **blasting an iron ingot** (blast furnace); full set of steel tools (sword/pickaxe/axe/shovel/hoe, diamond-level mining, durability 900) and armor (helmet/chest/legs/boots, above iron, toughness 1.0). Custom `ToolMaterial`/`ArmorMaterial`; armor reuses the iron worn-look for now (placeholder art). All craftable. (`AloneItems`.)
- [x] **Diamond gear abolished (§8.5)** — all 9 diamond tool/armor crafting recipes removed via Fabric `load_conditions` (false) overrides in `data/minecraft/recipe/` (barrier-only body as a fail-safe). Diamonds remain as items (blocks/trade/future cutting-tips); netherite gear falls away with the diamond base. *Steel top tier is the intended replacement — not yet added.*
- [x] **Armor mitigates injuries + fractures (§8.6)** — bleeding is now a damage-scaled chance (harder hit → likelier) that **armor blunts** (full armor ≈ 20% of the chance); a **heavy blow (≥8 dmg) can fracture** (sprain), also armor-reduced. Gives armor a role in the condition system. (`Conditions` AFTER_DAMAGE.)
- [x] **Splint treats sprains (§1.5 medicine)** — craftable `alone:splint` (2 sticks + string); sneak + right-click while sprained to bind the joint and cut recovery time to ~¼. Completes the injury→treatment matrix (bleeding/infection → cloth, sprain → splint). (`Conditions`, `AloneItems.SPLINT`.)
- [x] **Death is a setback, not a reset (§11)** — after a death-respawn you wake "carried home, days later": 40% health, food 6, thirst 25, stamina 20, high fatigue, and a lingering sprain — so recovering is the real death penalty. (ServerPlayerEvents.AFTER_RESPAWN; skips End returns.)
- [x] **Sleep quality by warmth (§1.4/§5.5)** — waking recovery now scales with how comfortable you were: comfortable = full night; cold/hot = 60%; freezing/roasting = 30% (with a "slept poorly" message). Makes fire/shelter/hot-meal warmth matter for rest, not just survival. (`SurvivalMeters.restQuality`.)
- [x] **Hot meals warm you (§1.3)** — eating a cooked meal/stew (datapack `hot_meals` tag) bumps body temperature toward comfort (+25, capped so it can't cause heatstroke); it drifts back over ~40s. Another tool against winter cold alongside fire + shelter. (`SurvivalMeters.warm`.)
- [x] **Shelter insulation (§5.5)** — being under cover (no sky access) shields you from rain and the cold night sky and pulls ambient temperature ~40% toward comfortable; deep underground stays a stable cave. Building a roof + a fire is now how you beat winter cold. (`SurvivalMeters.ambientTemperature`.)
- [x] **Food affects thirst (§1.1/§1.2)** — eating **salt-preserved** food dehydrates (−12), **dry** foods (bread/cookie/dried kelp/rotten flesh) cost a little (−6), and **juicy** foods (fruit/melon/soups) give water back (+6). Makes salted winter stores a real trade-off — stockpile water too. (`ItemStackFinishUsingMixin`, `dry_foods`/`juicy_foods` tags.)
- [x] **Thermoregulation feeds the meters (§1.2/§1.3/§1.4)** — body temperature is no longer isolated; it now drives the other two meters. **Heat → sweat**: thirst drains proportional to how hot your *body* runs (desert, fire, heavy clothing, hard exertion all cost water — replaces the crude "hot biome" flag). **Cold → shiver + burn**: a cold body burns extra food to hold its heat (scaled by how cold), and below −30 stamina recovers at half rate (stiff, shivering). Ties fire/shelter/clothing/season into hunger and water, not just the freeze/heatstroke extremes. (`SurvivalMeters.tick`.)
- [x] **Clothing insulation (§1.3/§5.5)** — what you wear now shifts the temperature loop. **Leather/hide** armour (helmet/chest/legs/boots + turtle shell) insulates (~9 each): in the cold it blunts the chill back toward comfort; in the **heat** those unsheddable layers trap warmth and make you overheat faster. **Metal** plates barely insulate and, under an **open sky in the heat**, bake you (+4 each). Wet clothing insulates nothing (submerged skips it). Gives leather a real survival role beyond injury-mitigation — dress for the season. (`SurvivalMeters.clothingShift`.)
- [x] **Salt + food preservation (§2/§4.2)** — boiling a **salt** waterskin over a campfire now yields an `alone:salt` item (desalination byproduct). Hold a perishable food, keep salt in the off hand, **sneak + right-click** to salt it → marked `PRESERVED` so spoilage skips it (keeps through winter), costing 1 salt. Ties seawater → salt → winter stores. (`Campfires`, `Spoilage.PRESERVED`, `Preserving`.)
- [x] **Iron pot — bigger water vessel (§2 vessel ladder)** — generalized `WaterskinItem` to a per-vessel capacity; `alone:iron_pot` (3 iron) holds **6 sips** vs the waterskin's 3, and boils/fills the same way. A real travel/desert upgrade. (`AloneItems`, `WaterskinItem`.)
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

## Playtest fixes (this continuation)
- [x] **Sprain lasts longer** — a fall sprain was only ~30s; now ~2 min of limping (splint still cuts it to ~30s). Injuries should be a real setback. (`Conditions.SPRAIN_TICKS`.)
- [x] **Worn armour doesn't eat your volume** — equipped armour rides on your body, so it no longer counts toward the hard volume cap (you can wear a full set *and* still pick up a block). Its weight still slows you. (`Carry.isWornArmor`.)
- [x] **Can't throw blocks via the inventory either** — dragging a block-item out of the inventory window used to throw it (that path doesn't set `thrownFromHand`); now any *living* player's block-drop is handed back (death drops still fall). Creative exempt. (`LivingEntityDropMixin`.)
- [x] **Torch break — single alone torch, no vanilla one** — breaking a torch is now handled end-to-end via `BEFORE` (cancel vanilla break + drop, pop one `alone:torch_lit` with its remaining fuel). Fixes the double-drop; converts any world torch to an alone torch. **Vanilla torch recipe removed** (`data/minecraft/recipe/torch.json` false-condition). (`Torches`.)
- [x] **Barely-scratched digs heal** — a dig under 10% progress no longer saves a permanent chip; it resets to a pristine block (client + server both gate on 10%). (`MultiPlayerGameModeMixin`, `ServerPlayerGameModeMixin`.)
- [x] **Drinking takes time + a water gauge** — sipping from a waterskin/iron pot is now a ~1.6s timed drink (DRINK animation), not instant; and the vessel shows a durability-style **water bar** tinted by quality (blue clean / murky raw / sickly tainted / teal salt) so you can read how much water is left and what kind. (`WaterskinItem`.)

- [x] **Leaves feel like foliage (§5.4)** — breaking leaves by **hand** yields snapped **sticks + leaf litter** (not a tidy block); an **axe or hoe** shears the whole **leaf block** free. Either way it's **slow, tugging work** (hand ~12× slower, blade ~3×). Vanilla saplings/apples still drop, so tree farming survives. (`Leaves`, `PlayerDestroySpeedMixin`.)
- [x] **Free-climbing (§5.4/§5.7)** — new ways up/down: **leaves work like scaffolding** — stand on top of a canopy, climb up through it, **crouch to lower yourself down** through it. Climbing a tree is **slow (~0.4× ladder) and tiring** (~12 stamina/block; a ~6 m tree ≈ most of your wind). You can also **scrabble up OR down any flat full-block wall** (no height limit) at **slow rock-climbing pace** (ascent ~0.3×; descent stays ladder-smooth) — **brutal** at ~18 stamina/block either direction. **Run out of stamina and you fall**; **haul >~25 kg and you can't climb at all**. Stamina is charged server-side off real Y-travel (movement is client-driven, so collision/velocity are unreliable there) and synced to the client so the fall-when-spent gate works. (`Climbing`, `LivingEntityClimbMixin`, `LeavesCollisionMixin`.)

- [x] **Forge & temper — timed metal crafting (§8.2)** — metal gear is no longer finished in the grid. A grid-crafted iron/steel tool or armour piece comes out an **unforged blank** (brittle, ~20 durability) until you work it hot: **hold it by a lit forge** (blast furnace/furnace/smoker/campfire/lava) to **heat** it, then **right-click an anvil** (with a craftable `alone:smithing_hammer` in your pack) to **hammer** — a piece takes many blows across several heats (12 for tools, up to 20 for a chestplate). On completion it gets a **random quality** (crude→masterwork) that sets its durability (0.6×–1.5×). **Re-temper**: reheat + rehammer a finished piece to **reroll quality**, but each rework lowers the ceiling (×0.85) — and repairs it. Live durability via `ItemStackForgeDurabilityMixin`; forge state/grade shown in the tooltip; heat/blows on the action bar. (`Forging`, `forgeable` tag.)

- [x] **Rope (§5.7)** — a craftable coil (`alone:rope`, 3 string → 3) you **throw down a cliff face**: aim at a block face near the top and use it, and the rope **unrolls straight down** the open air (one length per block, up to 32) as climbable `alone:rope` blocks. Rope is in `minecraft:climbable` with **no collision**, so vanilla treats it exactly like a ladder — **free, full-speed, safe up/down climbing**, the civilized counterpoint to brutal free-climbing. Break it to recover the coil. (`RopeItem`, `AloneBlocks.ROPE`.)

- [x] **Plant fiber → string (§8.1)** — string no longer requires killing spiders. Tearing up **grass/ferns/vines/dead bush** drops **`alone:plant_fiber`** (bare hands ~50% for one strand; a **cutting blade** — sword/axe/hoe/flint knife — strips 1–2 clean lengths). **Twist 3 fiber → 1 string** (shapeless). Spiders still drop string; this just frees you from them and feeds the whole string→rope/bandage/bow economy. (`Fibers`.)

- [x] **Friction fire needs tinder (§3.1)** — drilling with a stick only makes an ember now; it won't catch without **tinder** (a bundle of `alone:plant_fiber`, or dry `leaf_litter`), which is **consumed when the fire lights**. Without tinder you just smoke, with a throttled hint. Ties the new fiber/leaf-litter into fire-starting — you gather tinder before you can make fire. (`FireStarting`.)

## Later phases (need assets/models or large scope)
Custom items/blocks (knapping tools, bedroll, vessels — need textures); timed crafting (§8.2);
torches burning out (§5.6); seasons (§10); transport tree (§6); structures/loot/relics (§12);
scent-driven wildlife AI + hunting (§7); the full condition-panel-replaces-health UI (§1.5).
