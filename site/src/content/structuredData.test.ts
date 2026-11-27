import { describe, expect, it } from "vitest";
import { LATEST } from "./releases";
import { softwareApplication } from "./structuredData";

describe("softwareApplication", () => {
  it("describes the free app at the latest version, in the page language", () => {
    const en = softwareApplication("en");
    expect(en["@type"]).toBe("SoftwareApplication");
    expect(en.softwareVersion).toBe(LATEST.version);
    expect(en.offers.price).toBe("0");
    expect(en.url).toBe("https://rohy.kingalabs.com/");
    expect(softwareApplication("fr").url).toBe("https://rohy.kingalabs.com/fr/");
  });

  it("carries no rating, since none is invented", () => {
    expect(JSON.stringify(softwareApplication("en"))).not.toContain("aggregateRating");
  });
});
