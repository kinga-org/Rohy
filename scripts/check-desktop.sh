#!/usr/bin/env bash
# Rust desktop crates: format, lints, tests and coverage thresholds (lines, functions, regions).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/desktop"
THRESHOLD="${COVERAGE_THRESHOLD:-80}"
cargo fmt --all -- --check
cargo clippy --workspace --all-targets -- -D warnings
if cargo llvm-cov --version >/dev/null 2>&1; then
  # Windows reports paths with backslashes: the filter accepts both separators.
  cargo llvm-cov --workspace --ignore-filename-regex 'src[/\\]bin[/\\]|rohy-desktop[/\\]src[/\\](main|platform)\.rs' --summary-only \
    --fail-under-lines "$THRESHOLD" --fail-under-functions "$THRESHOLD" --fail-under-regions "$THRESHOLD"
else
  echo "cargo-llvm-cov missing (cargo install cargo-llvm-cov): running tests without coverage gate" >&2
  cargo test --workspace
fi
