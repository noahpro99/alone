#!/usr/bin/env bash
# Builds Alone.mrpack: external mods referenced from Modrinth (url+hash), custom Alone mods
# bundled under overrides/mods/. Requires: curl, jq, zip.
set -uo pipefail
MC="26.2"; LOADER_VER="0.19.3"; PACK_NAME="Alone"
UA="alone-modpack/0.1 (local)"
ROOT="$(pwd)"
# Single source of truth for the version — read the mod version so the pack never drifts from the build.
PACK_VER="$(grep -E '^mod_version=' "$ROOT/gradle.properties" | cut -d= -f2 | tr -d '[:space:]')"
WORK="$ROOT/dist/mrpack-work"
rm -rf "$WORK"; mkdir -p "$WORK/overrides/mods"

# required client experience mods
REQUIRED=(mouse-tweaks real-camera sodium iris sound sound-physics-remastered \
          enhancedvisuals modmenu ambientsounds presence-footsteps not-enough-animations jei yacl)
OPTIONAL=()

api(){ curl -s -m 30 -H "User-Agent: $UA" "$1"; }
declare -A SEEN
ENTRIES="$WORK/entries.jsonl"; : > "$ENTRIES"

# env override for optional mods (client optional / server unsupported)
resolve(){ # slug  force_env(optional|"")
  local slug="$1" force="${2:-}"
  [[ -n "${SEEN[$slug]:-}" ]] && return
  SEEN[$slug]=1
  local ver proj
  ver=$(api "https://api.modrinth.com/v2/project/$slug/version?loaders=%5B%22fabric%22%5D&game_versions=%5B%22$MC%22%5D")
  local first; first=$(echo "$ver" | jq -c '.[0] // empty')
  [[ -z "$first" ]] && { echo "  !! $slug: no $MC/fabric version"; return; }
  proj=$(api "https://api.modrinth.com/v2/project/$slug")
  local cenv senv
  cenv=$(echo "$proj" | jq -r '.client_side'); senv=$(echo "$proj" | jq -r '.server_side')
  [[ "$cenv" == "unknown" || "$cenv" == "null" ]] && cenv="optional"
  [[ "$senv" == "unknown" || "$senv" == "null" ]] && senv="optional"
  if [[ "$force" == "optional" ]]; then cenv="optional"; senv="unsupported"; fi
  echo "$first" | jq -c --arg c "$cenv" --arg s "$senv" '
    (.files[] | select(.primary==true)) // .files[0] | {
      path: ("mods/" + .filename),
      hashes: {sha1: .hashes.sha1, sha512: .hashes.sha512},
      env: {client: $c, server: $s},
      downloads: [.url],
      fileSize: .size
    }' >> "$ENTRIES"
  echo "  + $slug ($(echo "$first" | jq -r '.version_number'))"
  # required deps
  for pid in $(echo "$first" | jq -r '.dependencies[]? | select(.dependency_type=="required") | .project_id // empty'); do
    local dslug; dslug=$(api "https://api.modrinth.com/v2/project/$pid" | jq -r '.slug // empty')
    [[ -n "$dslug" ]] && resolve "$dslug" ""
  done
}

echo "Resolving mods…"
for s in "${REQUIRED[@]}"; do resolve "$s" ""; done
for s in "${OPTIONAL[@]+"${OPTIONAL[@]}"}"; do resolve "$s" "optional"; done
# fabric-api explicitly (needed by the Alone mods + many others)
resolve fabric-api ""

# dedupe by path, assemble index
jq -s 'unique_by(.path)' "$ENTRIES" > "$WORK/files.json"
COUNT=$(jq 'length' "$WORK/files.json")
jq -n --arg name "$PACK_NAME" --arg ver "$PACK_VER" --arg mc "$MC" --arg fl "$LOADER_VER" \
      --slurpfile files "$WORK/files.json" '{
  formatVersion: 1, game: "minecraft", versionId: $ver, name: $name,
  summary: "Alone — survival-realism overhaul.",
  files: $files[0],
  dependencies: {"minecraft": $mc, "fabric-loader": $fl}
}' > "$WORK/modrinth.index.json"

# bundle the custom Alone mods as overrides (not on Modrinth)
cp "$ROOT/modules/core/build/libs/alone-core-$PACK_VER.jar" "$WORK/overrides/mods/" 2>/dev/null && echo "  bundled alone-core"
cp "$ROOT/modules/food/build/libs/alone-food-$PACK_VER.jar" "$WORK/overrides/mods/" 2>/dev/null && echo "  bundled alone-food"

OUT="$ROOT/dist/${PACK_NAME}-${PACK_VER}.mrpack"
rm -f "$OUT"
( cd "$WORK" && zip -qr "$OUT" modrinth.index.json overrides )
echo "Built $OUT  ($COUNT external files + bundled Alone mods)"
