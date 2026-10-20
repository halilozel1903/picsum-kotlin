#!/usr/bin/env bash
# Captures README screenshots of the sample app on a running emulator.
# The sample opens each screen from the `scene` extra and loads it live from the Lorem Picsum API, with the photos
# from Picsum's image CDN. Each scene logs when its data and every photo on screen have loaded; a scene that shows an
# error or does not finish loading fails the run. There is no fallback data. Every capture is also checked for the
# expected text and for a blank image.
#
#   bash scripts/screenshots.sh tablet   # pixel_tablet in landscape: gallery, detail, effects, authors
set -euo pipefail
source "$(dirname "$0")/screenshot-lib.sh"

# The text each scene must show; the capture fails without it. None of these appear on the error screen.
expected_text() {
  case "$1" in
    tablet-gallery) echo "Showing" ;;
    tablet-detail) echo "Original size" ;;
    tablet-effects) echo "Blur 4 of 10" ;;
    tablet-authors) echo "photographers in the first" ;;
  esac
}

SCENE_TIMEOUT=180

install_sample
ensure_landscape
set_night_mode light
for scene in tablet-gallery tablet-detail tablet-effects tablet-authors; do
  fresh_launch --es scene "$scene"
  wait_for_scene "$scene" "$SCENE_TIMEOUT"
  capture "$scene" "$(expected_text "$scene")"
done
