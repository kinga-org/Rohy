package app.rohy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeHandleRulesTest {
    @Test
    fun visibleOnlyWhenConnectedEnabledAndAllowed() {
        assertTrue(EdgeHandleRules.visible(connected = true, enabled = true, canDrawOverlays = true))
        assertFalse(EdgeHandleRules.visible(connected = false, enabled = true, canDrawOverlays = true))
        assertFalse(EdgeHandleRules.visible(connected = true, enabled = false, canDrawOverlays = true))
        assertFalse(EdgeHandleRules.visible(connected = true, enabled = true, canDrawOverlays = false))
    }

    @Test
    fun positionIsAFractionOfTheTrack() {
        assertEquals(0f, EdgeHandleRules.clampPosition(-2f))
        assertEquals(1f, EdgeHandleRules.clampPosition(3f))
        assertEquals(0.5f, EdgeHandleRules.clampPosition(0.5f))
        assertEquals(EdgeHandleRules.DEFAULT_POSITION, EdgeHandleRules.clampPosition(Float.NaN))
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
