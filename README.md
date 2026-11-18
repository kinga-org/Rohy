# Rohy

Pont de presse-papiers entre un Mac et un téléphone Android : natif des deux côtés, local,
chiffré de bout en bout, gratuit et libre. « Rohy » signifie « lien, corde » en malgache.

- **Mac → Android** : tout texte copié sur le Mac arrive dans le presse-papiers du téléphone.
- **Android → Mac** : tuile Réglages rapides « Envoyer au Mac », menu Partager, ou action de la notification
  (Android interdit la lecture du presse-papiers en arrière-plan depuis Android 10).

## Structure

| Dossier | Contenu |
|---|---|
| `mac/` | Paquet Swift : `RohyCore` (protocole, crypto), `RohyServer` (WebSocket + Bonjour), app barre de menus `Rohy`, `rohy-harness` (tests d'interopérabilité) |
| `android/` | `core` (protocole Kotlin, JVM pur) et `app` (service, tuile, partage, interface Compose) |
| `protocol/` | Vecteurs de test partagés par les deux implémentations |
| `docs/` | Protocole, phases, backlog, étude concurrentielle, [avancement](docs/avancement/README.md) |

## Prérequis

- macOS 14+, Xcode (Swift 6).
- Android SDK (plateforme 36) et un JDK 21 (celui d'Android Studio convient).

## Commandes

```bash
scripts/setup.sh          # installe les hooks Git du dépôt
scripts/check-mac.sh      # tests Swift + seuil de couverture
scripts/check-android.sh  # tests Kotlin (dont client Kotlin ↔ vrai serveur Swift) + Kover + build debug
mac/scripts/bundle.sh     # construit mac/dist/Rohy.app
```

Installer l'app Android sur un téléphone branché :

```bash
cd android && ./gradlew :app:installDebug
```

## Développement sur Mac

`mac/scripts/bundle.sh` signe l'app de façon ad hoc : la signature change à chaque compilation, et macOS
redemande alors l'accès au trousseau (clé privée de Rohy) au lancement. Pour l'éviter, crée un certificat
gratuit « Apple Development » (Xcode → Réglages → Comptes) puis :

```bash
CODESIGN_IDENTITY="Apple Development" mac/scripts/bundle.sh
```

## Sécurité

Le protocole est décrit dans [`docs/protocol.md`](docs/protocol.md). Il n'a pas encore été audité.
