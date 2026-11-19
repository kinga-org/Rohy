# Phase 08 — App de bureau Windows et Linux (iced)

**Statut :** en cours (démarrée le 2026-11-18)

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
- [x] Squelette iced + icône de barre des tâches
- [x] Fenêtre d'appairage et liste des appareils
- [x] Surveillance et écriture du presse-papiers par système
- [x] Stockage de la clé dans le trousseau du système
- [x] Archives portables dans les releases GitHub (Windows `.zip`, Linux x86_64 `.tar.gz`)
- [ ] Paquets d'installation (`.msi`, `.deb` / AppImage)
- [ ] Tests manuels Windows 11 et Linux (X11 ou KDE)

## Notes de réalisation
- `iced::daemon` : aucune fenêtre au démarrage ; fenêtres « Associer un téléphone » et « Téléphones associés »
  ouvertes depuis le menu de l'icône.
- Icône : `tray-icon` (Windows, macOS) ; sous Linux, backend `ksni` (D-Bus StatusNotifierItem), sans GTK.
- Serveur `rohy-server` sur son propre runtime tokio ; repli sur un port libre si 47823 est pris.
- Presse-papiers : `arboard` interrogé toutes les 0,5 s + garde anti-écho par contenu ; formats Windows des
  gestionnaires de mots de passe filtrés (`clipboard-win`). Linux : l'indice KDE n'est pas encore lu.
- Logique testée à part (`state`, `clipboard`, `identity`, `images`, `service`) ; `main.rs` et
  `platform.rs` (colle avec le système) exclus de la couverture et vérifiés à la main.
- Essai sur le Mac de développement : l'app démarre, l'icône apparaît, serveur et Bonjour actifs.
- Essai avec le Galaxy S24 Ultra (18/11, 16:49) : association par QR code avec l'app iced sur le Mac,
  connexion établie (port de repli 51981), synchronisation confirmée par l'utilisateur.
  Retour : fonctionne, mais le design des fenêtres est à revoir.
- Refonte visuelle d'après la maquette « Rohy bureau — refonte » (canvas Design,
  https://claude.ai/artifact/StchZXp6RPY4E4Nv2CwSc9) : vert foncé #1B4B5A, police Inter embarquée
  (OFL, `assets/fonts/`), cartes arrondies, icônes SVG au trait, QR code en correction H avec le logo
  au centre, bannière « Connecté à … » avec la dernière activité, liste des téléphones, état vide.
  Code dans `src/ui.rs` ; rendus PNG hors écran pour comparer à la maquette :
  `ROHY_SNAPSHOT_DIR=… cargo test -p rohy-desktop -- --ignored renders`.
- Options de lancement `--appairage` et `--telephones` : ouvrent la fenêtre dès que le serveur écoute.
- Mode sombre : palettes claire et sombre (maquette, deuxième rangée), choisies au dessin d'après
  le thème iced. Sous-menu « Apparence » de l'icône : Système (suit le mode clair/sombre du
  système, par défaut), Clair, Sombre ; choix gardé dans le fichier `appearance` du dossier de
  données. Le QR code reste noir sur blanc dans les deux modes.

## Critères d'acceptation
Association et synchronisation dans les deux sens avec le Samsung, sur Windows 11 et sur une distribution Linux.

## Stratégie de test
Logique testée dans `rohy-core` ; état de l'interface testé par messages iced ; tests manuels par système.
