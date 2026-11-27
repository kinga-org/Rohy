import { describe, expect, it } from "vitest";
import { BODY_MAX, clean, isDownloadName, NAME_MAX, parseReview, toPublished } from "./reviews";

const valid = {
  name: "Safi",
  rating: 5,
  devices: "mac",
  body: "Works great between my phone and my Mac.",
  lang: "en",
  canPublish: true,
  token: "turnstile-token",
};

describe("parseReview", () => {
  it("accepts a full review", () => {
    const parsed = parseReview(valid);
    expect(parsed).toEqual({ ok: true, review: { ...valid, devices: "mac", lang: "en" } });
  });

  it("treats an empty or missing name as anonymous", () => {
    for (const name of ["", "   ", undefined]) {
      const parsed = parseReview({ ...valid, name });
      expect(parsed.ok && parsed.review.name).toBeNull();
    }
  });

  it("rejects a name that is too long", () => {
    expect(parseReview({ ...valid, name: "a".repeat(NAME_MAX + 1) })).toEqual({ ok: false, error: "name" });
    expect(parseReview({ ...valid, name: "a".repeat(NAME_MAX) }).ok).toBe(true);
  });

  it.each([0, 6, 2.5, "4", null])("rejects rating %s", (rating) => {
    expect(parseReview({ ...valid, rating })).toEqual({ ok: false, error: "rating" });
  });

  it("only knows the three computer systems", () => {
    expect(parseReview({ ...valid, devices: "ios" })).toEqual({ ok: false, error: "devices" });
    for (const devices of ["mac", "windows", "linux"]) expect(parseReview({ ...valid, devices }).ok).toBe(true);
  });

  it("needs a body between the limits once trimmed", () => {
    expect(parseReview({ ...valid, body: "   short   " })).toEqual({ ok: false, error: "body" });
    expect(parseReview({ ...valid, body: "x".repeat(BODY_MAX + 1) })).toEqual({ ok: false, error: "body" });
    expect(parseReview({ ...valid, body: 42 })).toEqual({ ok: false, error: "body" });
  });

  it("needs a known language, an explicit consent answer and a token", () => {
    expect(parseReview({ ...valid, lang: "de" })).toEqual({ ok: false, error: "lang" });
    expect(parseReview({ ...valid, canPublish: "yes" })).toEqual({ ok: false, error: "consent" });
    expect(parseReview({ ...valid, token: "" })).toEqual({ ok: false, error: "token" });
  });

  it("keeps a review the person does not want published, flagged as private", () => {
    const parsed = parseReview({ ...valid, canPublish: false });
    expect(parsed.ok && parsed.review.canPublish).toBe(false);
  });

  it("rejects things that are not objects", () => {
    expect(parseReview(null).ok).toBe(false);
    expect(parseReview("hello").ok).toBe(false);
  });
});

describe("clean", () => {
  it("drops control characters and collapses blank lines", () => {
    expect(clean("a\u0007b\n\n\n\nc\r", true)).toBe("ab\n\nc");
    expect(clean(" Sa\nfi ", false)).toBe("Safi");
  });
});

describe("toPublished", () => {
  it("maps a row and drops rows with unknown devices", () => {
    const row = { id: 1, name: null, rating: 4, devices: "linux", body: "Nice app for Linux.", created_at: "2026-11-27T10:00:00Z" };
    expect(toPublished(row)).toEqual({ id: 1, name: null, rating: 4, devices: "linux", body: "Nice app for Linux.", createdAt: "2026-11-27T10:00:00Z" });
    expect(toPublished({ ...row, devices: "bsd" })).toBeNull();
  });
});

describe("isDownloadName", () => {
  it("accepts release file names", () => {
    expect(isDownloadName("Rohy-macOS-0.1.4.zip")).toBe(true);
    expect(isDownloadName("Rohy-android-0.1.4-test-signature.apk")).toBe(true);
  });

  it.each(["../secret", "Rohy-../x", "rohy.zip", "Rohy-a/b.zip", "Rohy-", ""])("rejects %s", (name) => {
    expect(isDownloadName(name)).toBe(false);
  });
});
