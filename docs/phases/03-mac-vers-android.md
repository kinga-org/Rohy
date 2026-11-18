# Phase 03 — Texte Mac → Android

**Statut :** en cours — code et tests automatisés terminés ; reste la validation sur le Samsung.

## Objectif
Tout texte copié sur le Mac arrive automatiquement dans le presse-papiers du téléphone.

## Périmètre
- Surveillance de `NSPasteboard.changeCount` (toutes les 0,5 s) sur le Mac.
- Filtre des contenus secrets (types `org.nspasteboard.*`).
- Service de premier plan Android `connectedDevice` qui garde la connexion et écrit le presse-papiers.
- Anti-boucle : le Mac ignore ce qu'il vient d'écrire lui-même.

## Dépendances
Phase 02.

## Tâches
- [x] `PasteboardWatcher` + filtre + garde anti-écho (Mac)
- [x] `SyncService` Android : connexion, reconnexion avec attente progressive, écriture du presse-papiers
- [ ] Vérifier l'écriture en arrière-plan sur Samsung (One UI) et le maintien du service

## Critères d'acceptation
- Copier un texte sur le Mac le rend collable sur le téléphone en moins de 2 s sur le même Wi-Fi.
- Un mot de passe copié depuis un gestionnaire (type Concealed) n'est pas envoyé.

## Stratégie de test
Tests unitaires du filtre et de l'anti-écho ; test d'interopérabilité ; test manuel sur appareil.
