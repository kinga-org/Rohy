"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import { t, type Lang } from "@/content/i18n";
import { BODY_MAX, BODY_MIN, DEVICES, NAME_MAX, type Devices, type ReviewError } from "../../worker/reviews";
import { CheckIcon, Star } from "./Icons";

/** Public Turnstile site key of rohy.kingalabs.com (not a secret; the secret is a Worker secret). */
const SITE_KEY = "0x4AAAAAAGVreG9DKVe0orlL";
/** Cloudflare's test key, always passing, for localhost where the real key is refused. */
const TEST_SITE_KEY = "1x00000000000000000000AA";
const siteKey = () => (window.location.hostname.endsWith("kingalabs.com") ? SITE_KEY : TEST_SITE_KEY);
const SCRIPT = "https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit";

interface Turnstile {
  render(el: HTMLElement, options: Record<string, unknown>): string;
  reset(id: string): void;
  remove(id: string): void;
}
declare global {
  interface Window {
    turnstile?: Turnstile;
  }
}

let scriptPromise: Promise<Turnstile> | null = null;
function loadTurnstile(): Promise<Turnstile> {
  if (window.turnstile) return Promise.resolve(window.turnstile);
  scriptPromise ??= new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.src = SCRIPT;
    script.async = true;
    script.onload = () => (window.turnstile ? resolve(window.turnstile) : reject(new Error("turnstile")));
    script.onerror = () => {
      scriptPromise = null;
      reject(new Error("turnstile"));
    };
    document.head.appendChild(script);
  });
  return scriptPromise;
}

type Status = "idle" | "sending" | "sent";
type FormError = ReviewError | "network" | "other";

export function ReviewForm({ lang }: { lang: Lang }) {
  const d = t(lang).reviews;
  const [status, setStatus] = useState<Status>("idle");
  const [error, setError] = useState<FormError | null>(null);
  const [rating, setRating] = useState(0);
  const [name, setName] = useState("");
  const [devices, setDevices] = useState<Devices>("mac");
  const [body, setBody] = useState("");
  const [consent, setConsent] = useState(false);
  const [token, setToken] = useState("");
  const widgetEl = useRef<HTMLDivElement>(null);
  const widgetId = useRef<string | null>(null);
  const errorRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (status === "sent") return;
    let cancelled = false;
    loadTurnstile()
      .then((ts) => {
        if (cancelled || !widgetEl.current || widgetId.current) return;
        widgetId.current = ts.render(widgetEl.current, {
          sitekey: siteKey(),
          language: lang,
          theme: "light",
          callback: (value: string) => setToken(value),
          "expired-callback": () => setToken(""),
          "error-callback": () => setToken(""),
        });
      })
      .catch(() => !cancelled && setError("token"));
    return () => {
      cancelled = true;
      if (widgetId.current && window.turnstile) window.turnstile.remove(widgetId.current);
      widgetId.current = null;
    };
  }, [lang, status]);

  useEffect(() => {
    if (error) errorRef.current?.focus();
  }, [error]);

  const message = (e: FormError) => (e in d.errors ? d.errors[e as keyof typeof d.errors] : d.errors.other);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (rating < 1) return setError("rating");
    if ([...body.trim()].length < BODY_MIN) return setError("body");
    if (!token) return setError("token");
    setError(null);
    setStatus("sending");
    try {
      const res = await fetch("/api/reviews", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ name, rating, devices, body, lang, canPublish: consent, token }),
      });
      if (res.ok) {
        setStatus("sent");
        return;
      }
      const data = (await res.json().catch(() => ({}))) as { error?: FormError };
      setError(data.error ?? "other");
    } catch {
      setError("network");
    }
    setStatus("idle");
    setToken("");
    if (widgetId.current) window.turnstile?.reset(widgetId.current);
  }

  if (status === "sent") {
    return (
      <div className="stack" style={{ gap: 16 }}>
        <div role="status" className="sent">
          <CheckIcon />
          <div className="stack" style={{ gap: 4 }}>
            <strong>{d.sentTitle}</strong>
            <span style={{ fontSize: 15, lineHeight: 1.5 }}>{d.sentText}</span>
          </div>
        </div>
        <button
          type="button"
          className="pill pill-tonal pill-small"
          style={{ alignSelf: "flex-start" }}
          onClick={() => {
            setRating(0);
            setBody("");
            setConsent(false);
            setToken("");
            setStatus("idle");
          }}
        >
          {d.sendAnother}
        </button>
      </div>
    );
  }

  const count = [...body].length;
  return (
    <form className="form" onSubmit={submit} noValidate>
      {error && (
        <div ref={errorRef} tabIndex={-1} role="alert" className="alert alert-error">
          {message(error)}
        </div>
      )}

      <label className="field">
        <span>{d.name} <span className="hint">{d.nameHint}</span></span>
        <input
          className="input"
          type="text"
          name="name"
          autoComplete="given-name"
          maxLength={NAME_MAX}
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
      </label>

      <fieldset className="stars-field" aria-invalid={error === "rating"}>
        <legend>{d.rating}</legend>
        <div className="stars">
          {[1, 2, 3, 4, 5].map((n) => (
            <label key={n} className={`star${n <= rating ? " on" : ""}`}>
              <input
                type="radio"
                name="rating"
                value={n}
                checked={rating === n}
                aria-label={d.star(n)}
                onChange={() => setRating(n)}
              />
              <Star on={n <= rating} size={26} outlined />
            </label>
          ))}
        </div>
      </fieldset>

      <label className="field">
        {d.devices}
        <select className="select" name="devices" value={devices} onChange={(e) => setDevices(e.target.value as Devices)}>
          {DEVICES.map((id) => <option key={id} value={id}>{d.deviceOptions[id]}</option>)}
        </select>
      </label>

      <label className="field">
        {d.body}
        <textarea
          className="textarea"
          name="body"
          rows={5}
          maxLength={BODY_MAX}
          required
          aria-invalid={error === "body"}
          value={body}
          onChange={(e) => setBody(e.target.value)}
        />
        <span className="count" aria-hidden="true">{d.bodyCount(count, BODY_MAX)}</span>
      </label>

      <div className="turnstile" aria-label={d.robot} role="group">
        <div ref={widgetEl} />
      </div>

      <label className="consent">
        <input type="checkbox" name="publish" checked={consent} onChange={(e) => setConsent(e.target.checked)} />
        {d.consent}
      </label>

      <button type="submit" className="pill pill-primary" disabled={status === "sending"}>
        {status === "sending" ? d.sending : d.submit}
      </button>
    </form>
  );
}
