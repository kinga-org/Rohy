import { pathFor, t, type Lang } from "./i18n";
import { LATEST } from "./releases";

const SITE_URL = "https://rohy.kingalabs.com";

/**
 * schema.org description of the app for search engines. No rating on purpose: reviews are
 * moderated and few, and a made-up rating is not allowed.
 */
export function softwareApplication(lang: Lang) {
  const m = t(lang).meta;
  return {
    "@context": "https://schema.org",
    "@type": "SoftwareApplication",
    name: "Rohy",
    url: SITE_URL + pathFor(lang, "home"),
    description: m.homeDescription,
    inLanguage: lang,
    applicationCategory: "UtilitiesApplication",
    operatingSystem: "Android 10+, macOS 14+, Windows 10/11, Linux",
    softwareVersion: LATEST.version,
    datePublished: LATEST.date,
    downloadUrl: `${SITE_URL}${pathFor(lang, "home")}#download`,
    image: `${SITE_URL}/icon.svg`,
    offers: { "@type": "Offer", price: "0", priceCurrency: "EUR" },
    publisher: { "@type": "Organization", name: "Kinga", url: "https://kingalabs.com" },
  };
}
