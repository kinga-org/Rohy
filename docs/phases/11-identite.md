# Phase 11 — Nouvelle identité « Lagon & Soleil », logo « La corde » (Android, Mac, Windows, Linux)

**Statut :** en cours (démarrée le 2026-11-24)

## Objectif
Donner à Rohy un look moderne et mignon que les gens ont envie d'utiliser, le même sur chaque
système, sans quitter les composants standard (Material 3 sur Android, AppKit sur Mac). Pas d'effet
verre : la piste « Liquid Glass » a été écartée.

## Maquette
Canvas « Rohy Android — refonte » (https://claude.ai/artifact/EytQs4bmUWfonPhdtqfp16), rangée
« Nouvelle identité — moderne et mignonne ». Références : « Accueil — Lagon & Soleil, logo « La
corde » (retenu) », « Accueil — Lagon (sombre, gris Android, retenu) » et « « La corde » sur toutes
les surfaces ». Mac et bureau : canvas « Rohy macOS — natif » et « Rohy bureau — refonte » (mise en
page des rangées « Myrtille », logo et couleurs remplacés par le choix final).

## Choix validés
- Historique du 24 nov. : Myrtille & Pêche + « Le presse-papiers » d'abord, puis le propriétaire a
  préféré le logo « La corde » dans ses couleurs d'origine, et toute l'app dans ces couleurs.
- Palette Lagon & Soleil :
  - clair : principale `#1B6E85`, douce `#D3ECF3`, soleil `#F5B71E`, soleil doux `#FFF1C7`,
    fond `#F4F9FA`, surface `#FFFFFF`, texte `#0E2A33`, texte secondaire `#47626A` ;
  - sombre : gris neutres d'Android (demande du propriétaire, pas de fond coloré) : fond `#191C1D`,
    surface et carte de connexion `#262A2C`, texte `#E1E3E3`, texte secondaire `#BFC8CA`, filets
    `#353A3C` ; le bleu-vert reste pour les accents : principale `#7FD0E6`, pastilles `#1F4B57`,
    soleil `#FFD25E`, soleil doux `#4F3D0A`.
- Logo « La corde » : corde blanche aux bouts jaunes `#F5B71E` sur carré arrondi `#1B6E85`. En une
  seule couleur (notification, tuile, icône thématique, barre des menus du Mac) : la corde et ses
  deux bouts.
- Formes : coins très arrondis (cartes 26–32 dp), bouton principal en pilule, listes groupées
  (premier et dernier élément plus arrondis), pastilles d'icône teintées (douce / accent doux).
- Accueil connecté : illustration téléphone + ordinateur qui se sourient, reliés par un pointillé et
  un cœur ; pastille « Synchronisation active ».

## Périmètre
- Thème Compose : nouvelles couleurs clair/sombre, formes.
- Icône de l'app (adaptive icon) et icône de notification.
- Accueil, appairage, injoignable, « Garder la connexion », journal, écran d'autorisation de la
  poignée : nouvelles couleurs et formes.
- Onglet de la poignée d'envoi aux couleurs de la palette.

- Mac (maquette « Rohy macOS — natif ») :
  composants natifs inchangés ; seulement l'icône de l'app, le symbole de la barre des menus (image
  modèle, estompée sans téléphone connecté), la couleur d'accent de l'app (prise en compte quand
  l'accent du système est « Multicolore ») et le logo au centre du QR code.
- Windows et Linux (maquette « Rohy bureau — refonte ») : même
  palette et mêmes formes qu'Android, carte « Connecté » avec l'illustration, logo dans l'en-tête, le
  QR code, l'icône de la zone de notification et de la fenêtre.

Hors périmètre : icône du fichier `Rohy.exe` dans l'Explorateur Windows (ressource à embarquer au
build), à faire séparément.

## Dépendances
Phase 10 (poignée d'envoi), dont l'écran et la ligne de réglage sont restylés ici.

## Tâches
- [ ] Mettre à jour la maquette des écrans restants (appairage, injoignable, fiabilité, journal)
- [x] Thème Compose (couleurs, formes) clair et sombre
- [x] Icône de l'app (adaptative, avec couche monochrome) et icône de notification / tuile
- [x] Illustration de l'accueil (dessinée en Compose, couleurs du thème)
- [x] Accueil, boutons, cartes, liste groupée « Envoyer plus vite », onglet de la poignée
- [x] Mac : icône (catalogue d'assets compilé par `actool`), accent, symbole de la barre des menus,
  logo du QR code
- [x] Windows et Linux : palette, formes, illustration, logo, icône de la zone de notification et de
  la fenêtre
- [ ] Essais réels : Mac (menu et icône), Windows, Linux
- [ ] Vérifier les autres écrans (appairage, injoignable, fiabilité, journal) face à la maquette
- [ ] Captures sur le Samsung et le Huawei, comparées à la maquette

## Critères d'acceptation
- Chaque écran correspond à sa maquette, en clair et en sombre.
- Contraste du texte ≥ 4,5:1 partout.
- Aucun composant dessiné à la main là où Material 3 en fournit un.

## Stratégie de test
Tests unitaires existants inchangés ; Lint ; captures d'écran sur le Samsung (Android 16) et le
Huawei (Android 10), en clair et en sombre.
