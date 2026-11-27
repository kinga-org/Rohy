# Rohy

Pont de presse-papiers entre un téléphone Android et un ordinateur macOS, Windows ou Linux : apps natives,
réseau local uniquement, chiffré de bout en bout, gratuit et libre. « Rohy » signifie « lien, corde » en
malgache.

- **Ordinateur → Android** : tout texte copié sur l'ordinateur arrive dans le presse-papiers du téléphone.
- **Android → ordinateur** : tuile Réglages rapides « Envoyer », menu Partager, menu de sélection ou action
  de la notification (Android interdit la lecture du presse-papiers en arrière-plan depuis Android 10).

| Plateforme | App | Téléchargement |
|---|---|---|
| Android 10+ | Kotlin + Compose | APK dans les [releases](https://github.com/kinga-org/Rohy/releases) |
| macOS 14+ | Swift, barre des menus | `Rohy.app` dans les releases |
| Windows 10/11 | Rust + iced, zone de notification | `Rohy.exe` dans les releases |
| Linux (x86_64) | Rust + iced (même app que Windows) | `Rohy-linux-x64-….tar.gz` dans les releases |

## Structure

| Dossier | Contenu |
|---|---|
| `mac/` | Paquet Swift : `RohyCore` (protocole, crypto), `RohyServer` (WebSocket + Bonjour), app barre de menus `Rohy`, `rohy-harness` (tests d'interopérabilité) |
| `desktop/` | Espace de travail Cargo pour Windows/Linux : `rohy-core` (protocole en Rust), `rohy-server` (WebSocket + Bonjour, `rohy-harness`), app `apps/rohy-desktop` (iced) |
| `android/` | `core` (protocole Kotlin, JVM pur) et `app` (service, tuile, partage, interface Compose) |
| `site/` | Site vitrine `rohy.kingalabs.com` : Next.js (export statique) + Worker Cloudflare pour les avis (D1) et les téléchargements (R2), voir [site/README.md](site/README.md) |
| `protocol/` | Vecteurs de test partagés par les trois implémentations (Swift, Kotlin, Rust) |
| `docs/` | Protocole, phases, backlog, étude concurrentielle, [avancement](docs/avancement/README.md) |

## Deux apps bureau, une seule expérience

L'app Mac (Swift, `mac/`) et l'app Windows/Linux (Rust + iced, `desktop/apps/rohy-desktop`) sont deux
codes différents qui doivent rester identiques pour l'utilisateur. **Toute modification de l'une impose
de vérifier l'autre**, dans la même PR ou dans une PR liée :

- mêmes fonctions, mêmes états (démarrage, attente, connecté, erreur), mêmes libellés et mêmes
  comportements (association, oubli, fermeture de la fenêtre du QR code) ;
- les textes et règles d'affichage partagés (temps relatifs, dates, ligne d'état) sont testés des deux
  côtés avec les mêmes valeurs attendues (`DesktopPresentation` en Swift, `state.rs` en Rust) ;
- l'apparence, elle, suit chaque système :
  - Mac : composants natifs (vrai menu `NSMenu` dans la barre des menus, fenêtre standard, police,
    couleur d'accent et mode clair ou sombre du système). Maquette : canvas
    « Rohy macOS — natif » (https://claude.ai/artifact/7VWrpCw2XEDyaH4Ze1tSRr) ;
  - Windows et Linux : thème Rohy (vert foncé, Inter, cartes), choix « Apparence » dans le menu de
    l'icône. Maquette : canvas « Rohy bureau — refonte »
    (https://claude.ai/artifact/StchZXp6RPY4E4Nv2CwSc9).

La description de la PR indique ce qui a été vérifié de l'autre côté, ou pourquoi rien n'est à faire.

## Prérequis

- macOS 14+, Xcode (Swift 6).
- Android SDK (plateforme 36) et un JDK 21 (celui d'Android Studio convient).
- Rust stable, `cargo install cargo-llvm-cov` et `rustup component add llvm-tools-preview` (bureau).
- Node.js 22+ (site vitrine).

## Commandes

```bash
scripts/setup.sh          # installe les hooks Git du dépôt
scripts/check-mac.sh      # tests Swift + seuil de couverture
scripts/check-desktop.sh  # Rust : fmt, clippy, tests + seuil de couverture (cargo-llvm-cov)
scripts/check-android.sh  # tests Kotlin (dont client Kotlin ↔ vrai serveur Swift) + Kover + build debug
scripts/check-site.sh     # site vitrine : types, tests, export statique
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

L'action « Releases » (`.github/workflows/release.yml`) construit les apps et les joint à une
release GitHub **brouillon** : rien n'est public tant qu'elle n'est pas publiée depuis GitHub.

| Fichier | Contenu |
|---|---|
| `Rohy-android-<version>.apk` | App Android, à installer à la main (sources inconnues autorisées) |
| `Rohy-macOS-<version>.zip` | `Rohy.app` universelle (Apple Silicon et Intel) |
| `Rohy-windows-x64-<version>.zip` | `Rohy.exe` portable, sans installateur |
| `Rohy-linux-x64-<version>.tar.gz` | Exécutable `rohy` portable (x86_64, glibc 2.35+ : Ubuntu 22.04, Debian 12, Fedora 36 ou plus récents) |

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
- **Linux** : extraire puis lancer `./rohy` (`tar -xzf Rohy-linux-x64-<version>.tar.gz && Rohy/rohy`).
  L'icône passe par la zone de notification (StatusNotifierItem) : native sous KDE, Xfce, Cinnamon ;
  sous GNOME, il faut l'extension « AppIndicator and KStatusNotifierItem Support » (installée d'office
  sur Ubuntu). La clé privée va dans le trousseau du bureau (Secret Service : GNOME Keyring ou KWallet).
  Sous GNOME en Wayland, la copie n'est pas détectée toute seule : envoyer depuis le menu de l'icône.

## Sécurité

Le protocole est décrit dans [`docs/protocol.md`](docs/protocol.md). Il n'a pas encore été audité.
