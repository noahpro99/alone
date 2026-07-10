# ALONE: A Survival-Realism Overhaul for Minecraft
### Full Design Proposal — v1.3
*(v1.1 integrates the injury system, water containers, fire & heat technology, hunting/tracking, combat redesign, navigation, masterwork crafting, horse carts, glass gating, and the full system audit.)*

---

## 0. Design Philosophy

**One sentence:** Vanilla Minecraft is a game about accumulation; this is a game about *maintenance*, where every mechanic answers the question "how would a real human do this?"

Three governing principles, in priority order:

1. **Believability over simulation.** We model the *decisions* real survival forces (carry vs. cache, cook vs. risk it, treat the wound vs. push on), not the physics. Anywhere true realism means tedium without a decision, we compress time but keep the relationship intact — and where compression happens, it happens honestly, through the work-session mechanic (§8.3), not by pretending the work was small.
2. **Danger is legible and opt-in.** Nothing ambushes you for no reason. Nature threatens through systems you can see coming. Monsters exist only where the world signals them: ruins with loot. Every death should be traceable to a decision.
3. **Vanilla-shaped.** Same blocks, same biomes, same crafting grid, same Ender Dragon. A vanilla player should recognize everything and be wrong about how all of it behaves. Some vanilla fictions are the game's soul and are deliberately protected (creepers stay; beds still explode in the End).

The reference fantasy is the show *Alone*: dropped into wilderness with almost nothing, living the daily loop of water, food, warmth, and shelter — with a long-horizon goal (the Dragon) that requires seasons of preparation to even attempt.

---

## 1. The Player's Body

Four survival meters — hunger, thirst, temperature, stamina — plus sleep and a **condition panel** that replaces the health bar entirely (§1.5). UI rule: meters are small and corner-mounted, pulsing into prominence only in danger bands. The screen feels like vanilla until your body is complaining.

### 1.1 Hunger
- Drains from *activity*, not time alone. A day of sprinting, chopping, and swimming burns 3–4x an idle day. Cold adds a passive burn (your body heats itself).
- **Nutrition variety:** foods belong to groups — protein, vegetable, grain, fruit/forage. Effective max hunger shrinks after days of a single group ("food fatigue"), recovering with variety. No vitamins spreadsheet; just "you can't live on bread."
- **Eating on the move:** snack-tier food (berries, jerky, hardtack, dried fish) can be eaten while walking or **riding**. Proper meals (stews, cooked cuts) root you in place for a few seconds. Nothing can be eaten while sprinting or in combat.
- **Hunger gates sleep** (§1.4): below half hunger, sleep is restless and recovery is poor; while starving, you cannot stay asleep at all — you wake repeatedly through the night and gain nothing. An empty stomach is a survival emergency partly *because* it steals your rest.

### 1.2 Thirst
- Drains faster than hunger; faster still in heat or exertion. Water weighs 1 kg/liter, so carried water is a real logistics cost (§4.1).
- Source quality: rain catch/springs (clean) → flowing rivers (usually fine, small illness chance) → still ponds/swamps (dangerous raw) → **ocean = salt water** — drinking it (bare-handed *or* from a vessel) **dehydrates** you rather than quenching. **Boiling purifies fresh water and desalinates seawater** — it's the one fix for anything, so the sea is only water *after* you've boiled it.
- See §2 for containers — v1.1 removes the abstract "water bottle" and replaces it with a real vessel technology ladder.

### 1.3 Temperature
- Body temperature driven by biome, season, time of day, weather, immersion, clothing, and heat sources.
- **Cold band:** slowdown → stamina penalties → *hypothermia* (a condition, §1.5, ultimately fatal). Countered by layered wool/leather clothing, fire, and Shelter Quality (§5.5). **Wetness multiplies cold** — falling in a winter river is an immediate emergency; a fire dries you.
- **Heat band:** thirst drain → stamina penalties → *heatstroke*. Countered by shade, night travel, light clothing, water. The Nether maxes this axis permanently (§13).

### 1.4 Stamina, exertion debt, and sleep
- Stamina drains from sprinting, jumping, swimming, chopping, mining, and fighting; restores from rest, food, and fully from sleep. Empty stamina: no sprint, half mining speed, weak attacks, short breath-hold.
- **Exertion debt ("soreness"):** burn beyond ~150% of a normal day's stamina and tomorrow's stamina cap is reduced 10–25%, cleared by a full night's sleep plus a lighter day. Deliberately capped at one level deep — no soreness spirals; it's a pacing decision, not a punishment.
- **Sleep is a need, not a skip button.** Each skipped night stacks fatigue (stamina cap −25%, then slowness, then screen-blur micro-sleeps). Sleeping requires warmth and dryness: a bedroll (wool + hide) is the minimum, and Shelter Quality (§5.5) sets recovery.
- **Sleep as honest time-skip:** sleeping accelerates game ticks rather than teleporting to dawn. Body needs still drain per tick, weather rolls on, fires burn down, smokers finish. If a scent event, loud noise, or storm damage occurs nearby, **you wake "a second later" in real time — but hours later in game time**: it's 3 a.m., the hearth is embers, and the bear is already at the drying rack. Maximum tension, no new mechanics.

### 1.5 The Condition Panel — injuries replace health
There is **no hearts bar**. The body is represented by:

- **Vitality/blood level** — drops from bleeding and severe conditions; restores over *days* of eating, drinking, and rest.
- **Discrete injuries and conditions**, each with a cause, a treatment, and an untreated trajectory:

| Condition | Cause | Treatment | Untreated |
|---|---|---|---|
| Bleeding | cuts, arrows, claws | bandage (cloth, boiled) | drains vitality → death |
| Fracture / sprain | falls, crush | splint (sticks + cloth), rest | limping for a week+, worsens with use |
| Infection | dirty wounds, zombie hits | wash with boiled water, herbal poultice | fever → debilitation → death over days |
| Burn | fire, lava, boiling water | cool water, dressing | pain (stamina cap cut), infection risk |
| Hypothermia / frostbite | cold + wet | fire, shelter, warm food | slows → unconscious → death |
| Heatstroke | heat + dehydration | shade, water, rest | collapse |
| Sickness | bad water/food, contamination | rest, warmth, clean water, bland cooked food | days of lost productivity; rarely lethal alone |

