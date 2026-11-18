# Phase 09 — Plusieurs ordinateurs par téléphone

**Statut :** à faire

## Objectif
Permettre au téléphone de rester associé et connecté à plusieurs ordinateurs (Mac, Windows, Linux) à la fois.

## Périmètre
- Liste d'ordinateurs associés sur Android, une connexion par ordinateur dans le service.
- Envoi Android → ordinateurs : à tous par défaut ; choix possible depuis la notification.
- Écran : liste des ordinateurs avec leur état, oubli individuel.
- Migration de l'association unique actuelle vers la liste.

## Dépendances
Phases 06 et 07.

## Tâches
- [ ] Stockage d'une liste d'ordinateurs (avec migration)
- [ ] Service multi-connexions
- [ ] Interface et notification
- [ ] Tests (stockage, migration, routage des envois)

## Critères d'acceptation
Le téléphone reçoit du Mac et d'un PC Windows/Linux et leur envoie, sans réassociation.

## Stratégie de test
Tests unitaires JVM ; test d'interopérabilité avec deux serveurs (`rohy-harness` Swift et Rust).
