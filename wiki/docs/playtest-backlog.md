# Playtest backlog

Running list of issues/ideas raised in playtest, to work through. Checked = done.

## Bugs / exploits
- [ ] **1. Gravel flint exploit** — place a gravel block and break it fast; it sometimes drops flint. Exploitable (infinite flint from one gravel). Flint should only come from *genuinely mining* natural gravel, not player-placed / instant place-break.
- [ ] **2. No throwing a rock while crouched** — crouch is for deliberate actions (knap, rive, sneak-place); throwing a rock while crouched should be blocked.
- [ ] **4. Axe hand orientation** — the axe displays in the wrong orientation held in hand (item model display transform).
- [ ] **9. Drank water but hydration didn't rise** — (message cut off) drank but hydration stayed. Investigate the two-stage gut→hydration absorb.
- [ ] **11. Craft time only charged once** — you pay the timed-craft cost the first time, but making the same item again is instant. Worked-ticks aren't reset after taking the result, so repeats skip the timer. Should charge every craft.
- [ ] **16. Guards move too slow** — guards path very slowly; check movement speed. Also: is their arrow count realistic?
- [ ] **17. Caught climbing a 1-block height** — you can still get "caught"/gripped climbing a single-block step; that should never trigger free-climb.

## Tuning / realism questions (verify against IRL → MC-day scale)
- [ ] **3. Fire duration** — is a fire lasting as long as it should vs real (relative to the MC-day/real-day scale)?
- [ ] **6. Endurance bar (golden carrot)** — drains slowly; what does it represent IRL, and are the amounts realistic?
- [ ] **8. Dehydration rate** — are the dehydration/thirst rates realistic in days → MC days?
- [ ] **10. Plank & stick fuel** — are planks (and sticks) set to the right fire-fuel burn time relative to the MC-day/real-day scale?
- [ ] **12. Fence weight** — fence carry weight seems wrong; check vs a real fence/board.
- [ ] **13. Food sickness** — got sick from food: right duration? right dehydration amount? does resting affect it (should it)?
- [ ] **14. Crafting-table path** — is the progression to the crafting table and what it unlocks realistic?

## Features / mechanics
- [ ] **5. Drinking-water temperature** — drinking cold/cool water should lower your body temperature a bit (and hot drinks warm you).
- [ ] **7. Boats** — realistic stamina cost + realistic speed; add **two-person boats**; make boats **hard to craft** like IRL (a real build, not instant).
- [ ] **15. Trampling crops angers the village** — jumping on / trampling crops (farmland → dirt) should count as a crime against the village.
- [ ] **18. Villager & guard drops** — villagers and guards should drop **raw meat, fat, hide, brains, etc.** (realistic — they're people/animals with bodies).

---
*Raised in playtest; ordering above is rough. Work quick bugs first, then tuning, then the big features (boats).*
