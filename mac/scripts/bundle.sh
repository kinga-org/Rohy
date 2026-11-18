#!/usr/bin/env bash
# Builds mac/dist/Rohy.app (menu bar app, ad-hoc signed for local use).
set -euo pipefail
cd "$(dirname "$0")/.."
CONFIG="${1:-release}"
swift build -c "$CONFIG" --product Rohy
BIN="$(swift build -c "$CONFIG" --show-bin-path)/Rohy"
APP="dist/Rohy.app"
rm -rf "$APP"
mkdir -p "$APP/Contents/MacOS" "$APP/Contents/Resources"
cp "$BIN" "$APP/Contents/MacOS/Rohy"
cp Resources/Info.plist "$APP/Contents/Info.plist"
codesign --force --sign "${CODESIGN_IDENTITY:--}" --options runtime "$APP"
echo "Built $APP"
