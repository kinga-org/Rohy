# Rohy

Pont de presse-papiers entre un Mac et un téléphone Android : natif des deux côtés, local,
chiffré de bout en bout, gratuit et libre. « Rohy » signifie « lien, corde » en malgache.

- **Mac → Android** : tout texte copié sur le Mac arrive dans le presse-papiers du téléphone.
- **Android → ordinateur** : tuile Réglages rapides « Envoyer », menu Partager, ou action de la notification
  (Android interdit la lecture du presse-papiers en arrière-plan depuis Android 10).

## Structure

| Dossier | Contenu |
|---|---|
| `mac/` | Paquet Swift : `RohyCore` (protocole, crypto), `RohyServer` (WebSocket + Bonjour), app barre de menus `Rohy`, `rohy-harness` (tests d'interopérabilité) |
| `desktop/` | Espace de travail Cargo pour Windows/Linux : `rohy-core` (protocole en Rust), `rohy-server` (WebSocket + Bonjour, `rohy-harness`) |
| `android/` | `core` (protocole Kotlin, JVM pur) et `app` (service, tuile, partage, interface Compose) |
| `protocol/` | Vecteurs de test partagés par les deux implémentations |
| `docs/` | Protocole, phases, backlog, étude concurrentielle, [avancement](docs/avancement/README.md) |

## Deux apps bureau, une seule expérience

L'app Mac (Swift, `mac/`) et l'app Windows/Linux (Rust + iced, `desktop/apps/rohy-desktop`) sont deux
codes différents qui doivent rester identiques pour l'utilisateur. **Toute modification de l'une impose
de vérifier l'autre**, dans la même PR ou dans une PR liée :

- mêmes écrans, libellés, états (démarrage, attente, connecté, erreur), couleurs claires et sombres,
  choix « Apparence » et comportements (association, oubli, fermeture de la fenêtre du QR code) ;
- même maquette de référence : canvas « Rohy bureau — refonte »
  (https://claude.ai/artifact/StchZXp6RPY4E4Nv2CwSc9) ;
- les textes et règles d'affichage partagés (temps relatifs, dates, ligne d'état) sont testés des deux
  côtés avec les mêmes valeurs attendues (`DesktopPresentation` en Swift, `state.rs` en Rust) ;
- seule l'enveloppe propre au système peut différer : sur Mac, un panneau dans la barre des menus ;
  sur Windows et Linux, le menu natif de l'icône et une fenêtre « Téléphones associés ».

La description de la PR indique ce qui a été vérifié de l'autre côté, ou pourquoi rien n'est à faire.

## Prérequis

- macOS 14+, Xcode (Swift 6).
- Android SDK (plateforme 36) et un JDK 21 (celui d'Android Studio convient).
- Rust stable, `cargo install cargo-llvm-cov` et `rustup component add llvm-tools-preview` (bureau).

## Commandes

```bash
scripts/setup.sh          # installe les hooks Git du dépôt
scripts/check-mac.sh      # tests Swift + seuil de couverture
scripts/check-desktop.sh  # Rust : fmt, clippy, tests + seuil de couverture (cargo-llvm-cov)
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

## Releases (téléchargements GitHub)

L'action « Releases » (`.github/workflows/release.yml`) construit les trois apps et les joint à une
release GitHub **brouillon** : rien n'est public tant qu'elle n'est pas publiée depuis GitHub.

| Fichier | Contenu |
|---|---|
| `Rohy-android-<version>.apk` | App Android, à installer à la main (sources inconnues autorisées) |
| `Rohy-macOS-<version>.zip` | `Rohy.app` universelle (Apple Silicon et Intel) |
| `Rohy-windows-x64-<version>.zip` | `Rohy.exe` portable, sans installateur |

Créer une release : pousser une étiquette, par exemple `git tag v0.1.0 && git push origin v0.1.0`, ou
lancer l'action depuis l'onglet Actions avec une version. Une PR qui modifie l'action construit seulement.

Signatures, à savoir avant de partager :
- **Android** : signé avec la clé de release si les secrets existent, sinon avec une clé de test (le nom
  du fichier finit alors par `-test-signature` et une future version signée ne s'installera pas par-dessus).
  Créer la clé une fois, la garder hors du dépôt, puis ajouter quatre secrets au dépôt :
  `keytool -genkeypair -v -keystore rohy-release.jks -alias rohy -keyalg RSA -keysize 4096 -validity 10000`,
  puis `ROHY_KEYSTORE_BASE64` (sortie de `base64 -i rohy-release.jks`), `ROHY_KEYSTORE_PASSWORD`,
  `ROHY_KEY_ALIAS` (`rohy`) et `ROHY_KEY_PASSWORD`. Perdre cette clé empêche toute mise à jour de l'app.
- **macOS** : signature ad hoc ; sur un autre Mac, la première ouverture se fait par clic droit → Ouvrir.
  La diffusion sans avertissement demandera un certificat Developer ID et la notarisation.
- **Windows** : non signé ; SmartScreen affiche un avertissement (« Informations complémentaires » →
  « Exécuter quand même »).

## Sécurité

Le protocole est décrit dans [`docs/protocol.md`](docs/protocol.md). Il n'a pas encore été audité.
