/** Review form rules, shared by the Worker and its tests. No Cloudflare APIs here. */

export const DEVICES = ["mac", "windows", "linux"] as const;
export type Devices = (typeof DEVICES)[number];
export const LANGS = ["en", "fr"] as const;
export type Lang = (typeof LANGS)[number];

export const NAME_MAX = 40;
export const BODY_MIN = 10;
export const BODY_MAX = 2000;
/** Largest request body we read; a full review is far below this. */
export const REQUEST_MAX_BYTES = 16 * 1024;

export interface ReviewInput {
  name: string | null;
  rating: number;
  devices: Devices;
  body: string;
  lang: Lang;
  canPublish: boolean;
  token: string;
}

export type ReviewError = "name" | "rating" | "devices" | "body" | "lang" | "consent" | "token";

export type Parsed = { ok: true; review: ReviewInput } | { ok: false; error: ReviewError };

/** Removes control characters (keeps new lines in the body) and trims. */
export function clean(text: string, keepNewLines: boolean): string {
  const pattern = keepNewLines ? /[\u0000-\u0009\u000B-\u001F\u007F]/g : /[\u0000-\u001F\u007F]/g;
  return text.replace(pattern, "").replace(/\n{3,}/g, "\n\n").trim();
}

export function parseReview(raw: unknown): Parsed {
  if (typeof raw !== "object" || raw === null) return { ok: false, error: "body" };
  const data = raw as Record<string, unknown>;

  const nameRaw = data.name ?? "";
  if (typeof nameRaw !== "string") return { ok: false, error: "name" };
  const name = clean(nameRaw, false);
  if ([...name].length > NAME_MAX) return { ok: false, error: "name" };

  const rating = data.rating;
  if (typeof rating !== "number" || !Number.isInteger(rating) || rating < 1 || rating > 5) {
    return { ok: false, error: "rating" };
  }

  const devices = data.devices;
  if (typeof devices !== "string" || !(DEVICES as readonly string[]).includes(devices)) {
    return { ok: false, error: "devices" };
  }

  if (typeof data.body !== "string") return { ok: false, error: "body" };
  const body = clean(data.body, true);
  const length = [...body].length;
  if (length < BODY_MIN || length > BODY_MAX) return { ok: false, error: "body" };

  const lang = data.lang;
  if (typeof lang !== "string" || !(LANGS as readonly string[]).includes(lang)) {
    return { ok: false, error: "lang" };
  }

  if (typeof data.canPublish !== "boolean") return { ok: false, error: "consent" };

  const token = data.token;
  if (typeof token !== "string" || token.length === 0 || token.length > 4096) {
    return { ok: false, error: "token" };
  }

  return {
    ok: true,
    review: {
      name: name.length > 0 ? name : null,
      rating,
      devices: devices as Devices,
      body,
      lang: lang as Lang,
      canPublish: data.canPublish,
      token,
    },
  };
}

/** A review as the site shows it. */
export interface PublishedReview {
  id: number;
  name: string | null;
  rating: number;
  devices: Devices;
  body: string;
  createdAt: string;
}

export interface ReviewRow {
  id: number;
  name: string | null;
  rating: number;
  devices: string;
  body: string;
  created_at: string;
}

export function toPublished(row: ReviewRow): PublishedReview | null {
  if (!(DEVICES as readonly string[]).includes(row.devices)) return null;
  return {
    id: row.id,
    name: row.name,
    rating: row.rating,
    devices: row.devices as Devices,
    body: row.body,
    createdAt: row.created_at,
  };
}

/** Download names we serve from the bucket, e.g. Rohy-macOS-0.1.4.zip. Blocks paths and odd keys. */
export function isDownloadName(name: string): boolean {
  return /^Rohy-[A-Za-z0-9][A-Za-z0-9._-]{0,120}$/.test(name) && !name.includes("..");
}
