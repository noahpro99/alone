---
slug: /
sidebar_position: 1
sidebar_label: Overview
title: Alone
---

# Alone

A survival-realism overhaul modpack for **Minecraft 26.2** (Fabric).

> **One sentence:** Vanilla Minecraft is a game about accumulation; Alone is a game about
> *maintenance* — water, food, warmth, shelter, and a long-horizon goal (the Dragon) that takes
> seasons to attempt.

A vanilla player should recognize everything and be wrong about how all of it behaves. The
reference fantasy is the show *Alone*: dropped into wilderness with almost nothing, living the
daily loop of water, food, warmth, and shelter.

## Where to start

| If you want to… | Go to |
|---|---|
| Understand the design vision, system by system | **[Design Proposal](./proposal/philosophy.md)** |
| Build and run the pack, or cut a release | **[Getting Started](./getting-started/dev-quickstart.md)** |
| See how the code is structured and sequenced | **[Engineering](./engineering/architecture.md)** |
| See what's already built and what's next | **[Progress & Backlog](./progress/backlog.md)** |

## This wiki

This is the single source of truth for the project. It replaces the loose collection of
markdown files that used to live at the repo root:

- The **[Design Proposal](./proposal/philosophy.md)** — the full v1.3 design vision, one page per
  section (the `§N` cross-references used throughout the codebase map to these pages).
- The **[Engineering](./engineering/architecture.md)** plan — toolchain, module architecture, and
  the phased rollout.
- The **[Progress & Backlog](./progress/backlog.md)** — the running record of what's been built
  and what remains.
- **[Getting Started](./getting-started/dev-quickstart.md)** — the dev quickstart, building the
  distributable `.mrpack`, and the repo layout.

## Toolchain at a glance

Minecraft 26.2 · Fabric Loader 0.19.3 · Fabric API 0.152.1+26.2 · **JDK 25** ·
non-remapping Loom (`net.fabricmc.fabric-loom` 1.17) · Gradle 9.5.1 · **no mappings** —
26.1+ ships unobfuscated. Full detail in **[Toolchain](./engineering/toolchain.md)**.
