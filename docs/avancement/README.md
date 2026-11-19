# Avancement

Suivi de l'avancement de Rohy. Ce tableau de bord est mis à jour à chaque changement fusionné ;
le détail de chaque session de travail est dans un journal daté (`AAAA-MM-JJ.md`).

## État des phases

| Phase | Statut | Reste à faire |
|---|---|---|
| [01 — Fondation](../phases/01-fondation.md) | Terminée | — |
| [02 — Appairage et chiffrement](../phases/02-appairage-chiffrement.md) | Terminée | — |
| [03 — Texte Mac → Android](../phases/03-mac-vers-android.md) | Fonctionne (Samsung, Huawei) | Tenue de l'écriture en arrière-plan sur la durée (phase 06) |
| [04 — Texte Android → Mac](../phases/04-android-vers-mac.md) | Fonctionne | Mesurer la latence de la tuile |
| [05 — Prototype OTP via appareil compagnon](../phases/05-prototype-otp-compagnon.md) | À faire | Tout |
| [06 — Fiabilité et accueil Samsung](../phases/06-fiabilite-samsung.md) | En cours | Test d'endurance de 24 h |
| [07 — Cœur Rust et serveur pour le bureau](../phases/07-coeur-rust.md) | Terminée | — |
| [08 — App de bureau Windows et Linux (iced)](../phases/08-app-bureau-iced.md) | En cours | Installateurs (`.msi`, `.deb` / AppImage), essais sur un vrai Windows et un vrai Linux |
| [09 — Plusieurs ordinateurs par téléphone](../phases/09-plusieurs-ordinateurs.md) | À faire | Tout |

## Releases

- Dernière publiée : **0.1.4** (Android, macOS, Windows, Linux), APK signé avec la clé de test.
- Dans `main`, pas encore publié : Android Lint en CI, boutons « Ouvrir » Samsung/Huawei corrigés.
- Testé sur téléphone : Samsung S24 Ultra (Android 16), Huawei P30 Pro (Android 10). Non testés :
  Android 11 à 15 et 17, `Rohy.exe` sur Windows, l'app Linux sur un vrai bureau.

## Backlog (docs/features)

| Fiche | Statut |
|---|---|
| [Windows et Linux](../features/windows-linux.md) | Promue en phases 07, 08 et 09 |
| [Codes de vérification](../features/codes-verification.md) | Dépend de la phase 05 |
| [Historique](../features/historique.md) | Après le MVP |
| [Images et fichiers](../features/images-fichiers.md) | Après le MVP |
| [Hors réseau local](../features/hors-reseau-local.md) | Selon la demande |

## Prochaines étapes

1. Phase 08 : essayer l'app Linux sur le portable du propriétaire, puis `Rohy.exe` sur un vrai Windows.
2. Phase 06 : test d'endurance de 24 h, écran éteint, changement de Wi-Fi et veille du Mac.
3. Phase 08 : installateurs `.msi` et `.deb` / AppImage.
4. Phase 09 : plusieurs ordinateurs par téléphone.
5. Phase 05 : prototype des codes de vérification.
6. Clé de signature Android définitive (fin des réinstallations à chaque version).

## Journal

- [2026-11-19](2026-11-19.md) — plantage du scan en release (R8) et plantage Android 10 corrigés, garde-fous R8, Animal Sniffer et Lint en CI ; app Mac native ; build Linux ; 0.1.2 à 0.1.4 publiées ; phase 07 terminée.
- [2026-11-18](2026-11-18.md) — étude, MVP texte dans les deux sens, premier test réel, CI dans kinga-org, début de la phase 06.
