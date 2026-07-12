# Changelog

Player-facing notes for the Alone modpack. Newest at the top.

## Unreleased

### New features

- **The start — an Alone-style loadout.** On your first join to a world you wake with nothing, are told the
  **biome** you're in and what it will do to you, and get to **pick two items** from an approved list
  (`/loadout`, click-to-pick). A one-time, per-world choice that survives death. See *The Start — Your Loadout*.
- **Ferro rod** — a reliable, **rain-tolerant** fire-starter above the friction bow drill: catches in a couple
  of strokes for next to no stamina, and lights in the rain where a drill only smokes. Forged from steel, or
  brought as a loadout pick.
- **Shelter ladder** — **thatch** (a cheap day-one roof woven from leaf litter + sticks; flammable),
  **tarp** (the premium, *fireproof*, packable roof you can pitch over a fire), and a **warmth-rated sleeping
  bag** (full rest through a freezing night — but a sweatbox on a warm one, so it's a cold-weather tool).
- **Sewing kit** — mend worn **leather/hide clothing** by hand (a plant-fibre thread per patch); there's no
  anvil in the wild.
- **Towel** — right-click to **rub yourself dry at once**, the fast counter to wet-cold after you climb out
  of a river or duck out of the rain.
- **Hand saw** — the woodworking upgrade over splitting boards by hand: far quicker, far less sweat, and more
  clean boards per log.
- **Gill net** — the portable, **open-water** counterpart to the fixed fish weir: faster and holds a batch,
  but only fishes deep water (the weir still works any shoreline).
- **Grain → bread path** — bread is now *made*, not crafted from raw wheat: **grind** wheat to flour, work it
  with water into **dough**, then **bake** it on a fire or in a furnace.
- **Squirrels climb trees** — cornered near a tree, a squirrel scurries up the trunk instead of hopping like
  a rabbit.

### Balance & realism

- **Ice houses need restocking.** Packed ice now slowly melts in an **above-freezing biome** (so a lowland
  ice house is a restock-each-winter store), while in a cold biome it keeps for free — *where* you site it
  matters.
- **The sleeping bag is a real choice, not an upgrade** over the bedroll: it wins in the cold and roasts you
  in the heat.
- **Thatch, tarp, and the sleeping bag are real labour** — timed weaving/sewing at the bench, not instant
  crafts. Same for grinding flour.
- **Falls sprain by chance, not always** — a fit person walks off a short drop most of the time.
- Spare woven baskets now count toward carry volume (only the one carry-aid basket is free).

### Fixes

- **Cold barely worked.** A phantom "0-degree heat source" with no fire nearby was warming any cold body back
  toward neutral, pinning body temperature near 0 on land — so hypothermia, freeze damage, and cold-chill
  sickness were effectively unreachable unless you stood in a fire. Cold now bites.
- **Swimming under ice wasn't deadly** — the water was taking the *air's* roof warmth. Ice-biome water is now
  near-freezing whether open or frozen over.
- **Skills were wiped on death** (they were meant to survive it) — now they persist.
- **The loadout could be re-picked after death** — now it's genuinely one-time.
- **Bleed-out only worked on ~1 in 5 wounded animals** (a tick phase-aliasing bug) — now on all of them.
- **Fall injuries were applied twice** — turning "a sprain is a chance" into a certainty above ~10 m.
- **"Going off" food never made you sick** at a stable temperature — the eat check now drains freshness like
  the tooltip does.
- **Dirty-hand wounds could never turn septic** — now an untreated contaminated wound can fester and kill, as
  documented.
- **The drying rack lost days of away-time** after a long absence — now the backlog catches up.
- **Fibre stripping hung forever on tall grass / ferns** — the tug counter no longer resets on a double-height
  plant.
- **Grassy, sandy, and snowy 2-block edges weren't climbable** — the natural surfaces are now in the climb tag.
- **A finished boil was lost** if the fire burned out before you lifted the pot.
- **Plank duplication** on a near-full inventory.

### Debug (op-only)

- `/alone wet` · `/alone cold` · `/alone hot` · `/alone dirty` · `/alone kit` — set up test conditions and get
  this build's new items instantly, alongside `/alone reset`.