- **Death only comes from real causes:** blood loss, advanced infection, starvation, dehydration, hypothermia, drowning, or catastrophic trauma (a long fall, a bear mauling, dragon's breath) that kills outright.
- Consequences: medicine becomes a craft tree (bandages, splints, poultices, herbal teas — foraged, learned, stocked before expeditions); every fight is about *what got hurt*, not a number; and the endgame (§13) becomes terrifying with zero extra tuning.
- Golden apples and true potions survive only as **relics** (§12) — irreplaceable old-world medicine that can erase a condition instantly. Using one should feel like burning a miracle.

### 1.6 Digestion and waste
Believability, not crudeness — there are **no explicit visuals**; the point is the *hygiene discipline and the disease vector*, carried by a modest need indicator and a quick "relieve yourself" crouch action.

- **What goes in comes out.** Eating and drinking slowly fill a combined bowel/bladder need. Clear it by relieving yourself (a brief, stationary action). Hold it too long and you get a focus/stamina nag and can't rest well ("you can't sleep bursting") — a gentle nudge, never a death cause on its own.
- **Where you go matters — contamination (§2, §5.6).** Relieving yourself fouls that spot: the ground and any water there gain a **tainted** state. Do it near your **water source, food stores, or camp** and you raise sickness odds for what you drink, cook, and store there — and lay a **scent trail** for predators (§4.3). The system teaches the real habit: a latrine **downhill and downwind** of your well and your bed. Rain and time wash a fouled patch clean slowly; flowing water carries it downstream.
- **Diarrhea (dysentery) is the killer.** It's caused by the same things sickness is — bad water (§1.2), a tainted vessel (§2), spoiled or raw food (§4.2), dirty hands (§5.6), infection (§1.5). While you have it, **hydration drains fast**: it's a live **dehydration emergency** (§1.2 → §1.5) that *will* kill you if you don't keep drinking **clean** water and resting. It also forces frequent, sometimes uncontrolled relief — so a sick survivor **contaminates their own site** unless they're disciplined, feeding the loop back on itself.
- **Treatment & prevention.** Rehydrate with clean water (the one thing that matters), rest, eat bland cooked food (the Sickness row, §1.5), and lean on the medicine tree (charcoal, herbal teas). But prevention is the whole design: **boil your water, keep the dirty and clean vessels apart (§2), wash your hands (§5.6), cook your food (§4.2), and don't foul your camp.**
- **Why it belongs here:** it turns dehydration from a slow meter into an acute cause of death, and it ties water discipline, food safety, hygiene, scent, and the condition model into one honest consequence chain — get careless with any of them and your own body becomes the emergency.

---

## 2. Water Vessels and Contamination

No plastic exists, so no abstract "water bottle." Vessels are a technology ladder, each gated by its own real craft (fire tiers in §3, glass chemistry in §3.4):

1. **Birch-bark cup** (day one: bark + fiber). Tiny. Cannot sit in fire — but supports the oldest trick in the book: **hot-rock boiling.** Heat stones in the campfire, drop them in the bark or hide container, and the water boils. Real, ancient, and your only purification for the first days.
2. **Waterskin** (tanned hide + fiber + tallow seal): the workhorse. About a day's water, light, packs soft.
3. **Clay jug and pot** (dug clay, shaped, dried, kiln-fired): boils directly over fire, good capacity, heavy, **shatters on hard falls**.
4. **Iron pot** (forge-tier): boil-and-carry, near-indestructible — the single most valuable tool in the game, exactly as on the show.
5. **Glass bottles** (late mid-game, §3.4): light, sealable, see the contents. A shelf of them is a trophy.

**Vessel contamination:** every container has a clean/tainted state. Filling from murky water, drinking after handling raw meat, or storing spoiled contents taints it; drinking from a tainted vessel rolls sickness *every time*, and the odds stack with what tainted it. Cleaning = boil water in it or scrub with clean water. The system quietly teaches the real habit: one dedicated dirty vessel for collecting, one clean vessel for drinking, and never confuse them.

**Purification, deferring to real life and the show (live build):** boiling is *the* way to make untreated water safe — exactly as on *Alone*, where every drink of lake or stream water is a giardia gamble unless boiled. Rules as built:
- **Boiling purifies.** A **fire-safe** vessel (the iron pot; later the clay pot) set over a **lit campfire** comes to a rolling boil after a few seconds and turns its whole load from raw/tainted to **clean**, sterilising the vessel with it. Water quality is per-vessel: *raw* (murky, ~15% sickness a sip), *tainted* (dirty vessel, ~45%), *clean* (safe), *salt*.
- **Not every vessel can go over a flame.** A hide waterskin or bark cup would scorch — those are purified by **hot-rock boiling** (heat stones in the fire, drop them in) or simply filled from a pot you've already boiled. So the pot is the workhorse: boil in it, carry in the skin.
- **Boiling does *not* desalinate seawater.** Open boiling only concentrates the salt; drinkable water from the sea needs **distillation** (capturing the steam). Salt water therefore stays salt no matter how long it boils — the honest answer at the coast is to find a freshwater stream. (The old "boil it first" hint for seawater was wrong and has been corrected.)
- **Rainwater is clean** (a cauldron rain-catch fills vessels clean, fuel-free).

**World water** keeps all v1.0 rules: finite placed sources flowing downhill and pooling; oceans/rivers/lakes as infinite bodies; currents with real force (you cannot swim up a waterfall; upstream is work; boats get swept); buoyancy (wood floats and drifts, metal sinks); stamina-draining swimming; seasonal ice (§10).

---

## 3. Fire & Heat Technology (new section)

Fire is the first technology and the spine of every other one. It is never free.

### 3.1 Making fire
- **No day-one flint-and-steel** — steel is steel (§8.5). Day one is **friction fire**: hand drill or bow drill (sticks + fiber + dry tinder). Slow, stamina-draining, failure-prone, skill-improving with repetition, and **nearly impossible with wet materials or in rain** — so tinder is gathered dry and kept dry, and getting soaked at dusk is the classic survival emergency.
- **Flint-and-pyrite strike** (flint + found pyrite): faster and rain-tolerant. **Flint-and-steel** returns as the forge-era luxury it really was.
- **Keeping fire matters:** a banked fire holds embers overnight; a dead fire is a fresh friction ordeal. **Ember carrying** (a coal wrapped in bark and moss — real technique) lets you transport fire to the next camp for hours. Very *Alone*.

### 3.2 Furnace tiers (temperature is the tech tree)
1. **Campfire** (~700°C): cooking, hot rocks, drying.
2. **Clay kiln** (~1,000°C): fires pottery, hardens clay furnaces, makes charcoal in small batches.
3. **Charcoal clamp** (logs buried under dirt, burning for hours — a real early industry): bulk charcoal, the fuel of everything above campfire tier.
4. **Bloomery / forge + leather bellows** (~1,200°C): smelts iron from ore. Bellows require leather — no iron before hunting and tanning.
5. **Glass furnace** (forge heat + flux, §3.4).

### 3.3 The ash economy
Burning hardwood yields **wood ash**. Ash → leached with water in a barrel → lye water → boiled down (iron pot) → **potash**. Potash is flux for glass, an ingredient in soap (cleaning stations and vessels, §5.6), and a real trade good — this exact process was the first industrial export of colonial America.

### 3.4 Why you can't make glass on day three (and why that's correct)
Pure silica sand melts at ~1,700°C — beyond any wood fire. Real glassmakers added **flux** (potash/soda ash) to drop the melt to ~1,000–1,200°C, reachable only in a bellows-fed furnace, with **lime** (burnt limestone or seashells) as a stabilizer. So glass requires, simultaneously: leather (bellows) + clay-working (kiln → furnace) + forge-tier heat + the ash economy + lime burning — and skill, with early melts coming out crude, green, and bubbled. **Glass gates itself with zero artificial locks**, arriving late mid-game as the trophy it historically was. Windows in the cabin mean you've mastered four industries.

---

## 4. The Food Chain

The chain is **acquire → process → preserve → secure**, and every arrow has failure modes.

### 4.1 Acquiring
- **Foraging:** berries, mushrooms (some toxic — identified by trial or study, then permanently marked in your journal), roots, eggs. Zero infrastructure; the day-one economy. Herbs for medicine (§1.5) live here too.
- **Hunting:** full system in §7.3 — tracking, wind, persistence hunting.
- **Fishing:** buffed to a primary food source. Stocks in small waters are finite and recover slowly; overfish a pond and it's empty for a season. Fish traps and gill nets work passively but can be raided by wildlife.
- **Farming:** crops need season, light, water proximity, and *tending*. **Weeds** spawn on farmland over time, slow the crop in their tile, and spread to neighbors; cleared by hand or hoe. The daily farm walk is a real chore with a real payoff. Untended fields choke; trampled crops die; farmland erodes back to dirt.

### 4.2 Raw food, spoilage, and cold
- Raw chicken/pork: high sickness chance. Raw beef/mutton: moderate. Fresh raw fish: low but nonzero. Rotten flesh: near-guaranteed severe sickness.
- Freshness (fresh → stale → spoiled) is driven by conditions: warmth and time spoil; smoking, salting, and drying preserve for weeks; sealed hardtack and dried goods last months. Spoiled food doesn't vanish — it *smells* (§4.3).
- **Underground is stable, not cold** (~10°C year-round): a dug cellar keeps roots, grain, and cooked food well, but won't hold raw meat through summer by itself. For that: **harvest winter ice** from frozen lakes and pack it in **sawdust or straw insulation** — a real ice house holds ice into late summer. The winter ice harvest becomes an annual calendar event, and it's how you save a harvest and a hunting surplus across seasons.

### 4.3 Food security — scent, bears, and your pockets
- All fresh food emits **scent** (raw > cooked > preserved > sealed), accruing from items *in your inventory*, open containers, racks, carcasses, and waste. Your pockets are not a scent-proof void: walking home with six raw salmon makes you a dinner bell.
- Scent draws **wildlife, not monsters**: bears above all, plus wolves and thieving foxes. A bear that finds accessible food takes it — it smashes stick-tier walls, flips unsecured chests, and strips racks. It doesn't tunnel logs or stone, and it doesn't want *you* unless you stand between it and the food (winter-hungry bears are bolder).
- **Countermeasures, all real:** sealed barrels, the **hung cache** (a chest raised on a post — bear-proof), stone smokehouses, cellars behind solid doors, cooking away from where you sleep, burying waste. The core dilemma of the food game: **carry food and attract predators, or cache it and pay travel time.**

---

## 5. Materials, Weight, and Building

### 5.1 Weight and carrying
- Base capacity: tools, a day's rations and water, and **one stone-class block** — carried at a trudge (no sprint, no ledge-jumps, heavy stamina drain). A real cubic meter of stone (~2,700 kg) is uncarryable; "one, barely, slowly" preserves the decision without abolishing manual work.
- Classes: **Heavy** — stone, ore, metal, anvils, sand/gravel (one, trudging). **Medium** — logs (2–3 slowed; one shoulder-carried at walking pace; or floated downriver). **Light** — planks, sticks, cloth, food, tools. Dirt sits light-medium: earthworks are laborious, not insane. Water is heavy (§2).
- Overloading is permitted and brutal: each tier over slows further and disables sprint/jump/swim. You can always drag yourself home at a crawl — a very *Alone* moment.
- Net effect: **you bring your base to the materials.** Quarry camps, fishing camps, smelting camps, and worn paths between them, maturing via the transport tree (§6).

### 5.2 The shelter ladder
1. **Bedroll** (wool + hide): sleep anywhere dry-ish; worst recovery; wildlife can wake you.
2. **Stick Frame blocks** (stick wall, thatch roof, hide/cloth drape): sticks + leaves + fiber + hides. Feather-light in bundles, fast to craft and place. They stop **weather and wind** (count for Shelter Quality) but burn eagerly, fray in storms over weeks (visible states, fiber-repairable), and **a bear walks straight through them** after scent. Your first home protects you from the sky, not from nature.
3. **Log tier:** heavy, strong, warm, bear-resistant. The mid-game homestead.
4. **Stone/brick:** fireproof, bear-proof, best cellars — and a seasonal logistics project that feels like one.

### 5.3 Structure and gravity
- Loose materials (**dirt**, sand, gravel, cobble, stick blocks) are gravity-affected when disturbed and need support — dirt falls; no floating dirt bridges. Crafted materials cantilever a few blocks; beam blocks span further. Full stress simulation is explicitly out of scope.
- **Tree felling:** cut the base and the tree falls as an entity (it can crush you), breaking into logs. **Tree age states** (sapling → young → mature → old growth) scale height, yield, and chop time — old growth yields ~3x a young tree — keeping vanilla's 1x1 silhouette while making forests read as real. (2x2 old-growth trunks: config option.)
- **Fire spreads** through flammability tiers (stick > thatch > planks > logs); rain fights it; a stone hearth exists for safe indoor fire.

### 5.4 Honest work times (tuned to real life, compressed honestly)
- **Stone:** real quarrying is hours per block. In-game: **20–40 seconds of active swinging per block with a proper iron pick** plus real stamina (≈20–30x vanilla — you feel it), softer stone faster, ores slower. Planned quarrying uses work sessions (§8.3) so "a morning at the quarry" compresses without lying about the labor. **Wheelbarrow** (§6) exists precisely for the quarry-to-cart leg.
- **Trees:** a mid-size tree is 1–3 minutes of active chopping with sharp iron, several with stone, scaling with age state, heavy stamina, session-eligible. Real felling is 10–30+ minutes with iron; the compression keeps the ratio between tools honest.
- **Dirt:** shoveling is genuinely fast in real life — digging stays quick. The ground is the one material that cooperates.
- **Leaves:** dense foliage is slow, tugging work to clear by hand; a blade (axe/hoe) shears through quicker. Tearing a canopy apart bare-handed leaves you with **snapped twigs and leaf litter**, not a tidy hedge cube — only a **clean cut with an axe or hoe salvages the whole leaf block**. (Saplings/apples still fall from the vanilla loot, so tree farming survives.)

### 5.5 Shelter Quality
Enclosed spaces get an implicit rating (roof, wall tier, door, heat source, dry floor) shown as a simple icon at bedtime — *exposed / rough / snug / secure* — governing sleep recovery, temperature protection, and storage conditions.

### 5.6 Light and cleanliness
- **Torches burn out** (~2 game days) to relightable stubs; **candles** (tallow) are cheap and short; **lanterns** (iron + oil) are permanent — a reason to want iron beyond tools. Caving becomes a light-supply problem, as it was for real miners.
- Stations and containers carry a **clean/dirty state** (raw meat, rot, mud). Dirty stations raise contamination odds for anything made on them; wash with clean water, better with **soap** (tallow + potash — the ash economy again, §3.3).

### 5.7 Climbing and rope
Getting *up* is its own problem, not a free jump.
- **Trees / leaves** behave like scaffolding: you can stand on a canopy, but you climb up (or crouch down) *through* the foliage. It's **slow and tiring** — hauling yourself up a ~6 m tree is real effort (drains stamina; run out and you fall out of the tree). Branches give holds, so it's a touch quicker than bare rock.
- **Bare walls / cliffs** — you can free-climb a **flat full-block face** of any height, up or down, but it's **slow, deliberate rock-climbing** and **brutally strenuous**: clinging burns stamina every tick in either direction, and **when your stamina is gone you lose your grip and fall.** Carry too much weight (more than ~one block's mass) and you simply can't climb — you need both hands and a light load. This makes a sheer face a real barrier, not a staircase.
- **Rope** — the civilized answer. A crafted rope (later, hemp/plant fiber; for now string/leather) you can **anchor at the top and drop down a face**, or **fix and climb** — *much* easier and cheaper on stamina than free-climbing, and it lets you descend safely. Rope turns a deadly cliff into managed infrastructure: place it once, reuse it, and it's the difference between scouting a ravine and dying in one. (Ties into transport §6 — rope + rivers + carts are how you move through vertical terrain.)

