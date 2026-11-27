package app.rohy

/** Texts of the "Tuile des réglages rapides" row, which follows whether the tile is on the panel. */
object TileRules {
    fun button(added: Boolean): String = if (added) "Retirer" else "Ajouter"

    fun detail(added: Boolean): String =
        if (added) "Dans tes réglages rapides : glisse depuis le haut." else "Glisse depuis le haut, touche « Envoyer »."

    /** Results of `StatusBarManager.requestAddTileService` meaning the tile is now on the panel. */
    fun onPanelAfterRequest(result: Int, added: Int, alreadyAdded: Int): Boolean = result == added || result == alreadyAdded
}
