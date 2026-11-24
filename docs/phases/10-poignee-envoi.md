# Phase 10 — Poignée d'envoi (Android)

**Statut :** fonctionne sur le Samsung (démarrée le 2026-11-24) ; reste l'essai sur le Huawei

## Objectif
Envoyer le presse-papiers à l'ordinateur depuis n'importe quelle app, jeux en plein écran compris, sans
passer par les réglages rapides. Android 10+ interdit de savoir quand l'utilisateur copie : Rohy ne peut
pas réagir seul à une copie, il faut un geste. La poignée rend ce geste le plus court possible.

## Maquette
Canvas « Rohy Android — refonte » (https://claude.ai/artifact/EytQs4bmUWfonPhdtqfp16), rangée
« Poignée « Envoyer à l'ordinateur » » : au repos, onglet ouvert, envoyé, rien à envoyer, masquée,
autorisation, réglage dans l'accueil, sombre.

## Périmètre
- Fine barre gris clair sur le bord droit (3 × 50 dp, à 4 dp du bord), copiée de la poignée du panneau
  Edge de Samsung, par-dessus les autres apps (`TYPE_APPLICATION_OVERLAY`, autorisation « Afficher
  par-dessus les autres apps »).
- Visible seulement si : téléphone connecté à l'ordinateur, poignée activée, non masquée, autorisation
  accordée. Elle disparaît seule à la déconnexion.
- Comme chez Samsung, seul un glissé vers le centre de l'écran ouvre l'onglet « Envoyer à
  l'ordinateur » + ✕ ; un toucher ne fait rien (pas d'ouverture en tapant au clavier près du bord).
  La zone de la poignée est exclue du geste Retour d'Android (`systemGestureExclusionRects`) ;
  TalkBack peut l'ouvrir par un clic d'accessibilité. Toucher « Envoyer » lit le presse-papiers (la
  fenêtre de l'onglet prend le focus, ce qui autorise la lecture) et l'envoie. Repli : l'activité
  transparente déjà utilisée par la tuile.
- Retour visuel : « Envoyé » (vert) ou « Rien à envoyer » (orange), puis retour à la barre. L'onglet se
  replie seul après 4 s, ou par Retour.
- ✕ masque la poignée (message avec « Annuler ») ; la notification Rohy propose alors « Afficher la
  poignée ».
- Appui long puis glisser : déplacer la poignée verticalement ; position gardée en fraction de la
  hauteur, valable en portrait comme en paysage (recalculée à la rotation).
- Accueil : ligne « Poignée d'envoi » avec interrupteur dans « Envoyer plus vite » ; écran d'explication
  avant la demande d'autorisation.

Hors périmètre : détection automatique de la copie (impossible sans privilèges ; pas de root, jamais).

## Dépendances
Phase 04 (envoi Android → ordinateur), phase 06 (service au premier plan).

## Tâches
- [x] Règles pures (visibilité, position, états, glissé d'ouverture) et tests unitaires
- [x] Fenêtre de la poignée : barre, onglet, résultats, masquage, déplacement, rotation
- [x] Réglages : stockage, interrupteur, écran d'autorisation, action de notification
- [x] Essais sur le Samsung (portrait, paysage, jeu en plein écran)
- [ ] Essai sur le Huawei (Android 10)

## Critères d'acceptation
- Dans une app en plein écran, portrait ou paysage : copier un texte, glisser la barre vers le centre
  puis toucher « Envoyer » le fait arriver sur l'ordinateur.
- La poignée n'apparaît jamais quand le téléphone n'est pas connecté, ni quand elle est désactivée ou
  masquée.
- Un toucher ou un frôlement du bord n'ouvre rien et n'envoie rien ; le geste Retour d'Android reste
  utilisable ailleurs sur le bord.

## Stratégie de test
Tests unitaires des règles ; essais manuels sur le Samsung (Android 16) et le Huawei (Android 10), en
portrait et en paysage, avec captures d'écran comparées à la maquette.
