# Phase 06 — Fiabilité et accueil Samsung

**Statut :** en cours (démarrée le 2026-11-18)

## Objectif
Garder la connexion au Mac vivante pendant des heures sur un téléphone Samsung, la rétablir seule
après une coupure (Wi-Fi, veille du Mac, redémarrage du téléphone) et guider l'utilisateur dans les
réglages de batterie que l'app ne peut pas changer elle-même.

## Périmètre
- **Assistant « Garder la connexion »** dans l'app : liste de vérifications avec leur état réel quand
  Android permet de le lire (notifications, optimisation de batterie, mode « Restreint »), et raccourcis
  vers les réglages Samsung que l'app ne peut pas lire (« Applis jamais en veille », verrouillage dans les
  apps récentes).
- **Reconnexion automatique** : reprise immédiate quand le Wi-Fi revient, au lieu d'attendre le délai
  d'attente progressif.
- **Redémarrage automatique** du service après le démarrage du téléphone et après une mise à jour de l'app.
- **Journal de connexion** visible dans l'app (derniers événements horodatés) pour le test d'endurance.
- Permission `ACCESS_LOCAL_NETWORK` déclarée dès maintenant ; la demande à l'exécution viendra avec le
  passage à `targetSdk 37` (Android 17 ne l'impose qu'aux apps qui le ciblent).

Hors périmètre : association `CompanionDeviceManager` (phase 05), plusieurs ordinateurs par téléphone.

## Dépendances
Phases 03 et 04.

## Tâches
- [ ] Journal de connexion (tampon circulaire persistant) et ses tests
- [ ] Modèle de la liste de vérifications (états, actions) et ses tests
- [ ] Surveillance du réseau (`ConnectivityManager.NetworkCallback`) : reconnexion immédiate au retour du Wi-Fi
- [ ] Récepteur `BOOT_COMPLETED` / `MY_PACKAGE_REPLACED` qui relance le service si un Mac est associé
- [ ] Demande d'exemption d'optimisation de batterie (un appui) et détection du mode « Restreint »
- [ ] Raccourcis Samsung (Entretien de l'appareil → Batterie) avec repli sur les réglages de l'app
- [ ] Écran : section « Garder la connexion » avec l'état de chaque vérification et le journal
- [ ] Test d'endurance : 24 h écran éteint, puis changement de Wi-Fi et veille du Mac

## Critères d'acceptation
- Service toujours connecté après 24 h écran éteint sur le Samsung de test (journal à l'appui).
- Reconnexion en moins de 10 s après le retour du Wi-Fi ou le réveil du Mac.
- Après un redémarrage du téléphone, la connexion revient sans ouvrir l'app.
- Chaque vérification lisible par Android affiche son état réel.

## Stratégie de test
Tests unitaires JVM du journal, du modèle de vérifications et de la politique de reconnexion ;
vérification sur l'appareil via `adb` (redémarrage, Wi-Fi coupé puis rétabli) ; test d'endurance manuel.

## Note Google Play
La demande directe d'exemption d'optimisation de batterie (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) est
encadrée par Google Play : elle est acceptée quand la fonction principale de l'app est compromise sans
elle (connexion permanente à un appareil compagnon). La justification devra être fournie à la publication.
