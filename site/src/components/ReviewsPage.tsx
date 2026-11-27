import { t, type Lang } from "@/content/i18n";
import { PublishedReviews } from "./PublishedReviews";
import { ReviewForm } from "./ReviewForm";
import { Shell } from "./Shell";

export function ReviewsPage({ lang }: { lang: Lang }) {
  const d = t(lang).reviews;
  return (
    <Shell lang={lang} page="reviews">
      <main className="reviews-main">
        <section className="card" aria-labelledby="review-title">
          <div className="stack" style={{ gap: 8 }}>
            <h1 id="review-title">{d.title}</h1>
            <p className="lead" style={{ fontSize: 16 }}>{d.lead}</p>
          </div>
          <ReviewForm lang={lang} />
        </section>
        <PublishedReviews lang={lang} />
      </main>
    </Shell>
  );
}