---

## 6. Transport: the logistics tech tree

1. **Hands** (day 1): tools, rations, one heavy thing at a crawl.
2. **Backpack** (leather + string): expands light/medium capacity.
3. **Wheelbarrow** (plank + iron-banded wheel): 3–4 heavy blocks on flat, worked ground — the quarry workhorse; useless in rough terrain, which is one more argument for paths.
4. **Travois/sledge** (sticks + hide): 4–8 heavy blocks dragged at walking pace on flat ground; awful uphill; excellent on snow and ice — winter is hauling season.
5. **Rivers as highways:** logs and light goods float downstream on their own (log drives to the building site are a real strategy); a raft carries you and cargo. Downstream vs. upstream shapes where you settle.
6. **Boats** (multi-slot cargo): the reason coasts dominate — fishing, boiled drinking water, and bulk freight in one place.
7. **Pack animals:** donkeys/llamas carry heavy loads, need feed, and can be killed by wolves — living infrastructure you defend.
8. **Horse + cart ("carriage"):** the big unlock. A hitched cart carries an order of magnitude more than saddlebags, but the rig has a **wide turning circle, hates slopes, and can't thread forest** — which makes **road-building emergent gameplay**: you clear, flatten, and maintain a route between quarry and homestead because the cart demands it, and your world grows roads for the most honest reason there is. Horses eat, tire (their own stamina and soreness), and need rest days. A horse is capital, not a vehicle.
9. **Minecarts + rails:** the endgame — iron-expensive, capacity-transformative. A rail line from mine to homestead is a multi-season megaproject that feels like the industrial revolution because, in this world's terms, it is.

