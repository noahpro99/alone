---
sidebar_position: 2
title: "1. The Player's Body"
---

Four survival meters — hunger, thirst, temperature, stamina — plus sleep and a **condition panel** that replaces the health bar entirely (§1.5). UI rule: meters are small and corner-mounted, pulsing into prominence only in danger bands. The screen feels like vanilla until your body is complaining.

## 1.1 Hunger
- Drains from *activity*, not time alone. A day of sprinting, chopping, and swimming burns 3–4x an idle day. Cold adds a passive burn (your body heats itself).
- **Nutrition variety:** foods belong to groups — protein, vegetable, grain, fruit/forage. Effective max hunger shrinks after days of a single group ("food fatigue"), recovering with variety. No vitamins spreadsheet; just "you can't live on bread."
- **Eating on the move:** snack-tier food (berries, jerky, hardtack, dried fish) can be eaten while walking or **riding**. Proper meals (stews, cooked cuts) root you in place for a few seconds. Nothing can be eaten while sprinting or in combat.
- **Hunger gates sleep** (§1.4): below half hunger, sleep is restless and recovery is poor; while starving, you cannot stay asleep at all — you wake repeatedly through the night and gain nothing. An empty stomach is a survival emergency partly *because* it steals your rest.

## 1.2 Thirst
- Drains faster than hunger; faster still in heat or exertion. Water weighs 1 kg/liter, so carried water is a real logistics cost (§4.1).
- Source quality: rain catch/springs (clean) → flowing rivers (usually fine, small illness chance) → still ponds/swamps (dangerous raw) → **ocean = salt water** — drinking it (bare-handed *or* from a vessel) **dehydrates** you rather than quenching. **Boiling purifies fresh water and desalinates seawater** — it's the one fix for anything, so the sea is only water *after* you've boiled it.
- See §2 for containers — v1.1 removes the abstract "water bottle" and replaces it with a real vessel technology ladder.

## 1.3 Temperature
- Body temperature driven by biome, season, time of day, weather, immersion, clothing, and heat sources.
- **Cold band:** slowdown → stamina penalties → *hypothermia* (a condition, §1.5, ultimately fatal). Countered by layered wool/leather clothing, fire, and Shelter Quality (§5.5). **Wetness multiplies cold** — falling in a winter river is an immediate emergency; a fire dries you.
- **Heat band:** thirst drain → stamina penalties → *heatstroke*. Countered by shade, night travel, light clothing, water. The Nether maxes this axis permanently (§13).

## 1.4 Stamina, exertion debt, and sleep
- Stamina drains from sprinting, jumping, swimming, chopping, mining, and fighting; restores from rest, food, and fully from sleep. Empty stamina: no sprint, half mining speed, weak attacks, short breath-hold.
- **Exertion debt ("soreness"):** burn beyond ~150% of a normal day's stamina and tomorrow's stamina cap is reduced 10–25%, cleared by a full night's sleep plus a lighter day. Deliberately capped at one level deep — no soreness spirals; it's a pacing decision, not a punishment.
- **Sleep is a need, not a skip button.** Each skipped night stacks fatigue (stamina cap −25%, then slowness, then screen-blur micro-sleeps). Sleeping requires warmth and dryness: a bedroll (wool + hide) is the minimum, and Shelter Quality (§5.5) sets recovery.
- **Sleep as honest time-skip:** sleeping accelerates game ticks rather than teleporting to dawn. Body needs still drain per tick, weather rolls on, fires burn down, smokers finish. If a scent event, loud noise, or storm damage occurs nearby, **you wake "a second later" in real time — but hours later in game time**: it's 3 a.m., the hearth is embers, and the bear is already at the drying rack. Maximum tension, no new mechanics.

## 1.5 The Condition Panel — injuries replace health
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

## 1.6 Digestion and waste
Believability, not crudeness — there are **no explicit visuals**; the point is the *hygiene discipline and the disease vector*, carried by a modest need indicator and a quick "relieve yourself" crouch action.

- **What goes in comes out.** Eating and drinking slowly fill a combined bowel/bladder need. Clear it by relieving yourself (a brief, stationary action). Hold it too long and you get a focus/stamina nag and can't rest well ("you can't sleep bursting") — a gentle nudge, never a death cause on its own.
- **Where you go matters — contamination (§2, §5.6).** Relieving yourself fouls that spot: the ground and any water there gain a **tainted** state. Do it near your **water source, food stores, or camp** and you raise sickness odds for what you drink, cook, and store there — and lay a **scent trail** for predators (§4.3). The system teaches the real habit: a latrine **downhill and downwind** of your well and your bed. Rain and time wash a fouled patch clean slowly; flowing water carries it downstream.
- **Diarrhea (dysentery) is the killer.** It's caused by the same things sickness is — bad water (§1.2), a tainted vessel (§2), spoiled or raw food (§4.2), dirty hands (§5.6), infection (§1.5). While you have it, **hydration drains fast**: it's a live **dehydration emergency** (§1.2 → §1.5) that *will* kill you if you don't keep drinking **clean** water and resting. It also forces frequent, sometimes uncontrolled relief — so a sick survivor **contaminates their own site** unless they're disciplined, feeding the loop back on itself.
- **Treatment & prevention.** Rehydrate with clean water (the one thing that matters), rest, eat bland cooked food (the Sickness row, §1.5), and lean on the medicine tree (charcoal, herbal teas). But prevention is the whole design: **boil your water, keep the dirty and clean vessels apart (§2), wash your hands (§5.6), cook your food (§4.2), and don't foul your camp.**
- **Why it belongs here:** it turns dehydration from a slow meter into an acute cause of death, and it ties water discipline, food safety, hygiene, scent, and the condition model into one honest consequence chain — get careless with any of them and your own body becomes the emergency.
