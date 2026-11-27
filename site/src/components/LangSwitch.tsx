import Link from "next/link";
import { LANGS, pathFor, type Lang, type Page } from "@/content/i18n";

const NAMES: Record<Lang, string> = { en: "English", fr: "Français" };

/** Two-choice EN | FR switch. Keeps the same page in the other language. */
export function LangSwitch({
  lang,
  page,
  label,
  className = "",
  onPick,
}: {
  lang: Lang;
  page: Page;
  label: string;
  className?: string;
  onPick?: () => void;
}) {
  return (
    <div role="group" aria-label={label} className={`lang ${className}`.trim()}>
      {LANGS.map((l) => (
        <Link
          key={l}
          href={pathFor(l, page)}
          hrefLang={l}
          lang={l}
          title={NAMES[l]}
          aria-label={NAMES[l]}
          aria-current={l === lang ? "true" : undefined}
          onClick={onPick}
        >
          {l.toUpperCase()}
        </Link>
      ))}
    </div>
  );
}