---

## 7. Creatures

### 7.1 The core spawn rule: monsters only near loot structures
Hostile mobs spawn **only** within the influence radius of generated structures — dungeons, mineshafts, strongholds, ruined portals, ocean ruins, ancient cities, nether fortresses, and new surface ruins — with mob density, mob quality, and loot quality scaling together per structure tier. Open-world darkness spawns nothing.

- **Night belongs to nature**: cold, wolves, your dwindling torch. Zombies in a berry meadow would be noise.
- **Risk is legible**: mossy stone on a hillside is a promise and a warning; entering is an informed decision.
- **The expedition is a gameplay phase**: preserved rations, boiled water, bandages and poultices, full sleep, an exit plan — every raid is a hunt episode.
- **Clearing is durable**: destroy the spawner/heart and the structure stays yours (repopulating over many seasons at most). Cleared ruins are the world's best pre-built outposts, and your map grows zones of earned safety.

### 7.2 Wildlife
- Animals hear and smell. Sound spooks prey and draws predators; sneaking works; **wind direction matters** — approach downwind or be smelled first.
- **New/expanded prey:** deer (forest, fast, wary) and abundant rabbits alongside vanilla stock. Populations are regional, breed **seasonally** (spring births, autumn fattening) with real gestation and growth, and **can be locally overhunted** — empty a valley and it stays empty for a season or two. Finding an animal is not guaranteed; that's the point.
- **Predators:** wolf packs at night, bold in winter; **bears** as the food-security antagonist (§4.3); foxes and scavenger birds stealing small unattended items. Predators are rational — they respond to fire, injuries, and losses; they want calories, not murder.
- **Insects:** mosquito/blackfly pressure near still water and swamps, worst at dawn, dusk, and high summer — drawn to *you*, not light. Effects: restless sleep outdoors, a stacking irritation debuff, small disease chance in swamp/jungle. Counters, all real: **smudge fires** (smoky, green-wood fires repel them), mud on skin, string netting over the bedroll, and siting camp away from stagnant water. On the show, bugs break more people than bears do; here they're ambient pressure, not a mob.

