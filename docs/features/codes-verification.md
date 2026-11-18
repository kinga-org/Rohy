# Codes de vérification vers le Mac

Fonction phare envisagée, en trois niveaux :
1. Semi-manuel : « Copier » dans Google Messages, puis la tuile Rohy.
2. Automatique via association `CompanionDeviceManager`, si la phase 05 le confirme.
3. Mode avancé documenté via ADB (`RECEIVE_SENSITIVE_NOTIFICATIONS`).

Jamais de permission `RECEIVE_SMS` dans la version Google Play. Voir `docs/research/`.
