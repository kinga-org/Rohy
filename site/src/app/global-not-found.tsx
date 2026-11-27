import type { Metadata } from "next";
import { inter } from "./fonts";
import "./globals.css";

export const metadata: Metadata = { title: "Rohy: page not found", robots: { index: false } };

/** 404 for any unknown address, in both languages since the path may not tell which one. */
export default function GlobalNotFound() {
  return (
    <html lang="en" className={inter.variable}>
      <body>
        <main className="missing">
          <a href="/" className="brand" aria-label="Rohy">
            <svg width="44" height="44" viewBox="0 0 100 100" aria-hidden="true">
              <rect width="100" height="100" rx="28" fill="#1B6E85" />
              <path d="M27 67 C 27 36, 50 32, 50 50 S 73 64, 73 33" fill="none" stroke="#FFFFFF" strokeWidth="10" strokeLinecap="round" />
              <circle cx="27" cy="69" r="9" fill="#F5B71E" />
              <circle cx="73" cy="31" r="9" fill="#F5B71E" />
            </svg>
          </a>
          <h1 className="page-title">This page doesn’t exist</h1>
          <p className="lead">The link may be old or mistyped.</p>
          <p className="lead" lang="fr">Cette page n’existe pas. Le lien est peut-être ancien ou mal tapé.</p>
          <div className="row">
            <a href="/" className="pill pill-primary">Back to home</a>
            <a href="/fr/" className="pill pill-tonal" lang="fr">Retour à l’accueil</a>
          </div>
        </main>
      </body>
    </html>
  );
}