### 7.3 Hunting and tracking
- The skill path: **stalk (sneak) + wind + bow**, where a missed shot costs the whole stalk — deer flat outsprint you and rabbits jink.
- **Tracking:** animals leave footprints (fresh → fading) and **wounded animals leave blood trails**. Snow makes tracking trivially readable, giving winter hunting its own identity just as winter farming dies.
- **Persistence hunting** — the gift from real life: humans are slow but nearly tireless, and real hunters ran game to collapse over hours. In-game, a deer's sprint pool is deep but finite; keep it moving at your sustainable jog without losing the trail and it eventually staggers and stands. An entire hunting playstyle, historically accurate, built from nothing but the stamina system and tracks.
- **Butchering** at the carcass with a knife (dirty or dull knives raise contamination); yields meat, hide, bone, **fat** (→ tallow → candles, soap, waterproofing), sinew (cordage).

### 7.4 Villagers
Rare living settlements — proof other humans made it. Villagers sleep, eat, farm, and won't trade at 3 a.m. **Barter** replaces the emerald-only economy (your smoked fish, hides, and potash are worth things). **Theft has consequences:** witnessed stealing tanks reputation — villagers turn hostile, guards/golems respond, trade closes — recovering slowly through time and gifts. Villages are also the only safe source of some seeds and livestock, and the player's one thread to a wider world.

---

## 8. Tools, Crafting, and Combat

### 8.1 No punching trees
Bare hands can't break logs. The true start: gather **loose sticks and stones** (ground scatter), flint from gravel, fiber from grass; **knap** flint for an edge; lash edge + haft + fiber into knife, axe, spear. Ten minutes in, you hold day-one human technology, earned the way humans earned it.

### 8.2 Timed, stationary crafting
Every recipe takes time — torch ~2s, stick-wall bundle ~5s, wooden tool ~15s, chest ~30s, iron armor a multi-minute smithing job — and crafting roots you: **no panic-crafting mid-fight**. Workshop time is a phase of the day, done in safety.

**Forge & temper (metal gear).** Metal tools and armor aren't finished in the grid — a grid-crafted piece comes out an **unforged blank**: brittle, barely usable, until you work it hot. The loop mirrors real smithing and is deliberately *long*:
1. **Heat** — hold the blank by a lit forge (blast furnace, furnace, smoker, campfire, or lava) and it glows hotter each tick; step away and it cools. You can only work it while it's hot.
2. **Hammer** — right-click an anvil (with a **smithing hammer** in your pack) to land a blow. A piece takes **many blows across several heats** — a blade a dozen, a cuirass close to twenty — so forging one tool is real, tended work, not a click.
3. **Quality** — the finished piece rolls a **random quality** (crude → rough → serviceable → fine → masterwork) that sets its durability. This is where the *skill-by-doing* and material-quality of §8.5 express themselves.
4. **Re-temper** — not happy with the roll? Reheat and rehammer to **reroll the quality** — but every rework **tires the steel and permanently lowers its ceiling**, and a re-forge also fully repairs the piece. So reworking is a gamble: chase a better edge at the cost of a blade that won't last as long. It's the honest tension of the forge — you can always try again, but the metal remembers.

### 8.3 Work sessions — the honest speedup
Big items are **multi-stage projects** with visible in-world states (a half-planked hull on the shore; hides tanning in a bark-liquor trough; a charcoal clamp smoking for hours; a drying clay furnace). Engaging a project enters **accelerated tick mode** — the same mechanism as sleep: game time compresses, body meters still drain per tick, weather rolls, and **events interrupt you** back to real time. You experience "I spent the afternoon on the boat" in ninety real seconds, and you're hungrier for it.
Benchmark honesty: a real **dugout canoe with stone tools and fire-hollowing takes roughly 1–3 weeks** of work — in-game, ~5–6 days of work sessions. When the hull finally slides into the river, you'll know what it cost.

