# Alone

A survival-realism overhaul modpack for **Minecraft 26.2** (Fabric). Vanilla Minecraft is a game
about accumulation; Alone is a game about *maintenance* — water, food, warmth, shelter, and a
long-horizon goal (the Dragon) that takes seasons to attempt.

## 📖 Documentation

**All project documentation lives in the wiki:** https://noahpro99.github.io/alone/

The design proposal, engineering plan, dev quickstart, and progress backlog that used to be
scattered across root markdown files are now a single organized Docusaurus site under
[`wiki/`](wiki/), published to GitHub Pages on every push to `main`.

- **[Overview](https://noahpro99.github.io/alone/)**
- **[Getting Started](https://noahpro99.github.io/alone/getting-started/dev-quickstart)** — build and run the pack, cut a release, repo layout
- **[Design Proposal](https://noahpro99.github.io/alone/proposal/philosophy)** — the full v1.3 design vision, section by section
- **[Engineering](https://noahpro99.github.io/alone/engineering/architecture)** — toolchain, module architecture, phased rollout
- **[Progress & Backlog](https://noahpro99.github.io/alone/progress/backlog)** — what's built and what's next

## Quick start

```bash
nix develop                              # JDK 25 + gradle + graphics libs + node + Prism
gradle wrapper --gradle-version 9.5.1    # one time: generate ./gradlew
./gradlew :dev:runClient                 # launch the dev client with the whole pack hot-loaded
```

See the [Dev Quickstart](https://noahpro99.github.io/alone/getting-started/dev-quickstart) for the
full workflow, and [`wiki/README.md`](wiki/README.md) for editing the documentation site.

*AI was used in the development of this modpack.*