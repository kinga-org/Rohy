"""Reads `llvm-cov export -summary-only` JSON on stdin and enforces a minimum coverage per metric."""
import json
import sys

prefix, threshold = sys.argv[1], float(sys.argv[2])
files = [f for f in json.load(sys.stdin)["data"][0]["files"] if prefix in f["filename"]]
if not files:
    sys.exit(f"No covered files matched {prefix}")
failed = False
for metric in ("lines", "functions", "regions", "branches"):
    count = sum(f["summary"][metric]["count"] for f in files)
    covered = sum(f["summary"][metric]["covered"] for f in files)
    if count == 0:
        print(f"{metric:>9}: n/a (not instrumented; regions cover branch-level code paths)")
        continue
    percent = 100.0 if count == 0 else 100.0 * covered / count
    status = "ok" if percent >= threshold else "FAIL"
    failed |= percent < threshold
    print(f"{metric:>9}: {percent:6.2f}% ({covered}/{count}) {status}")
sys.exit(1 if failed else 0)
