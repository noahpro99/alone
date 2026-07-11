---
sidebar_position: 3
title: "Project Layout"
---

```
modules/core/     Alone: Core   — foundation + "no punching trees"
modules/food/     Alone: Table  — raw-food risk (mixin + datapack tag)
flake.nix         NixOS dev environment
build.gradle      shared Loom/Fabric config for every module
gradle.properties pinned versions
```

## Adding a module later

`mkdir -p modules/<name>/src/main/{java,resources}`, add a `build.gradle` with its
`archivesName`, a `fabric.mod.json`, then two lines in `settings.gradle`:

```groovy
include '<name>'
project(':<name>').projectDir = file('modules/<name>')
```

> **Note:** if you `git init` this repo, Nix flakes only see git-tracked files — remember to
> `git add` new files or `nix develop` won't pick them up.
