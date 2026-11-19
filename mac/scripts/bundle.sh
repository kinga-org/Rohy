#!/usr/bin/env bash
# Builds mac/dist/Rohy.app (menu bar app, ad-hoc signed for local use).
set -euo pipefail
cd "$(dirname "$0")/.."
# UNIVERSAL=1: one binary for Apple Silicon and Intel. ROHY_VERSION / ROHY_BUILD: version shown by macOS.
CONFIG="${1:-release}"
if [[ "${UNIVERSAL:-0}" == "1" ]]; then
  # One build per architecture, then lipo: a multi-arch `swift build` fails on some Xcode versions.
  SLICES=()
  for ARCH in arm64 x86_64; do
    swift build -c "$CONFIG" --arch "$ARCH" --scratch-path ".build/$ARCH" --product Rohy
    SLICES+=("$(swift build -c "$CONFIG" --arch "$ARCH" --scratch-path ".build/$ARCH" --show-bin-path)/Rohy")
  done
  BIN=".build/Rohy-universal"
  lipo -create "${SLICES[@]}" -output "$BIN"
else
  swift build -c "$CONFIG" --product Rohy
  BIN="$(swift build -c "$CONFIG" --show-bin-path)/Rohy"
fi
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
codesign --force --sign "${CODESIGN_IDENTITY:--}" --options runtime "$APP"
echo "Built $APP"
