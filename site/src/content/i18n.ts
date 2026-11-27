/** Every text on the site, in English (default, at /) and French (at /fr). */

export type Lang = "en" | "fr";
export const LANGS: Lang[] = ["en", "fr"];

export type Page = "home" | "releases" | "reviews";

/** Path of a page in a language, with the trailing slash the static export uses. */
export function pathFor(lang: Lang, page: Page): string {
  const base = lang === "en" ? "/" : "/fr/";
  return page === "home" ? base : `${base}${page}/`;
}

const en = {
  meta: {
    homeTitle: "Rohy: copy on your phone, paste on your computer",
    homeDescription:
      "Rohy links your Android phone and your computer. Text you copy on one arrives on the other, over your Wi-Fi, end-to-end encrypted. No account, no cloud.",
    releasesTitle: "What’s new in Rohy",
    releasesDescription: "What changes in each version of Rohy, and the files to download.",
    reviewsTitle: "Reviews of Rohy",
    reviewsDescription: "Tell us what works and what doesn’t. Reviews are published after they are read.",
    notFoundTitle: "Page not found",
  },
  nav: {
    label: "Main",
    download: "Download",
    releases: "What’s new",
    reviews: "Reviews",
    menu: "Menu",
    closeMenu: "Close menu",
    otherLanguage: "Français",
    otherLanguageLabel: "Lire en français",
    language: "Language",
  },
  home: {
    badge: "Android ⇄ Mac, Windows, Linux",
    title: "Copy on your phone. Paste on your computer.",
    lead: "Rohy links your Android and your computer. Text you copy on one arrives on the other, over your Wi-Fi, end-to-end encrypted. No account, no cloud.",
    downloadFree: "Download for free",
    seeReleases: "See what’s new",
    versionLine: (v: string) => `Version ${v} · free`,
    illustration: "A phone and a computer smiling at each other, linked by a heart",
    sentChip: "Text sent to the computer",
    downloadTitle: "Download Rohy",
    downloadLead: (v: string) => `Install the app on your phone and on your computer. Version ${v}.`,
    platforms: {
      android: {
        name: "Android",
        needs: "Android 10 or newer.",
        button: "Download the APK",
        note: "Android will ask you to allow the install.",
      },
      macos: {
        name: "macOS",
        needs: "macOS 14 or newer, Apple silicon or Intel. Lives in the menu bar.",
        button: "Download for Mac",
        note: "A .zip file: open it, then drag Rohy into Applications.",
      },
      windows: {
        name: "Windows",
        needs: "Windows 10 or 11, 64-bit. Lives next to the clock.",
        button: "Download for Windows",
        note: "A .zip file with Rohy.exe.",
      },
      linux: {
        name: "Linux",
        needs: "x86_64, Ubuntu 22.04 or similar (glibc 2.35+).",
        button: "Download for Linux",
        note: "A .tar.gz archive with the rohy program.",
      },
    },
    whyTitle: "Why Rohy",
    features: [
      { mark: "E2E", title: "End-to-end encrypted", text: "Only your phone and your computer can read what you copy." },
      { mark: "Wi-Fi", title: "On your Wi-Fi, no cloud", text: "Text travels straight across your local network. Nothing is stored anywhere else." },
      { mark: "0", title: "No account", text: "No sign-up: you pair your devices with a QR code." },
      { mark: "⇢", title: "From any app", text: "The handle on the edge of the screen sends text even from a full-screen game." },
    ],
    stepsTitle: "Ready in three steps",
    steps: [
      "Install Rohy on your computer and on your Android.",
      "On the computer, choose “Pair a phone”, then scan the QR code with the app.",
      "Copy on one side, paste on the other. That’s it.",
    ],
    reviewTitle: "A review, an idea, a problem?",
    reviewLead: "Tell us what works and what doesn’t. Every message is read.",
    reviewButton: "Give my review",
  },
  releases: {
    title: "What’s new",
    lead: "What changes in each version of Rohy, and the files to download.",
    soon: "Coming soon",
    nextVersion: "Next version",
    latest: "Latest version",
    files: { android: "Android (APK)", macos: "macOS", windows: "Windows", linux: "Linux" },
    downloadFile: (name: string, version: string) => `Download ${name}, version ${version}`,
  },
  reviews: {
    title: "Your review of Rohy",
    lead: "What works, what doesn’t, an idea. Reviews are published after they are read.",
    name: "Your first name",
    nameHint: "Optional. Leave it empty to stay anonymous.",
    rating: "Your rating",
    star: (n: number) => (n === 1 ? "1 star" : `${n} stars`),
    devices: "You use Rohy with",
    deviceOptions: { mac: "Android and Mac", windows: "Android and Windows", linux: "Android and Linux" },
    body: "Your review",
    bodyCount: (n: number, max: number) => `${n} / ${max}`,
    robot: "Anti-robot check",
    consent: "I agree that my first name (if filled in) and my review are published on this site.",
    submit: "Send my review",
    sending: "Sending…",
    sentTitle: "Thank you, it’s sent!",
    sentText: "Your review will be read, then published here if it follows the rules.",
    sendAnother: "Send another review",
    errors: {
      rating: "Pick a rating from 1 to 5 stars.",
      body: "Write at least 10 characters.",
      token: "The anti-robot check didn’t pass. Try again.",
      network: "Couldn’t send your review. Check your connection and try again.",
      other: "Something is wrong in the form. Check it and try again.",
    },
    publishedTitle: "Published reviews",
    loading: "Loading reviews…",
    empty: "No published reviews yet. Yours could be the first.",
    loadError: "Reviews can’t be loaded right now.",
    anonymous: "Anonymous",
    ratingLabel: (n: number) => `Rating: ${n} out of 5`,
  },
  footer: {
    by: "Rohy, by Kinga",
    copyright: "© 2026 Kinga",
  },
  notFound: {
    title: "This page doesn’t exist",
    text: "The link may be old or mistyped.",
    home: "Back to home",
  },
};

