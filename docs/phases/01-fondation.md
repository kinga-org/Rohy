# Phase 01 — Fondation

**Statut :** en cours

## Objectif
Poser le monorepo (Mac + Android + protocole), la qualité automatisée et la spécification du protocole.

## Périmètre
- Arborescence `mac/` (paquet Swift), `android/` (Gradle, Kotlin), `protocol/` (vecteurs de test), `docs/`.
- Spécification `docs/protocol.md` et vecteurs `protocol/test-vectors.json` partagés par les deux côtés.
- Hooks Git versionnés (`.githooks/`) et workflow GitHub Actions.

## Dépendances
Aucune.

## Tâches
- [x] Initialiser le dépôt et la documentation (README, phases, backlog, étude concurrentielle)
- [x] Rédiger la spécification du protocole v1
- [x] Générer les vecteurs de test (côté Swift/CryptoKit) et les vérifier côté Kotlin/BouncyCastle
- [x] Hooks `pre-commit` (tests rapides du côté modifié) et `pre-push` (suite complète + couverture)
- [x] Workflow CI : tests Swift + couverture, tests Kotlin + Kover, build de l'APK debug

## Critères d'acceptation
- `scripts/check-mac.sh` et `scripts/check-android.sh` passent en local.
- Les deux implémentations valident les mêmes vecteurs de test.
- Couverture ≥ 80 % (instructions, branches, fonctions, lignes) sur les modules cœur.

## Stratégie de test
Tests unitaires Swift (XCTest) et Kotlin (JUnit), vecteurs croisés, seuils de couverture appliqués dans les scripts et la CI.
