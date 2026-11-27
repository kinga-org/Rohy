# Phase 12 — Site vitrine `rohy.kingalabs.com`

**Statut :** en cours (démarrée le 2026-11-27)

## Objectif
Un site public pour télécharger Rohy sur les quatre systèmes, lire les nouveautés de chaque version
et laisser un avis. Public visé : le monde entier, donc anglais par défaut et français à `/fr/`.

## Maquette
Canvas « Rohy — site vitrine » (https://claude.ai/artifact/DridFj4pX5YrDQvkvS5M9w) : accueil
(ordinateur et téléphone), Nouveautés, Avis. Couleurs et logo de la phase 11. Sur téléphone, l'en-tête
tient sur une ligne : logo, bouton « Télécharger » compact et bouton menu.
Sélecteur de langue EN | FR : dans l'en-tête sur ordinateur, en bas du menu sur téléphone
(écran « Menu ouvert — téléphone ») ; il garde la même page dans l'autre langue.

## Choix validés
- Domaine `kingalabs.com` acheté chez Cloudflare Registrar (2026-11-27) ; le site est sur le
  sous-domaine `rohy.kingalabs.com`.
- Hébergement Cloudflare, offre gratuite : Worker avec fichiers statiques, D1 pour les avis, R2 pour
  les fichiers (le dépôt GitHub est privé), Turnstile contre les robots.
- Next.js en export statique ; le Worker ne sert que `/api/reviews` et `/download/<fichier>`.
- Aucun faux avis : les avis sont relus avant publication et un avis sans accord de publication
  n'est jamais affiché.

## Fait
- Site codé dans `site/` : accueil, Nouveautés (0.1.2 à 0.1.4 + « Prochaine version »), Avis
  (formulaire, Turnstile, avis validés), page 404, anglais et français.
- Worker : règles du formulaire testées (`worker/reviews.test.ts`), téléchargements depuis R2.
- CI : job `site` (types, tests, export statique).
- Essayé en local (`wrangler dev`) : pages, menu téléphone, envoi d'un avis, validation, téléchargement.

- En ligne le 2026-11-27 sur https://rohy.kingalabs.com : base D1 `rohy-reviews`, bucket R2
  `rohy-downloads` (fichiers 0.1.2 à 0.1.4), clés Turnstile, Worker `rohy-site`. Pages, 404, API des
  avis et téléchargements vérifiés en ligne ; le widget Turnstile se charge avec la vraie clé.

## Référencement
- `sitemap.xml` (6 pages, liens anglais/français), `robots.txt` qui l'annonce et bloque `/api/` et
  `/download/`, fiche `SoftwareApplication` (JSON-LD) sur l'accueil, sans note inventée.
- À faire par le propriétaire : Google Search Console (propriété `kingalabs.com`, envoi du sitemap),
  Bing Webmaster Tools (import depuis Google), Crawler Hints dans Cloudflare.

## Reste à faire
- Premier vrai avis envoyé depuis le site en ligne (essai par le propriétaire).
- Envoyer les fichiers dans R2 depuis le workflow de release (jeton Cloudflare en secret GitHub).
- Plus tard : prévenir par e-mail quand un avis arrive (Cloudflare Email Sending), mode sombre.