export type Dict = typeof en;

const fr: Dict = {
  meta: {
    homeTitle: "Rohy : copie sur ton téléphone, colle sur ton ordinateur",
    homeDescription:
      "Rohy relie ton Android et ton ordinateur. Un texte copié d’un côté arrive de l’autre, sur ton Wi-Fi, chiffré de bout en bout. Sans compte, sans cloud.",
    releasesTitle: "Nouveautés de Rohy",
    releasesDescription: "Ce qui change à chaque version de Rohy, et les fichiers à télécharger.",
    reviewsTitle: "Avis sur Rohy",
    reviewsDescription: "Dis-nous ce qui marche et ce qui coince. Les avis sont publiés après relecture.",
    notFoundTitle: "Page introuvable",
  },
  nav: {
    label: "Principale",
    download: "Télécharger",
    releases: "Nouveautés",
    reviews: "Avis",
    menu: "Menu",
    closeMenu: "Fermer le menu",
    otherLanguage: "English",
    otherLanguageLabel: "Read in English",
    language: "Langue",
  },
  home: {
    badge: "Android ⇄ Mac, Windows, Linux",
    title: "Copie sur ton téléphone. Colle sur ton ordinateur.",
    lead: "Rohy relie ton Android et ton ordinateur. Un texte copié d’un côté arrive de l’autre, sur ton Wi-Fi, chiffré de bout en bout. Sans compte, sans cloud.",
    downloadFree: "Télécharger gratuitement",
    seeReleases: "Voir les nouveautés",
    versionLine: (v: string) => `Version ${v} · gratuite`,
    illustration: "Un téléphone et un ordinateur qui se sourient, reliés par un cœur",
    sentChip: "Texte envoyé à l’ordinateur",
    downloadTitle: "Télécharger Rohy",
    downloadLead: (v: string) => `Installe l’app sur ton téléphone et sur ton ordinateur. Version ${v}.`,
    platforms: {
      android: {
        name: "Android",
        needs: "Android 10 ou plus récent.",
        button: "Télécharger l’APK",
        note: "Android te demandera d’autoriser l’installation.",
      },
      macos: {
        name: "macOS",
        needs: "macOS 14 ou plus récent, puce Apple ou Intel. Dans la barre des menus.",
        button: "Télécharger pour Mac",
        note: "Fichier .zip à ouvrir, puis glisser Rohy dans Applications.",
      },
      windows: {
        name: "Windows",
        needs: "Windows 10 ou 11, 64 bits. Près de l’horloge.",
        button: "Télécharger pour Windows",
        note: "Fichier .zip avec Rohy.exe.",
      },
      linux: {
        name: "Linux",
        needs: "x86_64, Ubuntu 22.04 ou équivalent (glibc 2.35+).",
        button: "Télécharger pour Linux",
        note: "Archive .tar.gz avec l’exécutable rohy.",
      },
    },
    whyTitle: "Pourquoi Rohy",
    features: [
      { mark: "E2E", title: "Chiffré de bout en bout", text: "Seuls ton téléphone et ton ordinateur peuvent lire ce que tu copies." },
      { mark: "Wi-Fi", title: "Sur ton Wi-Fi, sans cloud", text: "Les textes passent directement sur ton réseau local. Rien n’est stocké ailleurs." },
      { mark: "0", title: "Sans compte", text: "Pas d’inscription : on associe les appareils avec un QR code." },
      { mark: "⇢", title: "Depuis n’importe quelle app", text: "La poignée sur le bord de l’écran envoie un texte même depuis un jeu en plein écran." },
    ],
    stepsTitle: "Prêt en trois gestes",
    steps: [
      "Installe Rohy sur ton ordinateur et sur ton Android.",
      "Sur l’ordinateur, choisis « Associer un téléphone », puis scanne le QR code avec l’app.",
      "Copie d’un côté, colle de l’autre. C’est tout.",
    ],
    reviewTitle: "Un avis, une idée, un souci ?",
    reviewLead: "Dis-nous ce qui marche et ce qui coince. Chaque message est lu.",
    reviewButton: "Donner mon avis",
  },
  releases: {
    title: "Nouveautés",
    lead: "Ce qui change à chaque version de Rohy, et les fichiers à télécharger.",
    soon: "Bientôt",
    nextVersion: "Prochaine version",
    latest: "Dernière version",
    files: { android: "Android (APK)", macos: "macOS", windows: "Windows", linux: "Linux" },
    downloadFile: (name: string, version: string) => `Télécharger ${name}, version ${version}`,
  },
  reviews: {
    title: "Ton avis sur Rohy",
    lead: "Ce qui marche, ce qui coince, une idée. Les avis sont publiés après relecture.",
    name: "Ton prénom",
    nameHint: "Facultatif. Laisse vide pour rester anonyme.",
    rating: "Ta note",
    star: (n: number) => (n === 1 ? "1 étoile" : `${n} étoiles`),
    devices: "Tu utilises Rohy avec",
    deviceOptions: { mac: "Android et Mac", windows: "Android et Windows", linux: "Android et Linux" },
    body: "Ton avis",
    bodyCount: (n: number, max: number) => `${n} / ${max}`,
    robot: "Vérification anti-robot",
    consent: "J’accepte que mon prénom (s’il est rempli) et mon avis soient publiés sur ce site.",
    submit: "Envoyer mon avis",
    sending: "Envoi…",
    sentTitle: "Merci, c’est envoyé !",
    sentText: "Ton avis sera lu, puis publié ici s’il respecte les règles.",
    sendAnother: "Envoyer un autre avis",
    errors: {
      rating: "Choisis une note de 1 à 5 étoiles.",
      body: "Écris au moins 10 caractères.",
      token: "La vérification anti-robot n’est pas passée. Réessaie.",
      network: "Impossible d’envoyer ton avis. Vérifie ta connexion et réessaie.",
      other: "Quelque chose cloche dans le formulaire. Vérifie-le et réessaie.",
    },
    publishedTitle: "Avis publiés",
    loading: "Chargement des avis…",
    empty: "Aucun avis publié pour l’instant. Le tien peut être le premier.",
    loadError: "Les avis ne peuvent pas être chargés pour le moment.",
    anonymous: "Anonyme",
    ratingLabel: (n: number) => `Note : ${n} sur 5`,
  },
  footer: {
    by: "Rohy, par Kinga",
    copyright: "© 2026 Kinga",
  },
  notFound: {
    title: "Cette page n’existe pas",
    text: "Le lien est peut-être ancien ou mal tapé.",
    home: "Retour à l’accueil",
  },
};

export const DICTS: Record<Lang, Dict> = { en, fr };

export function t(lang: Lang): Dict {
  return DICTS[lang];
}

export function formatDate(iso: string, lang: Lang): string {
  return new Intl.DateTimeFormat(lang === "fr" ? "fr-FR" : "en-US", {
    day: "numeric",
    month: "long",
    year: "numeric",
    timeZone: "UTC",
  }).format(new Date(iso));
}
