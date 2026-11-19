#!/usr/bin/env bash
# Kotlin tests (including the interop test against the Swift server) + Kover thresholds + debug and release
# builds; the release build is minified by R8, which can break what libraries load by reflection.
# Animal Sniffer: `core` only calls Java APIs that exist on Android 10 (minSdk).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
source "$ROOT/scripts/env.sh"
if [ -z "${ROHY_HARNESS:-}" ]; then
  (cd "$ROOT/mac" && swift build --product rohy-harness)
  export ROHY_HARNESS="$(cd "$ROOT/mac" && swift build --show-bin-path)/rohy-harness"
fi
cd "$ROOT/android"
./gradlew --console=plain ${GRADLE_ARGS:-} :core:test :core:koverVerify :core:koverLog :core:animalsnifferMain :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease
"$ROOT/scripts/check-r8-registrars.sh"

# Same Kotlin interop test against the Rust server (desktop), when a Rust toolchain is available.
if command -v cargo >/dev/null 2>&1; then
  (cd "$ROOT/desktop" && cargo build --quiet -p rohy-server --bin rohy-harness)
  ROHY_HARNESS="$ROOT/desktop/target/debug/rohy-harness" \
    ./gradlew --console=plain ${GRADLE_ARGS:-} :core:test --tests app.rohy.core.InteropTest --rerun
fi
