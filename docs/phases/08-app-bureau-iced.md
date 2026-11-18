# Phase 08 — App de bureau Windows et Linux (iced)

**Statut :** à faire

## Objectif
App `rohy-desktop` en Rust + iced : icône dans la barre des tâches, fenêtre d'appairage (QR code),
appareils associés, synchronisation automatique du presse-papiers. Voir `docs/features/windows-linux.md`.

## Périmètre
- Icône de barre des tâches (`tray-icon`) et fenêtres iced.
- Presse-papiers : `arboard` ; Windows : notification de changement ; Linux X11/KDE : surveillance ;
  GNOME Wayland : envoi manuel (limite documentée dans l'app).
- Clé privée dans `keyring` (Gestionnaire d'identifiants Windows, Secret Service sous Linux).
- Filtre des contenus secrets (formats Windows des gestionnaires de mots de passe).
- Paquets : `.msi` (Windows), `.deb` / AppImage (Linux).

## Dépendances
Phase 07.

## Tâches
- [ ] Squelette iced + icône de barre des tâches
- [ ] Fenêtre d'appairage et liste des appareils
- [ ] Surveillance et écriture du presse-papiers par système
- [ ] Stockage de la clé dans le trousseau du système
- [ ] Paquets d'installation
- [ ] Tests manuels Windows 11 et Linux (X11 ou KDE)

## Critères d'acceptation
Association et synchronisation dans les deux sens avec le Samsung, sur Windows 11 et sur une distribution Linux.

## Stratégie de test
Logique testée dans `rohy-core` ; état de l'interface testé par messages iced ; tests manuels par système.
