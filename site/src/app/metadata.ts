import type { Metadata } from "next";
import { pathFor, t, type Lang, type Page } from "@/content/i18n";

export const SITE_URL = "https://rohy.kingalabs.com";

/** Title, description and language links for one page in one language. */
export function pageMetadata(lang: Lang, page: Page): Metadata {
  const m = t(lang).meta;
  const text: Record<Page, { title: string; description: string }> = {
    home: { title: m.homeTitle, description: m.homeDescription },
    releases: { title: m.releasesTitle, description: m.releasesDescription },
    reviews: { title: m.reviewsTitle, description: m.reviewsDescription },
  };
  const { title, description } = text[page];
  return {
    metadataBase: new URL(SITE_URL),
    title,
    description,
    icons: { icon: "/icon.svg" },
    alternates: {
      canonical: pathFor(lang, page),
      languages: { en: pathFor("en", page), fr: pathFor("fr", page), "x-default": pathFor("en", page) },
    },
    openGraph: { title, description, url: pathFor(lang, page), siteName: "Rohy", locale: lang === "fr" ? "fr_FR" : "en_US", type: "website" },
  };
}
