#!/usr/bin/env bash
# Copies a GitHub release's files into the R2 bucket the site serves downloads from.
# The GitHub repo is private, so visitors cannot download from GitHub directly.
# Usage: site/scripts/upload-release.sh v0.1.4   (needs `gh auth login` and `npx wrangler login`)
set -euo pipefail
TAG="${1:?usage: upload-release.sh <tag, e.g. v0.1.4>}"
REPO="${ROHY_REPO:-kinga-org/Rohy}"
BUCKET="${ROHY_BUCKET:-rohy-downloads}"
SITE="$(cd "$(dirname "$0")/.." && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

gh release download "$TAG" --repo "$REPO" --dir "$WORK"
cd "$SITE"
for file in "$WORK"/Rohy-*; do
  name="$(basename "$file")"
  case "$name" in
    *.apk) type="application/vnd.android.package-archive" ;;
    *.zip) type="application/zip" ;;
    *.tar.gz) type="application/gzip" ;;
    *) echo "skip $name"; continue ;;
  esac
  echo "upload $name"
  npx wrangler r2 object put "$BUCKET/$name" --file "$file" --content-type "$type" --remote
done
