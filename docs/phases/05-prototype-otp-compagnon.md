# Phase 05 — Prototype OTP via appareil compagnon

**Statut :** à faire

## Objectif
Vérifier sur le Samsung de test si une association `CompanionDeviceManager` vers le Mac lève le masquage
des codes de vérification (Android 15+) pour un `NotificationListenerService`. Ce test décide si la
fonction « code de vérification automatique vers le Mac » est publiable.

## Périmètre
Prototype jetable : association CDM (filtre Wi-Fi/BLE ou autogérée), écouteur de notifications qui
journalise le texte reçu, réception d'un SMS de code. Aucun envoi tant que le résultat n'est pas connu.

## Dépendances
Phase 02.

## Tâches
- [ ] Association CDM depuis l'app Android
- [ ] Écouteur de notifications et journalisation
- [ ] Essai sur One UI 7/8 avec un vrai SMS de code
- [ ] Essai du repli ADB `appops set <pkg> RECEIVE_SENSITIVE_NOTIFICATIONS allow`
- [ ] Décision documentée : niveau 2 (CDM) possible ou non

## Critères d'acceptation
Résultat consigné ici, avec captures et version d'Android/One UI.

## Stratégie de test
Manuelle, sur appareil réel.
