import { isDownloadName, parseReview, REQUEST_MAX_BYTES, toPublished, type ReviewRow } from "./reviews";

export interface Env {
  ASSETS: Fetcher;
  DB: D1Database;
  DOWNLOADS: R2Bucket;
  TURNSTILE_SECRET: string;
}

const SITEVERIFY = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

function json(data: unknown, status = 200, headers: HeadersInit = {}): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", ...headers },
  });
}

async function listReviews(env: Env): Promise<Response> {
  const { results } = await env.DB.prepare(
    "SELECT id, name, rating, devices, body, created_at FROM reviews " +
      "WHERE status = 'approved' AND can_publish = 1 ORDER BY created_at DESC LIMIT 50",
  ).all<ReviewRow>();
  const reviews = results.map(toPublished).filter((r) => r !== null);
  return json({ reviews }, 200, { "cache-control": "public, max-age=60" });
}

async function verifyTurnstile(env: Env, token: string, ip: string | null): Promise<boolean> {
  const form = new FormData();
  form.append("secret", env.TURNSTILE_SECRET);
  form.append("response", token);
  if (ip) form.append("remoteip", ip);
  const res = await fetch(SITEVERIFY, { method: "POST", body: form });
  if (!res.ok) return false;
  const outcome = (await res.json()) as { success?: boolean };
  return outcome.success === true;
}

async function addReview(request: Request, env: Env): Promise<Response> {
  if (!request.headers.get("content-type")?.startsWith("application/json")) {
    return json({ error: "content-type" }, 415);
  }
  const declared = Number(request.headers.get("content-length") ?? "0");
  if (declared > REQUEST_MAX_BYTES) return json({ error: "too-large" }, 413);
  const text = await request.text();
  if (text.length > REQUEST_MAX_BYTES) return json({ error: "too-large" }, 413);

  let raw: unknown;
  try {
    raw = JSON.parse(text);
  } catch {
    return json({ error: "body" }, 400);
  }
  const parsed = parseReview(raw);
  if (!parsed.ok) return json({ error: parsed.error }, 400);
  const review = parsed.review;

  if (!(await verifyTurnstile(env, review.token, request.headers.get("cf-connecting-ip")))) {
    return json({ error: "token" }, 403);
  }

  await env.DB.prepare(
    "INSERT INTO reviews (name, rating, devices, body, lang, can_publish, status, created_at) " +
      "VALUES (?, ?, ?, ?, ?, ?, 'pending', ?)",
  )
    .bind(review.name, review.rating, review.devices, review.body, review.lang, review.canPublish ? 1 : 0, new Date().toISOString())
    .run();
  return json({ ok: true }, 201);
}

async function download(name: string, request: Request, env: Env): Promise<Response> {
  if (!isDownloadName(name)) return new Response("Not found", { status: 404 });
  const object = request.method === "HEAD" ? await env.DOWNLOADS.head(name) : await env.DOWNLOADS.get(name);
  if (object === null) return new Response("Not found", { status: 404 });
  const headers = new Headers();
  object.writeHttpMetadata(headers);
  if (!headers.has("content-type")) headers.set("content-type", "application/octet-stream");
  headers.set("etag", object.httpEtag);
  headers.set("content-length", String(object.size));
  headers.set("content-disposition", `attachment; filename="${name}"`);
  headers.set("cache-control", "public, max-age=3600");
  const body = "body" in object ? (object as R2ObjectBody).body : null;
  return new Response(body, { headers });
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);

    if (url.pathname === "/api/reviews" || url.pathname === "/api/reviews/") {
      if (request.method === "GET") return listReviews(env);
      if (request.method === "POST") return addReview(request, env);
      return new Response("Method not allowed", { status: 405, headers: { allow: "GET, POST" } });
    }

    if (url.pathname.startsWith("/download/")) {
      if (request.method !== "GET" && request.method !== "HEAD") {
        return new Response("Method not allowed", { status: 405, headers: { allow: "GET, HEAD" } });
      }
      return download(decodeURIComponent(url.pathname.slice("/download/".length)), request, env);
    }

    return env.ASSETS.fetch(request);
  },
} satisfies ExportedHandler<Env>;
