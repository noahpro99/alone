# Alone — Plan for the Remaining Content

26.2 registration: `ResourceKey<Item>` -> `new Item(new Item.Properties().setId(key))` ->
`Registry.register(BuiltInRegistries.ITEM, key, item)`. Custom items live in core (`AloneItems`).
Textures/models are placeholders (missing-texture render) until art is added. Each item below is
build + boot verified (registers, recipes parse, tags merge).

## DONE (this session)

- [x] **Seasons (§10)** — `Seasons`: spring/summer/autumn/winter from world day (7-day seasons);
  winter/summer shift ambient temperature. Feeds `SurvivalMeters.ambientTemperature`.
- [x] **Crafting is exertion (§8.2)** — taking a crafted result costs stamina (`ResultSlotMixin`).
- [x] **Backpack (§6)** — carrying one raises your volume cap ~1.5 m³ (`Carry.volumeLimit(player)`,
  read by the volume mixins + HUD). Craftable (leather + string).
- [x] **Bedroll (§5.2)** — right-click to bed down for a far better rest than the ground (night + dry).
  Craftable (wool + leather). Shares `Sleeping.tryRest`.
- [x] **Waterskin (§2)** — right-click water to fill (3 sips, `WATER_CHARGES` data component);
  right-click to drink (restores thirst, ~15% sickness from untreated water). Craftable (leather + string).
- [x] **Primitive flint tools (§8.1)** — flint hatchet (fells trees; added to `#minecraft:axes`), flint
  pick, flint knife (crude chopper); custom FLINT tool tier. All craftable (flint + stick). `AloneItems`.

Obtain via crafting or `/give alone:waterskin` (etc.). No creative tab yet — the Fabric item-group
module wasn't on the classpath; add a tab + real textures/models later.

## Still to do — large systems (need entities / worldgen / invasive hooks; do WITH testing)

- **Structure-scoped hostile spawns (§7.1)** — hostiles only near loot structures. Hook: mixin
  `Monster.checkMonsterSpawnRules` (or `NaturalSpawner`) to require structure proximity via
  `ServerLevel.structureManager()`. High blast radius (get it wrong -> no mobs or mobs everywhere).
- **Condition panel replaces hearts (§1.5)** — DONE: vanilla hearts hidden
  (`HudElementRegistry.removeElement(HEALTH_BAR)`); HUD shows a **vitality bar** + paper-doll injury
  readout; injuries/sickness/infection drain and debilitate; **slow vitality regen** (vanilla fast
  food-regen disabled via `FoodDataRegenMixin`; heals ~1 HP/10s only when fed, hydrated, and unwounded).
  Death already comes from real causes (blood loss, starvation, dehydration, infection, trauma). Any
  further "every hit becomes a specific condition" refinement is optional polish.
- ~~**Torches burn out (§5.6)**~~ DONE — as a fuel *item*, not a block. `alone:torch` (unlit, durability =
  fuel) + `alone:torch_lit`; craft coal+stick → unlit; light via fire source or flint&steel (`TorchItem`);
  lit torch burns 1 fuel/tick (`Torches`) → spent unlit torch. Shader handles the glow. Placeholder models.
- **Transport vehicles (§6)** — wheelbarrow/travois/cart entities that carry heavy loads; models.
- **Structures, loot, relics (§12)** — structure-scoped spawns + relic items + redstone gating; worldgen.
- **Scent-driven wildlife + hunting (§7)** — custom AI goals (mobs to food scent), tracking, blood trails.
- **Farming realism (§4.1)** — ~~seasonal planting windows~~ DONE (crops pause in winter, `CropGrowthMixin`).
  Still open: weeds on farmland (weed block/state).
- ~~**Boiling purifies water (§2/§3)**~~ DONE (`WaterskinItem` + `Campfires`).
