# Phase 02 — Appairage et chiffrement

**Statut :** en cours

## Objectif
Associer un téléphone à un Mac par QR code et établir des sessions chiffrées de bout en bout.

## Périmètre
- Clés X25519 par appareil (Trousseau sur Mac, clé chiffrée par le Keystore sur Android).
- QR code `rohy://pair` affiché par le Mac ; scan (ML Kit code scanner) ou collage du code côté Android.
- Poignée de main, dérivation HKDF, trames ChaCha20-Poly1305 à compteur (voir `docs/protocol.md`).
- Liste des appareils associés sur le Mac, avec suppression.

## Dépendances
Phase 01.

## Tâches
- [x] Machine à états de session côté serveur (Swift) et côté client (Kotlin), sans réseau
- [x] Serveur WebSocket + Bonjour (Network.framework)
- [x] Client WebSocket (OkHttp) + découverte NSD avec repli sur les adresses du QR code
- [x] Écran d'appairage Mac (QR code + code texte) et écran d'association Android
- [ ] Test sur le Samsung de l'auteur

## Critères d'acceptation
- Un appareil inconnu sans jeton est refusé (`4403`).
- Une trame rejouée ou modifiée ferme la session (`4400`).
- Le test d'interopérabilité Kotlin ↔ serveur Swift réel passe.

## Stratégie de test
Tests unitaires des machines à états (cas nominaux et attaques : jeton faux, rejeu, clé substituée),
test d'interopérabilité de bout en bout via `rohy-harness`.
