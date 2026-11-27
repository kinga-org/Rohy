"use client";

import Link from "next/link";
import { useEffect, useId, useRef, useState } from "react";
import { pathFor, t, type Lang, type Page } from "@/content/i18n";
import { Logo, MenuIcon } from "./Icons";

/** Site header. On phones the links move into a menu so the bar stays on one line. */
export function Header({ lang, page }: { lang: Lang; page: Page }) {
  const d = t(lang).nav;
  const [open, setOpen] = useState(false);
  const panelId = useId();
  const buttonRef = useRef<HTMLButtonElement>(null);
  const other: Lang = lang === "en" ? "fr" : "en";
  const download = `${pathFor(lang, "home")}#download`;

  useEffect(() => {
    if (!open) return;
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setOpen(false);
        buttonRef.current?.focus();
      }
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [open]);

  const current = (p: Page) => (p === page ? "page" : undefined);
  const close = () => setOpen(false);

  return (
    <header className="header">
      <Link href={pathFor(lang, "home")} className="brand">
        <Logo />
        <span className="brand-name">Rohy</span>
      </Link>
      <span className="spacer" />
      <nav aria-label={d.label} className="links">
        <Link href={download}>{d.download}</Link>
        <Link href={pathFor(lang, "releases")} aria-current={current("releases")}>{d.releases}</Link>
        <Link href={pathFor(lang, "reviews")} aria-current={current("reviews")}>{d.reviews}</Link>
      </nav>
      <Link href={download} className="pill pill-primary cta">{d.download}</Link>
      <button
        ref={buttonRef}
        type="button"
        className="menu-button"
        aria-label={open ? d.closeMenu : d.menu}
        aria-expanded={open}
        aria-controls={panelId}
        onClick={() => setOpen((v) => !v)}
      >
        <MenuIcon open={open} />
      </button>
      {open && (
        <nav id={panelId} aria-label={d.label} className="menu-panel">
          <Link href={download} onClick={close}>{d.download}</Link>
          <Link href={pathFor(lang, "releases")} aria-current={current("releases")} onClick={close}>{d.releases}</Link>
          <Link href={pathFor(lang, "reviews")} aria-current={current("reviews")} onClick={close}>{d.reviews}</Link>
          <hr />
          <Link href={pathFor(other, page)} hrefLang={other} lang={other} aria-label={d.otherLanguageLabel} onClick={close}>
            {d.otherLanguage}
          </Link>
        </nav>
      )}
    </header>
  );
}
