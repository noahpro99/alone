---
sidebar_position: 1
title: "Toolchain"
---

Minecraft moved to **calendar versioning** in 2026. `26.2` = the *second game drop of 2026*
(what the old scheme would have called ~1.21.11). This changes our stack in three important ways:

| Thing | Value | Why it matters |
|---|---|---|
| Mod loader | **Fabric** | Lightweight, mixin-based; best fit for a deep-systems overhaul (proposal §14). |
| Java | **JDK 25** | 26.1+ moved the target off Java 21. The flake pins this. |
| Mappings | **None — unobfuscated** | Since 1.21.10 Minecraft ships *unobfuscated with parameter names*; Fabric dropped Yarn **and** Mojang-mapping remap. There is **no `mappings` line** — class/method names are the real ones (`net.minecraft.world.level.block.Block`, `state.is(...)`). |
| Loom (Gradle plugin) | **`net.fabricmc.fabric-loom` 1.17-SNAPSHOT** | The **non-remapping** Loom plugin for 26.1+. The old `fabric-loom` id is the remapping variant for obfuscated ≤1.21.11 — using it fails with *"Failed to find official mojang mappings for 26.2."* Deps use plain `implementation`, not `modImplementation`. |
| Gradle | **9.5.1** | Required by Loom 1.17. Pinned via the wrapper. |
| Fabric Loader | **0.19.3** | Runtime. New enum-extension API (0.19.0+). |
| Fabric API | **0.152.1+26.2** | Tag-removal API, client-command API, events. |

**Breaking changes in 26.2 to keep in mind** (from the Fabric 26.2 notes):
- Rendering moved toward **Vulkan/Blaze3D** — avoid raw OpenGL; go through Blaze3D. (The flake ships `vulkan-loader`.)
- **Block IDs and item IDs are stored separately now** — registration code must separate IDs from `Block`/`Item` instances (no more `valueLookupBuilder`).
- Several screen methods moved from `Minecraft` to `Minecraft.getInstance().gui`.

**Ecosystem caveat:** 26.2 is brand new. Do **not** assume Serene Seasons / Tough As Nails /
finite-fluid / falling-tree mods are ported yet. Anything on our critical path we build
custom (the proposal already flags most of these as "genuinely custom work" in §14). We adopt
a third-party mod only once we confirm a live 26.2 build.
