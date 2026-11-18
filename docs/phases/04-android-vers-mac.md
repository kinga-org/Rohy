# Phase 04 — Texte Android → Mac

**Statut :** en cours

## Objectif
Envoyer un texte du téléphone vers le Mac en un geste, malgré l'interdiction de lecture en arrière-plan (Android 10+).

## Périmètre
- Tuile Réglages rapides « Envoyer au Mac » : ouvre une activité transparente qui lit le presse-papiers au premier plan.
- Cible du menu Partager (`ACTION_SEND`, `text/plain`).
- Action « Envoyer le presse-papiers » dans la notification du service.
- Sur le Mac : écriture dans le presse-papiers et notification discrète.

## Dépendances
Phases 02 et 03.

## Tâches
- [x] `SendClipboardActivity` transparente (lecture au focus)
- [x] `ClipTileService` (API 34+ : `startActivityAndCollapse(PendingIntent)`)
- [x] `ShareReceiverActivity`
- [x] Réception côté Mac et écriture avec anti-écho
- [ ] Mesurer la latence tuile → Mac sur Samsung

## Critères d'acceptation
- Tuile, partage et action de notification livrent le texte au Mac.
- Aucune lecture du presse-papiers Android hors premier plan.

## Stratégie de test
Tests unitaires de la logique d'envoi (file d'attente avant authentification) ; test manuel sur appareil.
