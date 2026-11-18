#!/usr/bin/env bash
# Builds mac/dist/Rohy.app (menu bar app, ad-hoc signed for local use).
set -euo pipefail
cd "$(dirname "$0")/.."
# UNIVERSAL=1: one binary for Apple Silicon and Intel. ROHY_VERSION / ROHY_BUILD: version shown by macOS.
CONFIG="${1:-release}"
FLAGS=(-c "$CONFIG")
if [[ "${UNIVERSAL:-0}" == "1" ]]; then FLAGS+=(--arch arm64 --arch x86_64); fi
swift build "${FLAGS[@]}" --product Rohy
BIN="$(swift build "${FLAGS[@]}" --show-bin-path)/Rohy"
APP="dist/Rohy.app"
rm -rf "$APP"
mkdir -p "$APP/Contents/MacOS" "$APP/Contents/Resources"
cp "$BIN" "$APP/Contents/MacOS/Rohy"
cp Resources/Info.plist "$APP/Contents/Info.plist"
if [[ -n "${ROHY_VERSION:-}" ]]; then
  plutil -replace CFBundleShortVersionString -string "$ROHY_VERSION" "$APP/Contents/Info.plist"
fi
if [[ -n "${ROHY_BUILD:-}" ]]; then
  plutil -replace CFBundleVersion -string "$ROHY_BUILD" "$APP/Contents/Info.plist"
fi
# Inter (OFL), registered by macOS through ATSApplicationFontsPath.
cp -R Resources/Fonts "$APP/Contents/Resources/Fonts"
codesign --force --sign "${CODESIGN_IDENTITY:--}" --options runtime "$APP"
echo "Built $APP"