### 8.4 Skill by doing (replaces XP)
No floating orbs. Repetition improves speed and reduces failure per craft family — early knapping and pottery attempts can waste material (never worse than ~25%, never on ultra-cheap recipes), and your character visibly gets better, like episode one of any *Alone* season. Mining, chopping, fire-starting, tracking, and smithing all skill up the same way.

### 8.5 Materials that make sense — steel up, diamonds sideways
- Armor/weapon ladder becomes the real one: hide → leather → padded wool → iron chain → iron plate → **steel** (iron + charcoal, forge mastery — historically exactly how it worked).
- **Diamond armor is abolished** — you can't forge diamond and it would shatter. Diamonds become what they really are: **cutting tips.** Diamond-tipped picks and saws cut faster and cleaner (their true industrial use), and diamonds are elite trade goods.
- **Masterwork quality replaces enchanting:** smithing skill + material quality + sharpening state determine a tool's stats — *Crude / Sound / Fine / Superior / Masterwork*. A **sharpening stone** station keeps edges at full speed; repair is cheaper than replacement, because real economies repair.
- The genuinely magical — mending, feather falling, true potions — survives only as **relics** from structure loot (§12): the old world knew things you cannot relearn.

### 8.6 Combat
- **Every swing costs stamina.** Reach and speed genuinely differ by weapon, which crowns **the spear king of the early game** — as it was for a hundred thousand years of real humans. Bows demand draw time and stamina; knives are butchering tools that can fight badly.
- **Shields are not walls:** blocking stops most damage but drains stamina per hit and staggers you against heavy blows — and **big creatures defeat shields outright**: a bear swats it aside, an **enderman reaches over or wrenches it away**, a ravager shatters your guard. The shield answers arrows and shambling dead, not apex threats. No 100% anything.
- Damage lands as **conditions** (§1.5), not number-drain: an arrow means bleeding; a mace means fracture; a zombie bite means infection risk. Armor reduces the *severity and chance* of conditions by type — plate turns a slash into a bruise but does little for a fall.

---

## 9. Farming and Husbandry
Crops have seasonal planting windows, need water proximity and light, suffer **weeds** (§4.1), trampling, drought stress, and early frost — the sky telegraphs fronts (§10), so harvest-before-the-frost is a recurring scramble. Livestock are capital: they eat (grass in summer, *stored hay in winter* — another autumn chore), breed seasonally, and die to wolves behind stick fences (log-tier fencing holds predators out). A working farm is the *stability* strategy; it takes two seasons to be worth more than foraging, and by year two it's the engine of everything.

### 9.1 Livestock is bartered wealth — priced in grain
- **You cannot spawn, find wild, or conjure a herd from nothing.** Cattle, horses, oxen, sheep, goats, and pigs are **domesticated capital** that only enters your world by **barter** — traded from villagers (§7.4) or the rare surviving homestead/ruin (§12). Chickens and rabbits sit at the bottom of the ladder (cheap, half-wild). You start with none; every farm animal is something you *earn*.
- **The price is the historical one, paid in grain.** History priced livestock against wheat, and so do we: a cow cost on the order of **~150 measures of wheat** (Roman baseline — a cow ≈ 150 denarii, a *modius* of wheat ≈ 1 denarius; one square meter of wheat would take *millennia* to buy one, which is exactly the point — it takes a real field). Grain is the de-facto currency, as it genuinely was. In game terms a cow runs **~2–3 stacks of wheat** (or equivalent trade goods), and the ladder keeps the real ratios: chicken (a handful) < sheep/goat < pig < **cow** < ox/horse (dearest). Everything on a config slider.
- **What this does to the midgame:** to lead home your first breeding pair you farm grain across **many harvests**, haul the heavy surplus to trade (weight matters, §5.1), and spend it all at once — one animal is weeks of work made solid. A full field stops being just food and becomes *money*; a herd is *accumulated wealth*; and losing a cow to wolves behind a lazy fence is losing a fortune, not a respawn.

---

## 10. World: Seasons, Weather, Biomes — and Navigation

- **Four seasons** (~2 real hours each, configurable). **Winter is the recurring boss**: farming stops, foraging dies, fishing means ice holes, wolves get bold, and you live off what your autumn looked like. The entire preservation chain exists so surviving your first winter is the emotional midpoint of the game.
- **Weather has teeth:** storms fray stick builds and ground boats; blizzards kill unprepared travelers; droughts stress crops; rain is free clean water. Weather-sense (red skies, wind shifts, animal behavior) telegraphs fronts — storms are decisions, not ambushes.
- **Biomes are strategies:** temperate river valley (the "tutorial-correct" hand), coast (food + freight, storm-exposed), taiga (fur and timber, brutal winter), desert (heat preserves food, water is the whole game), jungle (food everywhere, spoilage everywhere, bugs everywhere), mountains (ore and defense, thin soil).
- **Navigation (new):** no GPS — the coordinates screen is removed (config). **Compasses are craftable** (lodestone is real). Maps are **hand-drawn**: parchment that fills only where you have walked, with player-placed marks. The sun and stars genuinely indicate direction. Getting lost is possible, so cairns, blazed trees, landmarks, and your own worn paths *matter* — and finding your way home in a whiteout is a story you'll tell.

---

## 11. Death and Respawn
- Normal mode: no corpse-run teleportation fiction. When you go down, you are **"carried home"** — you wake at your homestead **days later in game time**, wounded (lingering conditions, drained vitality), and the world moved on: crops choked with weeds or died, fires dead, food spoiled, maybe a bear visited. **Time is the death penalty.** Your gear lies where you fell — scenting — and recovering it before the scavengers do is a mission.
- **Hardcore-Alone mode:** one life, choose your show — *Survive winter*, *Survive one full year*, or *Slay the Dragon* — with a tap-out stat screen (days survived, calories eaten, structures cleared, distance walked).
- Every harsh system carries a config slider (spoilage, season length, thirst, weight, bugs, navigation aids). The default preset is *demanding but fair*; the difference between immersive and chore is personal.

---

## 12. Structures, Loot, and the Old World

Structures are the ruins of a vanished civilization and the **only source of what you can't make** — and now that enchanting and diamond gear are gone from the crafting economy (§8.5), this fiction carries the whole top of the item ladder:

