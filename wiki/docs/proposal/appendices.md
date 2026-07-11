---
sidebar_position: 16
title: "Appendices & Changelog"
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
