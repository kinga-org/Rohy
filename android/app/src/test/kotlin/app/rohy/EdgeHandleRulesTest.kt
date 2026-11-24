package app.rohy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeHandleRulesTest {
    @Test
    fun visibleOnlyWhenConnectedEnabledShownAndAllowed() {
        assertTrue(EdgeHandleRules.visible(connected = true, enabled = true, hidden = false, canDrawOverlays = true))
        assertFalse(EdgeHandleRules.visible(connected = false, enabled = true, hidden = false, canDrawOverlays = true))
        assertFalse(EdgeHandleRules.visible(connected = true, enabled = false, hidden = false, canDrawOverlays = true))
        assertFalse(EdgeHandleRules.visible(connected = true, enabled = true, hidden = true, canDrawOverlays = true))
        assertFalse(EdgeHandleRules.visible(connected = true, enabled = true, hidden = false, canDrawOverlays = false))
    }

    @Test
    fun positionStaysAwayFromTheScreenEnds() {
        assertEquals(0.1f, EdgeHandleRules.clampPosition(-2f))
        assertEquals(0.9f, EdgeHandleRules.clampPosition(3f))
        assertEquals(0.5f, EdgeHandleRules.clampPosition(0.5f))
        assertEquals(EdgeHandleRules.DEFAULT_POSITION, EdgeHandleRules.clampPosition(Float.NaN))
    }

    @Test
    fun samePositionInPortraitAndLandscape() {
        // Portrait 2340 px high, landscape 1080 px high, handle 200 px: same fraction of the free height.
        val portrait = EdgeHandleRules.topOffset(0.7f, available = 2340, viewHeight = 200)
        val landscape = EdgeHandleRules.topOffset(0.7f, available = 1080, viewHeight = 200)
        assertEquals(1498, portrait)
        assertEquals(616, landscape)
        assertEquals(0.7f, EdgeHandleRules.positionFrom(portrait, 2340, 200), 0.001f)
        assertEquals(0.7f, EdgeHandleRules.positionFrom(landscape, 1080, 200), 0.001f)
    }

    @Test
    fun dragOutsideTheScreenIsClamped() {
        assertEquals(0.9f, EdgeHandleRules.positionFrom(5000, 2340, 200))
        assertEquals(0.1f, EdgeHandleRules.positionFrom(-50, 2340, 200))
        // A view taller than the screen never divides by zero.
        assertEquals(0, EdgeHandleRules.topOffset(0.5f, available = 100, viewHeight = 300))
        assertEquals(0.9f, EdgeHandleRules.positionFrom(10, 100, 300))
    }

    @Test
    fun onlyAnInwardSwipeOpensTheTab() {
        assertTrue(EdgeHandleRules.opensTab(dx = -60f, dy = 10f, threshold = 48f))
        // A tap, a short move, an outward or vertical move: nothing opens.
        assertFalse(EdgeHandleRules.opensTab(dx = 0f, dy = 0f, threshold = 48f))
        assertFalse(EdgeHandleRules.opensTab(dx = -30f, dy = 0f, threshold = 48f))
        assertFalse(EdgeHandleRules.opensTab(dx = 60f, dy = 0f, threshold = 48f))
        assertFalse(EdgeHandleRules.opensTab(dx = -60f, dy = 90f, threshold = 48f))
    }

    @Test
    fun resultDependsOnTheClipboardText() {
        assertEquals(HandleMode.EMPTY, EdgeHandleRules.resultMode(null))
        assertEquals(HandleMode.EMPTY, EdgeHandleRules.resultMode(""))
        assertEquals(HandleMode.SENT, EdgeHandleRules.resultMode("4F7K-92QX"))
    }

    @Test
    fun everyModeButTheBarFoldsBackByItself() {
        assertNull(EdgeHandleRules.timeoutMillis(HandleMode.REST))
        assertEquals(4_000L, EdgeHandleRules.timeoutMillis(HandleMode.OPEN))
        assertEquals(1_600L, EdgeHandleRules.timeoutMillis(HandleMode.SENT))
        assertEquals(1_600L, EdgeHandleRules.timeoutMillis(HandleMode.EMPTY))
    }
}
