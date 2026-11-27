import { describe, expect, it } from "vitest";
import { DICTS, formatDate, pathFor } from "./i18n";

describe("pathFor", () => {
  it("puts English at the root and French under /fr", () => {
    expect(pathFor("en", "home")).toBe("/");
    expect(pathFor("en", "releases")).toBe("/releases/");
    expect(pathFor("fr", "home")).toBe("/fr/");
    expect(pathFor("fr", "reviews")).toBe("/fr/reviews/");
  });
});

describe("dictionaries", () => {
  it("have the same lists in both languages", () => {
    expect(DICTS.fr.home.features).toHaveLength(DICTS.en.home.features.length);
    expect(DICTS.fr.home.steps).toHaveLength(DICTS.en.home.steps.length);
  });

  it("have no empty text", () => {
    const walk = (value: unknown): void => {
      if (typeof value === "string") expect(value.trim()).not.toBe("");
      else if (value && typeof value === "object") Object.values(value).forEach(walk);
    };
    walk(DICTS);
  });
});

describe("formatDate", () => {
  it("writes the date in the page language", () => {
    expect(formatDate("2026-11-19", "en")).toBe("November 19, 2026");
    expect(formatDate("2026-11-19", "fr")).toBe("19 novembre 2026");
  });
});
