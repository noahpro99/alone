---
sidebar_position: 2
title: "Building the mrpack"
---

The distributable is an **`.mrpack`** (Modrinth format) — the Alone mods bundled with the curated
external mods (Sodium, Iris, JEI, sounds, animations, …) resolved from Modrinth by URL+hash.

```bash
# 1. build the alone-core / alone-food jars at the current version
nix develop -c ./gradlew build

# 2. build the mrpack (needs curl, jq, zip on PATH; hits the Modrinth API)
nix-shell -p jq zip curl --run "bash scripts/build-mrpack.sh"
```

Output: **`dist/Alone-<version>.mrpack`**. The version comes from `PACK_VER` in
`scripts/build-mrpack.sh`, and the script bundles `modules/{core,food}/build/libs/alone-*-$PACK_VER.jar`,
so step 1 must have produced jars at that same version. **To cut a new version**, bump both first:

```bash
sed -i 's/mod_version=0.3.0/mod_version=0.4.0/' gradle.properties
sed -i 's/PACK_VER="0.3.0"/PACK_VER="0.4.0"/' scripts/build-mrpack.sh
```

Install the resulting `.mrpack` with Prism/Modrinth/ATLauncher (Minecraft 26.2 / Fabric). The mod
jars alone (for a manual Fabric install) are in `modules/*/build/libs/` and need Fabric API 0.152.2+.