- **Relics:** enchanted-equivalent artifacts (a hatchet that never dulls = mending; boots that soften falls = feather falling), true potions, golden apples — irreplaceable, uncraftable, each one a decision to use or hoard.
- **Manufactured goods:** saddles, maps to other structures, sealed rations (never spoil until opened), exotic seeds, music discs, quality steel pieces.
- **Rationalized mob drops:** structure mobs drop what they carry — rusted gear and scrap to smelt down, tattered cloth, bones. The good gear is in the chests, not the corpses.
- **Redstone is old-world technology:** components are gated behind **engineering plans** found as structure loot — you *rediscover* pistons, comparators, rails. (Config flag to un-gate for builders; a mechanical-power alternative — water wheels, gears — is flagged as a beautiful v2.0 project, out of scope.)
- **Creepers stay**, structure-bound like everything hostile. Utterly unreal, deeply Minecraft — protected under principle 3.

Two playstyles fall out: the **homesteader** (slow, safe, farm-and-forge, masterwork gear) and the **raider** (fast, risky, relic-funded), with most players blending them.

---

## 13. The Endgame: the Dragon as a Mountaineering Expedition

The Dragon stays; the 30-minute speedrun dies of natural causes. Every vanilla step now runs through the survival economy, converting the checklist into a multi-season campaign with base camps — an assault on a summit.

- **Ender pearls** come only from endermen at high-tier structures (ancient cities, stronghold outskirts, warped ruins). Each pearl is a raid — against a creature that *wrenches your shield aside* (§8.6).
- **Blaze powder means the Nether**, and the Nether is the death zone: maximum permanent heat (heat-tier clothing and preparation mandatory), **no water anywhere** — every liter carried in — nothing edible, structure density maxed. Building the portal is itself a project: ten obsidian at Heavy weight is a week of logistics with cart and wheelbarrow. A fortress run means cached supplies at the portal, rationed water, and a planned extraction.
- **The stronghold is far**, eyes are too precious to burn casually — so triangulation, hand-drawn maps (§10), an overland or river route, and supply camps along a chain of previously cleared structures. Your whole playthrough becomes the supply line.
- **The injury system does the balancing.** You cannot out-eat damage; you arrive with the vitality you have; dragon's breath is a *burn + trauma* event; potions are finite relics. End crystal towers demand real climbing logistics — weight rules apply in the End too. **Beds still explode** (protected fiction), but hauling beds across a months-long expedition while keeping the wool dry is its own price: the cheese is permitted and honestly priced.
- Victory yields the vanilla rewards plus the true one: an elytra pilgrimage over a world where every cleared ruin on the way home is already yours. The credits roll over a survivor, not a speedrunner.

**The arc:** Spring 1 — knapped tools, friction fire, stick lean-to, first bear lesson. Summer 1 — log cabin, smoker, fish weir, first cleared dungeon, first pot. Autumn 1 — preservation sprint, hay in, cellar full. Winter 1 — the trial. Year 2 — iron, glass, boat, horse and cart, roads, pearl raids, obsidian logistics, the Nether. Year 2–3 — the march, the base camps, the End. A 60–100 hour arc with a shape, every hour of it the same daily loop that taught you the game.

---

## 14. Implementation Notes

- **Architecture:** Fabric modpack around one custom core mod (condition/injury system, weight, scent AI, timed crafting + work sessions, spawn-rule override, vessel states, seasons hooks), leaning on proven mods and datapacks where they fit (Serene Seasons / Tough As Nails lineage for temperature-thirst-seasons, Realistic Torches, falling-tree mods, finite-fluid mods). Loot tables, food-poison odds, and break-time multipliers are datapack territory. Genuinely custom work: scent-driven wildlife AI, persistence-hunting stamina for animals, the stick-block family with degradation, tree age states, weight-tiered movement, structure-scoped spawning, multi-stage projects with accelerated ticks, the condition panel, vessel contamination, hand-drawn maps, and the horse cart.
- **Prior art:** *TerraFirmaCraft* (knapping, weight, pit kilns — but abandons vanilla identity), *Vintage Story* (mandatory research — essentially this design as a standalone game), *Tough As Nails*, *Primitive Survival*, *First Aid* (injury zones). The differentiator remains principle 3: it must still *be Minecraft*.
- **Rollout tiers, playtested each:** Tier 1 — raw-food risk, no punching trees, injuries-lite (bleeding/bandage only), water currents, timed crafting: the game feels grounded immediately. Tier 2 — temperature, seasons, fire tech, torches, tree felling, stick shelters, structure-only spawning, vessels: the daily loop appears. Tier 3 — weight, scent/bears, spoilage conditions, full condition panel, work sessions, navigation, carts and roads: the full *Alone* experience, where the config sliders earn their keep.

---

## Appendix A: One In-Game Day (mid-game, autumn, log cabin era)

Wake at dawn — snug shelter, full recovery, no soreness because yesterday was a light day on purpose. The rain barrel is half full; set the iron pot boiling on the hearth and eat smoked fish and bread while it works (protein + grain; you need a vegetable today or fatigue starts). Fill the *clean* waterskin from the pot; the clay jug stays the dirty-collection vessel, always. Morning: run the trapline — two rabbits, a fox has robbed the third snare. Butcher at the stream with the sharpened knife, wash hands and blade. Midday: a work session on the boat (hull stage 3 of 5) — the afternoon compresses into a minute and a half of real time and you surface from it hungry, shoulders sore in the numbers. Snack jerky *while walking* to the felling site; shoulder-carry one old-growth log back, then a second by travois. Golden hour: the sky has been redding up all afternoon — frost is coming — so pull the carrots tonight, not Thursday. Load the smoker with rabbit, weed two rows while it starts, pitch hay to the penned sheep, hang the remaining raw meat in the cache on the post, bank the fire, bar the door. The wolves start up across the valley on schedule. Sleep — and at 3 a.m. real-time hiccups: a scent event, something snuffling at the cache post, hours of game time already gone, the hearth at embers. It can't reach the cache. That's why the cache exists. Back to sleep. Tomorrow: the mossy ruin two valleys over, because winter is coming, it has sealed rations in it, and you've put it off long enough.

