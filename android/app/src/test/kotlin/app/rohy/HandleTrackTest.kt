package app.rohy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class HandleTrackTest {
    // A 1080 × 2201 window space under a 97 px status bar, above a 42 px gesture area, 120 px corners.
    private fun track(side: HandleSide, radius: Float = 120f) = HandleTrack(
        side = side,
        edge = if (side == HandleSide.RIGHT) 1080f else 0f,
        top = 0f,
        bottom = 2201f,
        screenTop = -97f,
        screenBottom = 2243f,
        cornerRadius = radius,
        inset = 12f,
    )

    @Test
    fun squareCornersGiveAStraightTrackFromTopToBottom() {
        val t = track(HandleSide.RIGHT, radius = 0f)
        assertEquals(2201f, t.length, 1f)
        assertEquals(TrackPoint(1068f, 0f), t.pointAt(0f))
        assertEquals(1068f, t.pointAt(1000f).x, 0.01f)
        assertFalse(t.inCorner(1000f))
        val n = t.inwardNormal(1000f)
        assertEquals(-1f, n.x, 0.001f)
        assertEquals(0f, n.y, 0.001f)
    }

    @Test
    fun roundedCornersBendTheTrackButStayOutOfTheSystemZones() {
        val t = track(HandleSide.RIGHT)
        val end = t.pointAt(t.length)
        // The bottom corner bends inward and stops at the gesture area.
        assertTrue(end.x < 1068f)
        assertEquals(2201f, end.y, 1.5f)
        assertTrue(t.inCorner(t.length - 1f))
        // On the corner, the normal points to the corner's center (inward and up).
        val n = t.inwardNormal(t.length - 3f)
        assertTrue(n.x < 0f && n.y < 0f)
        assertEquals(1f, hypot(n.x, n.y), 0.001f)
        // Most of the top corner lies under the status bar: only its lower end is on the track, bending inward.
        assertTrue(t.inCorner(1f))
        assertTrue(t.pointAt(0f).x < 1068f)
        assertFalse(t.inCorner(200f))
        assertTrue(t.inwardNormal(3f).y > 0f)
    }

    @Test
    fun leftTrackMirrorsTheRightOne() {
        val left = track(HandleSide.LEFT)
        val right = track(HandleSide.RIGHT)
        assertEquals(right.length, left.length, 0.5f)
        assertEquals(1080f - right.pointAt(500f).x, left.pointAt(500f).x, 0.5f)
        assertEquals(1f, left.inwardNormal(500f).x, 0.001f)
    }

    @Test
    fun barsStayWholeAndDraggingFindsTheNearestPlace() {
        val t = track(HandleSide.RIGHT)
        assertEquals(70f, t.clampCenter(-10f, 140f))
        assertEquals(t.length - 70f, t.clampCenter(9999f, 140f))
        val center = t.clampCenter(800f, 140f)
        val bar = t.segment(center, 140f)
        assertEquals(800f - 70f, bar.first().y, 1f)
        assertEquals(800f + 70f, bar.last().y, 1f)
        assertEquals(800f, t.project(900f, 800f), 1.5f)
        // A track shorter than the bar centers it.
        val tiny = HandleTrack(HandleSide.RIGHT, 100f, 0f, 50f, 0f, 50f, 0f, 4f)
        assertEquals(tiny.length / 2, tiny.clampCenter(0f, 140f))
    }

    @Test
    fun onlyAnInwardSwipeOpensTheTab() {
        val right = TrackPoint(-1f, 0f)
        assertTrue(HandleTrack.opensTab(dx = -60f, dy = 10f, normal = right, threshold = 48f))
        assertFalse(HandleTrack.opensTab(dx = 0f, dy = 0f, normal = right, threshold = 48f))
        assertFalse(HandleTrack.opensTab(dx = -30f, dy = 0f, normal = right, threshold = 48f))
        assertFalse(HandleTrack.opensTab(dx = 60f, dy = 0f, normal = right, threshold = 48f))
        assertFalse(HandleTrack.opensTab(dx = -60f, dy = 90f, normal = right, threshold = 48f))
        // In a bottom corner, inward is up and to the side.
        val corner = TrackPoint(-0.7071f, -0.7071f)
        assertTrue(HandleTrack.opensTab(dx = -50f, dy = -50f, normal = corner, threshold = 48f))
        assertFalse(HandleTrack.opensTab(dx = -70f, dy = 0f, normal = corner, threshold = 60f))
    }

    @Test
    fun sideAndBounds() {
        assertEquals(HandleSide.LEFT, HandleTrack.sideFor(100f, 1080f))
        assertEquals(HandleSide.RIGHT, HandleTrack.sideFor(900f, 1080f))
        val box = HandleTrack.bounds(listOf(TrackPoint(1068f, 100f), TrackPoint(1068f, 240f)), 30f, 1080f, 2201f)
        assertEquals(listOf(1038f, 70f, 1080f, 270f), box.toList())
    }
}
