#!/usr/bin/env bash
# Downloads the Alone modpack's external (third-party) mods for Minecraft 26.2 / Fabric
# into a target folder (default ./mods), resolving required dependencies automatically.
# Requires: curl, jq.  Re-run any time to refresh.
set -uo pipefail
MC="26.2"; LOADER="fabric"
OUT="${1:-mods}"
UA="alone-modpack/0.1 (local dev)"
mkdir -p "$OUT"

# The mods requested for the pack (Modrinth slugs).
REQUESTED=(mouse-tweaks real-camera sodium iris sound sound-physics-remastered \
           enhancedvisuals modmenu physicsmod ambientsounds presence-footsteps not-enough-animations jei yacl)

declare -A SEEN
api() { curl -s -m 30 -H "User-Agent: $UA" "$1"; }

fetch() {
  local slug="$1"
  [[ -n "${SEEN[$slug]:-}" ]] && return
  SEEN[$slug]=1
  local json; json=$(api "https://api.modrinth.com/v2/project/$slug/version?loaders=%5B%22$LOADER%22%5D&game_versions=%5B%22$MC%22%5D")
  local fname url; fname=$(echo "$json" | jq -r '.[0].files[0].filename // empty')
  url=$(echo "$json" | jq -r '(.[0].files[] | select(.primary==true) | .url) // .[0].files[0].url // empty')
  if [[ -z "$fname" ]]; then echo "  !! $slug: no $MC/$LOADER version found"; return; fi
  echo "  >> $slug  ($(echo "$json" | jq -r '.[0].version_number'))"
  curl -s -L -m 180 -o "$OUT/$fname" "$url" && echo "     saved $fname"
  # follow REQUIRED dependencies
  for pid in $(echo "$json" | jq -r '.[0].dependencies[]? | select(.dependency_type=="required") | .project_id // empty'); do
    local dslug; dslug=$(api "https://api.modrinth.com/v2/project/$pid" | jq -r '.slug // empty')
    [[ -n "$dslug" ]] && { echo "     needs -> $dslug"; fetch "$dslug"; }
  done
}

echo "Downloading external mods for MC $MC ($LOADER) into: $OUT"
for s in "${REQUESTED[@]}"; do fetch "$s"; done
echo "Done. $(ls -1 "$OUT"/*.jar 2>/dev/null | wc -l) jars in $OUT/"
