#!/usr/bin/env bash
# Showcase website (site/): types, tests (review rules, release data, texts) and the static build.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/site"
export NEXT_TELEMETRY_DISABLED=1
npm ci
npx vitest run
npx next build   # also writes next-env.d.ts, which the type check below needs
npx tsc --noEmit
npx tsc -p worker --noEmit
test -f out/index.html && test -f out/fr/index.html && test -f out/404.html
