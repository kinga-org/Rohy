# Phase 06 — Fiabilité et accueil Samsung

**Statut :** à faire

## Objectif
Rendre la connexion durable sur les téléphones Samsung et guider l'utilisateur dans les réglages de batterie.

## Périmètre
- Écran d'accueil listant « Applis jamais en veille », batterie « Non restreinte », verrouillage dans les apps récentes.
- Permission `ACCESS_LOCAL_NETWORK` (Android 17) et alerte réseau local de macOS.
- Test d'endurance 24–72 h, reconnexion après changement de Wi-Fi.

## Dépendances
Phases 03 et 04.

## Tâches
- [ ] Assistant de réglages Samsung
- [ ] Gestion des permissions réseau local
- [ ] Test d'endurance

## Critères d'acceptation
Service vivant après 24 h écran éteint sur le Samsung de test.

## Stratégie de test
Tests unitaires de la détection constructeur ; tests manuels d'endurance.
