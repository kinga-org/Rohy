import type { MetadataRoute } from "next";
import { pathFor, type Page } from "@/content/i18n";
import { LATEST } from "@/content/releases";
import { SITE_URL } from "./metadata";

export const dynamic = "force-static";

const PAGES: Page[] = ["home", "releases", "reviews"];

/** Every page in both languages, each pointing at its translation. */
export default function sitemap(): MetadataRoute.Sitemap {
  return PAGES.flatMap((page) =>
    (["en", "fr"] as const).map((lang) => ({
      url: SITE_URL + pathFor(lang, page),
      lastModified: page === "reviews" ? undefined : LATEST.date,
      changeFrequency: page === "home" ? ("weekly" as const) : ("monthly" as const),
      priority: page === "home" ? 1 : 0.7,
      alternates: {
        languages: { en: SITE_URL + pathFor("en", page), fr: SITE_URL + pathFor("fr", page) },
      },
    })),
  );
}
