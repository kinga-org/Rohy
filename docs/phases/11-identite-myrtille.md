# Phase 11 — Nouvelle identité « Myrtille & Pêche » (Android, Mac, Windows, Linux)

**Statut :** en cours (démarrée le 2026-11-24)

## Objectif
Donner à l'app Android un look moderne et mignon que les gens ont envie d'utiliser, sans quitter les
composants Android standard (Material 3). Pas d'effet verre : la piste « Liquid Glass » a été écartée.

## Maquette
Canvas « Rohy Android — refonte » (https://claude.ai/artifact/EytQs4bmUWfonPhdtqfp16), rangée
« Nouvelle identité — moderne et mignonne ». Écrans de référence : « Accueil — Myrtille & Pêche
(retenu) » et « Accueil — Myrtille (sombre, retenu) ». Planches « Nouveaux logos » (piste 2, « Le presse-papiers ») et « Couleurs » (Myrtille &
Pêche).

## Choix validés
- Palette Myrtille & Pêche :
  - clair : principale `#5653D0`, douce `#E5E4FB`, accent `#FF9F6E`, accent doux `#FFE8DA`,
    fond `#F8F7FD`, surface `#FFFFFF`, texte `#1D1B39`, texte secondaire `#57557A` ;
  - sombre : gris neutres d'Android (demande du propriétaire, pas de fond bleu) : fond `#1C1B1F`,
    surface et carte de connexion `#2B2930`, texte `#E6E1E5`, texte secondaire `#CAC4D0`, filets
    `#3A3840` ; la myrtille reste pour les accents : principale `#BDBBFF`, pastilles `#3B3870`,
    accent `#FFB794`, accent doux `#5A3424`.
- Logo : d'abord « Le presse-papiers » (presse-papiers qui sourit) ; en essai depuis le 24 nov. : « La
  corde » (corde blanche aux bouts pêche) sur carré arrondi `#5653D0`. Icône de
  notification : silhouette d'une seule couleur.
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

- Mac (maquette « Rohy macOS — natif », rangée « Identité Myrtille & Pêche — toujours natif ») :
  composants natifs inchangés ; seulement l'icône de l'app, le symbole de la barre des menus (image
  modèle, estompée sans téléphone connecté), la couleur d'accent de l'app (prise en compte quand
  l'accent du système est « Multicolore ») et le logo au centre du QR code.
- Windows et Linux (maquette « Rohy bureau — refonte », rangée « Identité Myrtille & Pêche ») : même
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
