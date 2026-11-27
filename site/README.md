# Site vitrine de Rohy

`rohy.kingalabs.com` : accueil avec les téléchargements, page « Nouveautés » (releases) et page
« Avis » (formulaire + avis publiés). Anglais à `/`, français à `/fr/`.

- **Pages** : Next.js 16 en export statique (`out/`). Textes dans `src/content/i18n.ts`, versions et
  notes dans `src/content/releases.ts`.
- **Worker** (`worker/index.ts`) : sert `out/` et répond à
  - `GET /api/reviews` : avis validés (D1) ;
  - `POST /api/reviews` : nouvel avis, vérifié par Cloudflare Turnstile, enregistré « en attente » ;
  - `GET /download/<fichier>` : fichiers des releases, depuis le bucket R2 `rohy-downloads`
    (le dépôt GitHub est privé, ses releases ne sont pas téléchargeables par le public).
- **Maquette** : canvas « Rohy — site vitrine » (https://claude.ai/artifact/DridFj4pX5YrDQvkvS5M9w).

Tout tient dans l'offre gratuite de Cloudflare (Workers, D1, R2, Turnstile).

## Développer

```bash
cd site
npm install
npm run dev            # pages seules sur http://localhost:3000 (pas d'API : la liste d'avis affiche une erreur)
cp .dev.vars.example .dev.vars
npm run db:migrate:local
npm run preview        # build + Worker local sur http://localhost:8787 (D1 et R2 simulés, Turnstile en mode test)
```

Contrôle complet (comme la CI) : `scripts/check-site.sh` depuis la racine du dépôt.

## Première mise en ligne (une seule fois, par le propriétaire)

1. `cd site && npx wrangler login` (compte Cloudflare qui possède `kingalabs.com`).
2. Base des avis : `npx wrangler d1 create rohy-reviews`, puis copier l'`database_id` affiché dans
   `wrangler.jsonc` et lancer `npm run db:migrate:remote`.
3. Bucket des téléchargements : `npx wrangler r2 bucket create rohy-downloads`.
4. Turnstile : tableau de bord Cloudflare → Turnstile → ajouter un widget pour `rohy.kingalabs.com`.
   - clé secrète : `npx wrangler secret put TURNSTILE_SECRET` ;
   - clé publique : `SITE_KEY` dans `src/components/ReviewForm.tsx` (utilisée sur `*.kingalabs.com`,
     la clé de test de Cloudflare ailleurs, par exemple en local).
5. Fichiers : `scripts/upload-release.sh v0.1.4` (copie la release GitHub dans R2).
6. Mise en ligne : `npm run deploy`. Le domaine
   `rohy.kingalabs.com` est créé par `wrangler` (`routes` → `custom_domain`).

## À chaque nouvelle version

1. Ajouter la version en tête de `RELEASES` dans `src/content/releases.ts` (notes en anglais et en
   français) et vider `UPCOMING` de ce qui est sorti.
2. `scripts/upload-release.sh vX.Y.Z`.
3. `npm run deploy`.

## Relire les avis

Les avis arrivent avec le statut `pending` et ne s'affichent qu'une fois `approved`. Un avis dont
l'auteur n'a pas coché « J'accepte que… soit publié » (`can_publish = 0`) n'est jamais affiché, même
validé. Aucun avis n'est inventé : la liste vide affiche « Aucun avis publié pour l'instant ».

```bash
cd site
npx wrangler d1 execute rohy-reviews --remote --command "SELECT id, name, rating, devices, can_publish, body FROM reviews WHERE status = 'pending'"
npx wrangler d1 execute rohy-reviews --remote --command "UPDATE reviews SET status = 'approved' WHERE id = 1"
npx wrangler d1 execute rohy-reviews --remote --command "UPDATE reviews SET status = 'rejected' WHERE id = 2"
```
