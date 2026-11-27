import type { ReactNode } from "react";
import type { Lang, Page } from "@/content/i18n";
import { Footer } from "./Footer";
import { Header } from "./Header";

const SKIP: Record<Lang, string> = { en: "Skip to content", fr: "Aller au contenu" };

/** Header, page content and footer, the same on every page. */
export function Shell({ lang, page, children }: { lang: Lang; page: Page; children: ReactNode }) {
  return (
    <>
      <a href="#content" className="skip">{SKIP[lang]}</a>
      <Header lang={lang} page={page} />
      <div id="content">{children}</div>
      <Footer lang={lang} page={page} />
    </>
  );
}
