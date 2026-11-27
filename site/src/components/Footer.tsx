import Link from "next/link";
import { pathFor, t, type Lang, type Page } from "@/content/i18n";
import { Logo } from "./Icons";

export function Footer({ lang, page }: { lang: Lang; page: Page }) {
  const d = t(lang);
  const other: Lang = lang === "en" ? "fr" : "en";
  return (
    <footer className="footer">
      <div className="footer-inner">
        <span className="footer-brand"><Logo size={24} />{d.footer.by}</span>
        <Link href={pathFor(lang, "releases")}>{d.nav.releases}</Link>
        <Link href={pathFor(lang, "reviews")}>{d.nav.reviews}</Link>
        <Link href={pathFor(other, page)} hrefLang={other} lang={other} aria-label={d.nav.otherLanguageLabel}>
          {d.nav.otherLanguage}
        </Link>
        <span className="footer-end">{d.footer.copyright}</span>
      </div>
    </footer>
  );
}
