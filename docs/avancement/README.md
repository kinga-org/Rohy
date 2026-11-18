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
| [06 — Fiabilité et accueil Samsung](../phases/06-fiabilite-samsung.md) | En cours | Redémarrage du téléphone, test d'endurance de 24 h |

## Backlog (docs/features)

| Fiche | Statut |
|---|---|
| [Windows et Linux](../features/windows-linux.md) | Choix technique fait (Rust + iced), après stabilisation |
| [Codes de vérification](../features/codes-verification.md) | Dépend de la phase 05 |
| [Historique](../features/historique.md) | Après le MVP |
| [Images et fichiers](../features/images-fichiers.md) | Après le MVP |
| [Hors réseau local](../features/hors-reseau-local.md) | Selon la demande |

## Prochaines étapes

1. Phase 06 : redémarrage du téléphone, puis test d'endurance de 24 h écran éteint (journal de connexion).
2. Phase 05 : prototype de l'association `CompanionDeviceManager` sur le Samsung (codes de vérification).
3. Gérer plusieurs ordinateurs par téléphone (prérequis de Windows et Linux).

## Journal

- [2026-11-18](2026-11-18.md) — étude, MVP texte dans les deux sens, premier test réel, CI dans kinga-org, début de la phase 06.
