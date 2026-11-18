# Rohy pour Windows et Linux

**Statut :** backlog. Décision technique prise le 2026-11-18 ; à promouvoir en phase numérotée après la
stabilisation (phases 05 et 06) et le passage du téléphone à plusieurs ordinateurs.

## Objectif

Proposer le même pont de presse-papiers que sur le Mac aux utilisateurs Windows et Linux, avec le même
protocole (`docs/protocol.md`), la même hygiène des secrets et le même appairage par QR code.

## Décision : Rust + iced

| Option | Verdict | Raison |
|---|---|---|
| **Rust + iced** | **Retenue** | Interface native (pas de page web), binaire de quelques Mo, architecture Elm (état → message → mise à jour) qui colle à la machine à états de session |
| Rust + egui | Écartée | Mode immédiat redessiné à chaque image : moins adapté à une app qui dort dans la barre des tâches ; rendu plus « outil » |
| Tauri | Écartée | Interface en page web (moteur du système), même si plus léger qu'Electron |
| Kotlin Compose Desktop | Écartée | Réutilisait `android/core`, mais embarque Java (installateur de 60 à 80 Mo) |

Le Mac garde son app Swift native.

## Architecture

```
desktop/                     espace de travail Cargo
├── crates/rohy-core/        protocole v1 en Rust : X25519, HKDF-SHA256, ChaCha20-Poly1305,
│                            trames à compteur, session serveur (machine à états sans réseau)
└── apps/rohy-desktop/       app iced : icône de barre des tâches, fenêtre QR, appareils associés
```

| Rôle | Crate envisagée |
|---|---|
| Cryptographie | `x25519-dalek`, `hkdf`, `sha2`, `chacha20poly1305` (RustCrypto) |
| Serveur WebSocket | `tokio` + `tokio-tungstenite` |
| Découverte Bonjour | `mdns-sd` |
| Presse-papiers | `arboard` ; Windows : `AddClipboardFormatListener` pour être prévenu des changements |
| Icône de barre des tâches | `tray-icon` |
| QR code | `qrcode` |
| Clé privée | `keyring` (Gestionnaire d'identifiants Windows, Secret Service sous Linux) |

`rohy-core` est la troisième implémentation du protocole : elle doit valider `protocol/test-vectors.json`
comme Swift et Kotlin, et le test d'interopérabilité doit pouvoir viser son serveur.

## Contraintes par système

| Système | Contrainte | Réponse prévue |
|---|---|---|
| Windows | Pare-feu : autorisation demandée au premier lancement | Message d'accueil qui l'explique |
| Windows | Gestionnaires de mots de passe : formats `ExcludeClipboardContentFromMonitorProcessing`, `CanIncludeInClipboardHistory` | Ne jamais envoyer ces contenus (équivalent des marqueurs `org.nspasteboard.*`) |
| Linux X11, KDE (Wayland) | Lecture en arrière-plan possible (KDE via le protocole data-control) | Synchro automatique |
| Linux GNOME (Wayland) | Une app en arrière-plan ne peut pas lire le presse-papiers | Envoi par raccourci clavier ou menu de l'icône, comme sur Android |
| Linux | Bonjour via Avahi selon la distribution | Repli sur les adresses du QR code |

## Prérequis

1. Phases 05 (OTP via appareil compagnon) et 06 (fiabilité Samsung) terminées : le protocole peut encore évoluer.
2. **Plusieurs ordinateurs par téléphone** : l'app Android ne mémorise aujourd'hui qu'un Mac. Il faut une
   liste d'ordinateurs associés, une connexion par ordinateur, et choisir vers qui envoyer (tous par défaut).
3. CI : ajouter des tâches Windows et Linux (runners moins chers que macOS).

## Concurrence

- Windows : Phone Link synchronise déjà le presse-papiers avec les Samsung (intégré, fermé, via le cloud).
- Linux : KDE Connect et GSConnect sont solides.
- Valeur de Rohy ici : une seule app libre, locale et chiffrée de bout en bout pour Mac, Windows et Linux.

## Critères d'acceptation (à préciser lors de la promotion en phase)

- `rohy-core` valide les vecteurs de test partagés ; couverture ≥ 80 %.
- Appairage, envoi dans les deux sens et refus d'un inconnu vérifiés contre le client Kotlin.
- Windows 11 et une distribution Linux (X11 ou KDE) testés à la main ; limite GNOME documentée dans l'app.
