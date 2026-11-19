#!/usr/bin/env bash
# Fails when R8 removed the no-argument constructor of a Firebase/ML Kit component registrar from the release
# build: those registrars are created by reflection, and without it the QR scanner crashes (Rohy 0.1.0).
# Run after `./gradlew :app:assembleRelease` (scripts/check-android.sh does both).
set -euo pipefail
usage="${1:-$(cd "$(dirname "$0")/.." && pwd)/android/app/build/outputs/mapping/release/usage.txt}"
[ -f "$usage" ] || { echo "R8 usage report not found: $usage" >&2; exit 1; }
stripped=$(awk '/^[^ ].*Registrar:$/ { cls = $0; next } /^[^ ]/ { cls = "" }
  cls != "" && cls !~ /bouncycastle|SelectionRegistrar/ && /<init>\(\)/ { print cls }' "$usage")
if [ -n "$stripped" ]; then
  echo "R8 removed the constructor of these registrars (see android/app/proguard-rules.pro):" >&2
  echo "$stripped" >&2
  exit 1
fi
echo "R8 kept every component registrar constructor."
