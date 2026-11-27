import { formatDate, t, type Lang } from "@/content/i18n";
import { downloadUrl, LATEST, RELEASES, UPCOMING, type Platform } from "@/content/releases";
import { DownloadIcon } from "./Icons";
import { Shell } from "./Shell";

const ORDER: Platform[] = ["android", "macos", "windows", "linux"];

export function ReleasesPage({ lang }: { lang: Lang }) {
  const d = t(lang).releases;
  return (
    <Shell lang={lang} page="releases">
      <main className="releases-main">
        <div className="stack" style={{ gap: 8 }}>
          <h1 className="page-title">{d.title}</h1>
          <p className="lead" style={{ fontSize: 18 }}>{d.lead}</p>
        </div>

        {UPCOMING.length > 0 && (
          <article className="release release-soon">
            <div className="release-head">
              <span className="badge badge-sun">{d.soon}</span>
              <h2>{d.nextVersion}</h2>
            </div>
            <ul className="notes">
              {UPCOMING.map((note) => <li key={note.en}>{note[lang]}</li>)}
            </ul>
          </article>
        )}

        {RELEASES.map((release) => (
          <article key={release.version} className="release" aria-labelledby={`v${release.version}`}>
            <div className="release-head">
              {release === LATEST && <span className="badge badge-primary">{d.latest}</span>}
              <h2 id={`v${release.version}`}>Rohy {release.version}</h2>
              <time className="small" style={{ fontSize: 15 }} dateTime={release.date}>{formatDate(release.date, lang)}</time>
            </div>
            <ul className="notes">
              {release.notes.map((note) => <li key={note.en}>{note[lang]}</li>)}
            </ul>
            <div className="files">
              {ORDER.filter((p) => release.files[p]).map((p) => (
                <a
                  key={p}
                  href={downloadUrl(release.files[p]!)}
                  className="file"
                  aria-label={d.downloadFile(d.files[p], release.version)}
                  download
                >
                  <DownloadIcon size={16} />
                  {d.files[p]}
                </a>
              ))}
            </div>
          </article>
        ))}
      </main>
    </Shell>
  );
}
