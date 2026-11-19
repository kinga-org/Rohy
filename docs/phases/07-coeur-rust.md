# Phase 07 — Cœur Rust et serveur pour le bureau

**Statut :** terminée (2026-11-19) — critères d'acceptation remplis.

## Objectif
Écrire la troisième implémentation du protocole v1, en Rust, avec le serveur WebSocket + Bonjour, pour
préparer l'app Windows/Linux (phase 08). Voir `docs/features/windows-linux.md`.

## Périmètre
- `desktop/crates/rohy-core` : encodages, X25519 / HKDF-SHA256 / ChaCha20-Poly1305, trames à compteur,
  messages, QR d'appairage, session serveur (machine à états sans réseau), appareils de confiance,
  politique du presse-papiers et anti-écho.
- `desktop/crates/rohy-server` : serveur `tokio` + `tokio-tungstenite`, annonce Bonjour `mdns-sd`,
  binaire `rohy-harness` au même format que celui du Mac.

## Dépendances
Phases 01–02 (protocole et vecteurs de test).

## Tâches
- [x] `rohy-core` validé par `protocol/test-vectors.json`
- [x] Tests de la session serveur (appairage, jeton faux, rejeu, clé inconnue, versions)
- [x] `rohy-server` + test d'intégration Rust (client WebSocket réel)
- [x] `rohy-harness` Rust : le test d'interopérabilité Kotlin passe contre lui
- [x] `scripts/check-desktop.sh` : fmt, clippy, tests, couverture ≥ 80 %
- [x] CI : tâches Ubuntu et Windows (`desktop` dans `.github/workflows/ci.yml`, vertes à chaque PR depuis le 2026-11-18)

## Résultats (2026-11-18)
- Les vecteurs partagés passent du premier coup : Swift, Kotlin et Rust produisent les mêmes clés et trames.
- Le test d'interopérabilité Kotlin passe contre le serveur Rust (association, deux sens, reconnexion, refus).
- Couverture Rust : 98,5 % des lignes, 100 % des fonctions, 99,1 % des régions (`rohy-harness` exclu).

## Critères d'acceptation
- Les trois implémentations (Swift, Kotlin, Rust) valident les mêmes vecteurs.
- Le client Kotlin s'associe, échange dans les deux sens et se reconnecte contre le serveur Rust.
- Couverture Rust ≥ 80 % (lignes, fonctions, régions) ; CI verte sur Ubuntu et Windows.

## Stratégie de test
Tests unitaires Rust, test d'intégration réseau Rust, test d'interopérabilité Kotlin ↔ Rust.
