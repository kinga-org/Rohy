"use client";

import { useEffect, useState } from "react";
import { formatDate, t, type Lang } from "@/content/i18n";
import type { PublishedReview } from "../../worker/reviews";
import { Star } from "./Icons";

type State = { kind: "loading" } | { kind: "error" } | { kind: "ready"; reviews: PublishedReview[] };

/** Approved reviews, read from the Worker. Shows nothing invented: empty means empty. */
export function PublishedReviews({ lang }: { lang: Lang }) {
  const d = t(lang).reviews;
  const [state, setState] = useState<State>({ kind: "loading" });

  useEffect(() => {
    let alive = true;
    fetch("/api/reviews", { headers: { accept: "application/json" } })
      .then((res) => (res.ok ? res.json() : Promise.reject(new Error(String(res.status)))))
      .then((data: { reviews: PublishedReview[] }) => alive && setState({ kind: "ready", reviews: data.reviews }))
      .catch(() => alive && setState({ kind: "error" }));
    return () => {
      alive = false;
    };
  }, []);

  return (
    <section className="published" aria-labelledby="published-title" aria-busy={state.kind === "loading"}>
      <h2 id="published-title">{d.publishedTitle}</h2>
      {state.kind === "loading" && <p className="small">{d.loading}</p>}
      {state.kind === "error" && <p className="small">{d.loadError}</p>}
      {state.kind === "ready" && state.reviews.length === 0 && <p className="small">{d.empty}</p>}
      {state.kind === "ready" &&
        state.reviews.map((r, i) => {
          const name = r.name ?? d.anonymous;
          const tone = i % 2 === 0 ? "tile-soft" : "tile-sun";
          return (
            <article key={r.id} className="review">
              <div className="review-who">
                <span className={`tile avatar ${tone}`} aria-hidden="true">{[...name][0]?.toUpperCase()}</span>
                <div className="stack" style={{ gap: 2 }}>
                  <strong style={{ fontSize: 15 }}>{name}</strong>
                  <span className="review-meta">
                    {d.deviceOptions[r.devices]} · <time dateTime={r.createdAt}>{formatDate(r.createdAt, lang)}</time>
                  </span>
                </div>
              </div>
              <div className="review-stars" role="img" aria-label={d.ratingLabel(r.rating)}>
                {[1, 2, 3, 4, 5].map((n) => <Star key={n} on={n <= r.rating} />)}
              </div>
              <p>{r.body}</p>
            </article>
          );
        })}
    </section>
  );
}
