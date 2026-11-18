#!/usr/bin/env bash
# Installs the repository-managed Git hooks.
set -euo pipefail
cd "$(dirname "$0")/.."
git config core.hooksPath .githooks
chmod +x .githooks/* scripts/*.sh mac/scripts/*.sh
echo "Git hooks installed (.githooks)."
