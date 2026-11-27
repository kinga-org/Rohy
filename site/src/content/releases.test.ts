import { describe, expect, it } from "vitest";
import { isDownloadName } from "../../worker/reviews";
import { downloadUrl, LATEST, RELEASES } from "./releases";

describe("releases", () => {
  it("lists the newest release first", () => {
    const dates = RELEASES.map((r) => r.date);
    expect([...dates].sort().reverse()).toEqual(dates);
    expect(LATEST).toBe(RELEASES[0]);
  });

  it("names every file the way the Worker accepts and the release workflow builds it", () => {
    for (const release of RELEASES) {
      for (const file of Object.values(release.files)) {
        expect(isDownloadName(file!)).toBe(true);
        expect(file).toContain(release.version);
      }
    }
  });

  it("offers all four systems in the latest release", () => {
    expect(Object.keys(LATEST.files).sort()).toEqual(["android", "linux", "macos", "windows"]);
  });

  it("names the APK -test-signature only before the release key (0.2.0)", () => {
    for (const release of RELEASES) {
      const old = release.version.startsWith("0.1.");
      expect(release.files.android!.endsWith("-test-signature.apk")).toBe(old);
    }
  });

  it("builds download links on the site's own /download path", () => {
    expect(downloadUrl("Rohy-macOS-0.1.4.zip")).toBe("/download/Rohy-macOS-0.1.4.zip");
  });
});
