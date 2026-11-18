#!/usr/bin/env bash
# Swift tests + coverage threshold on RohyCore (protocol, crypto, session logic).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/mac"
THRESHOLD="${COVERAGE_THRESHOLD:-80}"
swift build
swift test --enable-code-coverage
python3 "$ROOT/scripts/coverage_gate.py" "Sources/RohyCore/" "$THRESHOLD" < "$(swift test --show-codecov-path)"
