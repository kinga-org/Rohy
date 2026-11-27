import Link from "next/link";
import { pathFor, t, type Lang } from "@/content/i18n";
import { downloadUrl, LATEST, type Platform } from "@/content/releases";
import { softwareApplication } from "@/content/structuredData";
import { DownloadIcon, Duo, PlatformIcon } from "./Icons";
import { Shell } from "./Shell";

const PLATFORMS: { id: Platform; tone: "soft" | "sun" }[] = [
  { id: "android", tone: "soft" },
  { id: "macos", tone: "sun" },
  { id: "windows", tone: "soft" },
  { id: "linux", tone: "sun" },
];

export function HomePage({ lang }: { lang: Lang }) {
  const d = t(lang).home;
  const version = LATEST.version;
  return (
    <Shell lang={lang} page="home">
      <main>
        <script
          type="application/ld+json"
          // Built from our own constants only, so no user text can reach this script.
          dangerouslySetInnerHTML={{ __html: JSON.stringify(softwareApplication(lang)).replace(/</g, "\\u003c") }}
        />
        <section className="hero">
          <div className="hero-text">
            <span className="chip chip-sun">{d.badge}</span>
            <h1>{d.title}</h1>
            <p className="hero-lead">{d.lead}</p>
            <div className="row">
              <a href="#download" className="pill pill-primary"><DownloadIcon />{d.downloadFree}</a>
              <Link href={pathFor(lang, "releases")} className="pill pill-tonal">{d.seeReleases}</Link>
            </div>
            <span className="small">{d.versionLine(version)}</span>
          </div>
          <div className="hero-art">
            <Duo label={d.illustration} />
            <span className="chip chip-ok"><span className="dot" />{d.sentChip}</span>
          </div>
        </section>

        <section id="download" className="section downloads" aria-labelledby="download-title">
          <div className="wrap stack" style={{ gap: 32 }}>
            <div className="stack" style={{ gap: 8 }}>
              <h2 id="download-title" className="section-title">{d.downloadTitle}</h2>
              <p className="lead">{d.downloadLead(version)}</p>
            </div>
            <div className="grid-cards">
              {PLATFORMS.map(({ id, tone }) => {
                const p = d.platforms[id];
                const file = LATEST.files[id];
                return (
                  <article key={id} className="platform">
                    <span className={`tile tile-${tone}`}><PlatformIcon platform={id} /></span>
                    <div className="stack" style={{ gap: 4 }}>
                      <h3>{p.name}</h3>
                      <span className="needs">{p.needs}</span>
                    </div>
                    {file && <a href={downloadUrl(file)} className="pill pill-primary pill-small" download>{p.button}</a>}
                    <span className="note">{p.note}</span>
                  </article>
                );
              })}
            </div>
          </div>
        </section>

        <section className="section" aria-labelledby="why-title">
          <div className="wrap stack" style={{ gap: 32 }}>
            <h2 id="why-title" className="section-title">{d.whyTitle}</h2>
            <div className="grid-cards">
              {d.features.map((f, i) => (
                <article key={f.title} className="feature">
                  <span className={`tile ${i % 2 === 0 ? "tile-soft" : "tile-sun"}`} aria-hidden="true">{f.mark}</span>
                  <h3>{f.title}</h3>
                  <p>{f.text}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section className="section steps-band" aria-labelledby="steps-title">
          <div className="wrap stack" style={{ gap: 32 }}>
            <h2 id="steps-title" className="section-title">{d.stepsTitle}</h2>
            <ol className="steps">
              {d.steps.map((step, i) => (
                <li key={step}><span className="step-num" aria-hidden="true">{i + 1}</span><span>{step}</span></li>
              ))}
            </ol>
          </div>
        </section>

        <section className="section">
          <div className="wrap">
            <div className="review-cta">
              <div className="stack" style={{ gap: 8, maxWidth: 620 }}>
                <h2>{d.reviewTitle}</h2>
                <p>{d.reviewLead}</p>
              </div>
              <Link href={pathFor(lang, "reviews")} className="pill pill-sun">{d.reviewButton}</Link>
            </div>
          </div>
        </section>
      </main>
    </Shell>
  );
}
