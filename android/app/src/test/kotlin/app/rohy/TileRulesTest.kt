package app.rohy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TileRulesTest {
    @Test
    fun buttonAndDetailFollowTheTile() {
        assertEquals("Ajouter", TileRules.button(added = false))
        assertEquals("Retirer", TileRules.button(added = true))
        assertEquals("Glisse depuis le haut, touche « Envoyer ».", TileRules.detail(added = false))
        assertEquals("Dans tes réglages rapides : glisse depuis le haut.", TileRules.detail(added = true))
    }

    @Test
    fun addedOrAlreadyAddedMeansOnThePanel() {
        // Values of StatusBarManager: 0 not added, 1 already added, 2 added, negatives are errors.
        assertTrue(TileRules.onPanelAfterRequest(2, added = 2, alreadyAdded = 1))
        assertTrue(TileRules.onPanelAfterRequest(1, added = 2, alreadyAdded = 1))
        assertFalse(TileRules.onPanelAfterRequest(0, added = 2, alreadyAdded = 1))
        assertFalse(TileRules.onPanelAfterRequest(-1, added = 2, alreadyAdded = 1))
    }
}
