#!/usr/bin/env bash
# Kotlin tests (including the interop test against the Swift server) + Kover thresholds + debug build.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
source "$ROOT/scripts/env.sh"
if [ -z "${ROHY_HARNESS:-}" ]; then
  (cd "$ROOT/mac" && swift build --product rohy-harness)
  export ROHY_HARNESS="$(cd "$ROOT/mac" && swift build --show-bin-path)/rohy-harness"
fi
cd "$ROOT/android"
./gradlew --console=plain ${GRADLE_ARGS:-} :core:test :core:koverVerify :core:koverLog :app:testDebugUnitTest :app:assembleDebug