## Appendix B: Changelog v1.0 → v1.1

- **Replaced** the health bar with the condition panel (vitality + discrete treatable injuries); death now has causes.
- **Replaced** water bottles with the vessel ladder (bark → skin → clay → iron pot → glass) + vessel contamination states + hot-rock boiling.
- **Added** §3 Fire & Heat Technology: friction fire, ember carrying, furnace tiers, charcoal clamps, the ash economy, and the self-gating chemistry of glass.
- **Added** hunting/tracking system: footprints, blood trails, wind, snow tracking, persistence hunting; deer and rabbit populations with seasonal breeding and local overhunting.
- **Added** combat redesign: stamina costs, spear primacy, shields drained and defeated by large creatures (enderman wrangle), damage-as-conditions.
- **Replaced** XP/enchanting with skill-by-doing + masterwork quality + relics; **abolished** diamond armor (diamonds → cutting tips, trade goods); **added** steel tier.
- **Added** work sessions (accelerated-tick project labor), sleep-as-time-skip with event interruption, exertion debt/soreness, hunger-gated sleep, eating while riding/walking (snack tier).
- **Added** horse + cart with emergent road-building; wheelbarrow; hay/winter feed chore.
- **Added** weeds on farmland; underground-as-stable-cool + ice house with sawdust insulation; winter ice harvest.
- **Added** mosquito/insect pressure with smudge fires, mud, netting.
- **Added** navigation: no coordinates, craftable compass, hand-drawn maps, celestial direction, cairns.
- **Added** village theft consequences and reputation.
- **Rationalized** mob drops; gated redstone behind found engineering plans; explicitly protected creepers and End bed-cheese as vanilla soul.
- **Rewrote** death: "carried home," time as the penalty, gear left scenting in the field.
- **Tuned** honest work times: stone 20–40s/block (iron pick), trees 1–3 min by age state, dirt fast and gravity-affected; dugout canoe ≈ 5–6 in-game days of sessions (real: 1–3 weeks).

## Appendix C: Changelog v1.1 → v1.2

- **Added** §1.6 Digestion and waste: a hygiene/disease system (no explicit visuals) where relieving yourself contaminates ground and water — foul your well/food/camp and you sicken yourself and scent for predators. **Diarrhea/dysentery** drains hydration fast, making dehydration an acute cause of death; prevention (boil water, clean vessels, wash hands, cook food, site a latrine downhill/downwind) is the whole point. Ties together §1.2, §1.5, §2, §4.2, §4.3, §5.6.
- **Added** §9.1 Livestock is bartered wealth: domesticated animals can't be spawned/found wild/bred from nothing — they're **bartered** (villagers §7.4, ruins §12) at **historical grain prices** (a cow ≈ ~150 measures of wheat, i.e. ~2–3 stacks; ratios scale chicken → ox). Grain becomes currency; a herd is earned wealth. Config-sliderable.
- **Clarified** §1.2 salt water: the **ocean is salt water** — drinking it (bare-handed or from a vessel) dehydrates rather than quenches; **boiling both purifies fresh water and desalinates seawater**. (Rivers/lakes stay fresh.) Now implemented.

## Appendix D: Changelog v1.2 → v1.3 (live build additions)

- **Added** §5.7 Climbing and rope: **leaves work like scaffolding** (stand on top, climb up / crouch down through) — slow and stamina-draining, like climbing a real ~6 m tree, and you fall out if you're spent. **Bare walls/cliffs** can be free-climbed up or down at slow rock-climbing pace, brutally strenuous (stamina every tick, fall when empty), impossible if overloaded (>~one block's mass). **Rope** anchors at the top to drop/climb a face far more cheaply — vertical terrain as managed infrastructure. Both climbing and rope implemented.
- **Expanded** §8.2 with **Forge & temper**: metal gear is a grid-crafted **unforged blank** (brittle) until you **heat** it at a forge and **hammer** it on an anvil (many blows across several heats) into a piece with a **random quality** (crude→masterwork) that sets durability. **Re-tempering** rerolls the quality but tires the steel (lower ceiling) and repairs the piece. Requires a craftable **smithing hammer**. Now implemented (iron + steel gear).
- **Expanded** §5.4 leaves as real foliage: **slow, tugging** to clear; **bare hand → sticks + leaf litter**, **axe/hoe → the leaf block** (vanilla saplings/apples still drop). Now implemented.
- **Implemented** §8.2 **timed, stationary crafting**: a recipe in the grid must be **worked** before the result can be taken (result-slot `mayPickup` gate), with a **green progress bar on the result slot** + action-bar %. Times are compressed real effort by category (simple ~2s, food ~5s, tool ~15s, station ~30s, armor ~2 min). **Resumable** — progress is kept per result item across closing/reopening and switching recipes. (Shift-click bulk-craft currently only times the first.)
- **Added** §8.1 **plant fiber → string**: strip fiber from grass/ferns/vines (a cutting blade strips more), twist 3 → 1 string — spider-free cordage feeding rope/bandages/bows. And friction fire (§3.1) now **requires tinder** (plant fiber or dry leaf litter), consumed when the ember catches — drilling alone only smokes. Now implemented.
- **Added** clothing insulation + thermoregulation feedback (§1.3): worn hide/leather **insulates** (warm in cold, a burden in heat), metal plates bake in the sun; body heat now **drives thirst (sweat) and hunger (shivering burns food; cold slows stamina recovery)**. Now implemented.
- **Added** vessel water gauge + **timed drinking** (§2): waterskin/iron-pot show a quality-tinted charge bar; drinking is a ~1.6 s action, not instant. Worn armor no longer counts against carry **volume**. Realistic **campfire recipe** (sticks over logs, no coal). **Vanilla torches abolished** (only the burn-down `alone` torch remains). Now implemented.

*End of proposal v1.3 (rolling — live-build changelog; features land in code as they're built).*