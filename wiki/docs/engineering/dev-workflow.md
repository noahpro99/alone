---
sidebar_position: 4
title: "Dev & Test Workflow"
---

**Two loops, two tools:**

1. **Iterating on a mod (fast loop):** `nix develop` → `./gradlew :core:runClient`. Loom
   launches the actual game with the mod hot-loaded — **no launcher involved.** This is where
   90% of development happens.
2. **Playtesting the assembled pack:** build all jars (`./gradlew build`), drop them into a
   **Prism Launcher** Fabric instance (Prism is in the flake). Lunar is a closed PvP-oriented
   client and does not run Fabric modpacks — Prism is the right tool for actually playing the
   pack; use Lunar only if we later ship an optimized-vanilla-adjacent variant.

**First-time setup:**
```bash
cd alone
nix develop                              # JDK 25 + gradle + graphics libs + prism
gradle wrapper --gradle-version 9.5.1    # generate ./gradlew once
./gradlew :core:runClient                # launch the dev client
```

See [the Dev Quickstart](../getting-started/dev-quickstart.md) for the quickstart.

## Open questions / decisions to revisit

- **Third-party adoption:** re-check each release whether Serene Seasons / Tough As Nails / a
  finite-fluid mod has a live 26.2 build before we build a custom equivalent (Phases 3, 7).
- **Config surface:** proposal §11 wants a slider for nearly every harsh system. Core should
  own one unified config from the start so modules register into it rather than each shipping
  its own file.
- **Where `no-punching-trees` finally lives:** it's seeded in Core now for Phase 0; it moves to
  Alone: Craft in Phase 6 alongside knapping.
- **Multiplayer:** proposal is single-player-flavored but nothing precludes co-op; keep all
  player state server-authoritative (data attachments) so it "just works" on a server.
