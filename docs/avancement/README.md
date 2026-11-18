# Avancement

Suivi de l'avancement de Rohy. Ce tableau de bord est mis à jour à chaque changement fusionné ;
le détail de chaque session de travail est dans un journal daté (`AAAA-MM-JJ.md`).

## État des phases

| Phase | Statut | Reste à faire |
|---|---|---|
| [01 — Fondation](../phases/01-fondation.md) | Terminée | — |
| [02 — Appairage et chiffrement](../phases/02-appairage-chiffrement.md) | Terminée | — |
| [03 — Texte Mac → Android](../phases/03-mac-vers-android.md) | Fonctionne | Mesurer la latence ; tenue du service sur la durée (phase 06) |
| [04 — Texte Android → Mac](../phases/04-android-vers-mac.md) | Fonctionne | Mesurer la latence de la tuile |
| [05 — Prototype OTP via appareil compagnon](../phases/05-prototype-otp-compagnon.md) | À faire | Tout |
| [06 — Fiabilité et accueil Samsung](../phases/06-fiabilite-samsung.md) | En cours | Test d'endurance de 24 h |
| [07 — Cœur Rust et serveur pour le bureau](../phases/07-coeur-rust.md) | En cours | CI Ubuntu et Windows au vert |
| [08 — App de bureau Windows et Linux (iced)](../phases/08-app-bureau-iced.md) | À faire | Tout |
| [09 — Plusieurs ordinateurs par téléphone](../phases/09-plusieurs-ordinateurs.md) | À faire | Tout |

## Backlog (docs/features)

| Fiche | Statut |
|---|---|
| [Windows et Linux](../features/windows-linux.md) | Promue en phases 07, 08 et 09 |
| [Codes de vérification](../features/codes-verification.md) | Dépend de la phase 05 |
| [Historique](../features/historique.md) | Après le MVP |
| [Images et fichiers](../features/images-fichiers.md) | Après le MVP |
| [Hors réseau local](../features/hors-reseau-local.md) | Selon la demande |

## Prochaines étapes

1. Phase 07 : CI Ubuntu et Windows au vert.
2. Phase 08 : app iced (icône de barre des tâches, appairage, presse-papiers par système).
3. Phase 06 : test d'endurance de 24 h, téléphone débranché.
4. Phase 09 : plusieurs ordinateurs par téléphone.

## Journal

- [2026-11-18](2026-11-18.md) — étude, MVP texte dans les deux sens, premier test réel, CI dans kinga-org, début de la phase 06.
