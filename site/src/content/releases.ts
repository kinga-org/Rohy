/**
 * Release notes shown on the site. Add the new release at the top when it is published on GitHub,
 * and upload its files to the R2 bucket (see README.md).
 */

export type Platform = "android" | "macos" | "windows" | "linux";

export interface Text {
  en: string;
  fr: string;
}

export interface Release {
  version: string;
  /** ISO date of the GitHub release. */
  date: string;
  notes: Text[];
  files: Partial<Record<Platform, string>>;
}

/** Up to 0.1.4 the APK was signed with a throwaway key, and its name says so. */
const files = (v: string, { linux = true, testSignature = false } = {}): Release["files"] => ({
  android: `Rohy-android-${v}${testSignature ? "-test-signature" : ""}.apk`,
  macos: `Rohy-macOS-${v}.zip`,
  windows: `Rohy-windows-x64-${v}.zip`,
  ...(linux ? { linux: `Rohy-linux-x64-${v}.tar.gz` } : {}),
});

export const RELEASES: Release[] = [
  {
    version: "0.2.0",
    date: "2026-11-27",
    files: files("0.2.0"),
    notes: [
      {
        en: "Android: the app is now signed with its own key. Uninstall 0.1.x first, then pair again.",
        fr: "Android : l’app est maintenant signée avec sa propre clé. Désinstalle la 0.1.x d’abord, puis associe à nouveau.",
      },
      {
        en: "New look: the “rope” logo and Lagoon & Sun colors, in every app.",
        fr: "Nouvelle identité : logo « La corde », couleurs Lagon & Soleil, sur toutes les apps.",
      },
      {
        en: "Android: a handle on the edge of the screen sends text from any app, even a game. Long-press it to move it, on either side.",
        fr: "Android : poignée sur le bord de l’écran pour envoyer depuis n’importe quelle app, même un jeu. Appui long pour la déplacer, des deux côtés.",
      },
      {
        en: "Android: the Quick Settings tile button turns into “Remove” when the tile is already there.",
        fr: "Android : le bouton de la tuile des réglages rapides devient « Retirer » quand elle est déjà là.",
      },
    ],
  },
  {
    version: "0.1.4",
    date: "2026-11-19",
    files: files("0.1.4", { testSignature: true }),
    notes: [
      {
        en: "Android 10: the app no longer closes while pairing (Huawei P30 Pro).",
        fr: "Android 10 : l’app ne se ferme plus à l’association (Huawei P30 Pro).",
      },
      {
        en: "Huawei and Honor: “Keep the connection” settings adapted.",
        fr: "Huawei et Honor : réglages de « Garder la connexion » adaptés.",
      },
    ],
  },
  {
    version: "0.1.3",
    date: "2026-11-19",
    files: files("0.1.3", { testSignature: true }),
    notes: [{ en: "New app for Linux (x86_64).", fr: "Nouvelle app pour Linux (x86_64)." }],
  },
  {
    version: "0.1.2",
    date: "2026-11-19",
    files: files("0.1.2", { linux: false, testSignature: true }),
    notes: [
      {
        en: "Mac: real system menu in the menu bar, native pairing window.",
        fr: "Mac : vrai menu du système dans la barre des menus, fenêtre d’association native.",
      },
      {
        en: "Android: scanning the QR code no longer crashes.",
        fr: "Android : le scan du QR code ne plante plus.",
      },
    ],
  },
];

/** Changes merged on main but not released yet. Empty it when they ship. */
export const UPCOMING: Text[] = [];

export const LATEST = RELEASES[0];

export function downloadUrl(file: string): string {
  return `/download/${encodeURIComponent(file)}`;
}
