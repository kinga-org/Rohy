package app.rohy

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class HandleSide { LEFT, RIGHT }

/** A point in pixels, in the coordinates of the handle's windows. */
data class TrackPoint(val x: Float, val y: Float)

/**
 * The path the edge handle slides along on one side of the screen (board "Poignée — déplacement le long des
 * bords et dans les coins"): straight down the side, bending with the screen's rounded corners at both ends,
 * and cut at [top] and [bottom], where Android keeps the status bar and the home gesture and the handle could
 * not be touched. Free of Android types so it is unit tested.
 *
 * @param edge x of that side's edge.
 * @param screenTop y of the physical top of the screen (negative when the status bar sits above [top]).
 * @param screenBottom y of the physical bottom of the screen.
 * @param cornerRadius radius of the screen's corners on this side; 0 for square corners.
 * @param inset distance from the edge to the middle of the bar.
 */
class HandleTrack(
    val side: HandleSide,
    edge: Float,
    top: Float,
    bottom: Float,
    screenTop: Float,
    screenBottom: Float,
    cornerRadius: Float,
    inset: Float,
) {
    private val points: List<TrackPoint>
    private val lengths: FloatArray

    /** Arc length where the straight part starts and ends; before and after, the track follows a corner. */
    val straightStart: Float
    val straightEnd: Float
    val length: Float get() = lengths.last()

    init {
        // Inward x direction: from the edge toward the middle of the screen.
        val inward = if (side == HandleSide.RIGHT) -1f else 1f
        val radius = if (cornerRadius > inset) cornerRadius else 0f
        val r = radius - inset
        val x = edge + inward * inset
        val raw = ArrayList<Pair<TrackPoint, Boolean>>()
        fun arc(centerY: Float, from: Double, to: Double) {
            val steps = max(2, ceil(r * PI / 2).toInt())
            for (i in 0..steps) {
                val angle = from + (to - from) * i / steps
                // Angle 0 points at the edge; negative angles go up.
                raw += TrackPoint(edge + inward * radius - inward * r * cos(angle).toFloat(), centerY + r * sin(angle).toFloat()) to false
            }
        }
        if (r > 0) arc(screenTop + radius, -PI / 2, 0.0)
        val straightTop = screenTop + radius
        val straightBottom = screenBottom - radius
        val stepCount = max(1, ceil(straightBottom - straightTop).toInt())
        for (i in 0..stepCount) raw += TrackPoint(x, straightTop + (straightBottom - straightTop) * i / stepCount) to true
        if (r > 0) arc(screenBottom - radius, 0.0, PI / 2)

        val kept = raw.filter { (point, _) -> point.y in top..bottom }.ifEmpty { listOf(TrackPoint(x, top) to true) }
        points = kept.map { it.first }
        lengths = FloatArray(points.size)
        var start = Float.NaN
        var end = 0f
        for (i in points.indices) {
            if (i > 0) lengths[i] = lengths[i - 1] + hypot(points[i].x - points[i - 1].x, points[i].y - points[i - 1].y)
            if (kept[i].second) {
                if (start.isNaN()) start = lengths[i]
                end = lengths[i]
            }
        }
        straightStart = if (start.isNaN()) 0f else start
        straightEnd = end
    }

    /** Keeps a bar of [barLength] whole on the track. */
    fun clampCenter(center: Float, barLength: Float): Float {
        val half = barLength / 2
        return if (length <= barLength) length / 2 else center.coerceIn(half, length - half)
    }

    fun pointAt(at: Float): TrackPoint {
        val u = at.coerceIn(0f, length)
        val i = indexAt(u)
        if (i >= points.lastIndex) return points.last()
        val span = lengths[i + 1] - lengths[i]
        val t = if (span == 0f) 0f else (u - lengths[i]) / span
        val a = points[i]
        val b = points[i + 1]
        return TrackPoint(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
    }

    /** Unit vector from the track toward the middle of the screen. */
    fun inwardNormal(at: Float): TrackPoint {
        val before = pointAt(at - 2f)
        val after = pointAt(at + 2f)
        val dx = after.x - before.x
        val dy = after.y - before.y
        val norm = hypot(dx, dy).takeIf { it > 0f } ?: return TrackPoint(if (side == HandleSide.RIGHT) -1f else 1f, 0f)
        // The track runs downward: turning its direction a quarter turn points inside.
        return if (side == HandleSide.RIGHT) TrackPoint(-dy / norm, dx / norm) else TrackPoint(dy / norm, -dx / norm)
    }

    fun inCorner(at: Float): Boolean = at < straightStart || at > straightEnd

    /** Points of a bar of [barLength] centered at [center] (already clamped). */
    fun segment(center: Float, barLength: Float): List<TrackPoint> {
        val from = (center - barLength / 2).coerceAtLeast(0f)
        val to = (center + barLength / 2).coerceAtMost(length)
        val inside = points.indices.filter { lengths[it] > from && lengths[it] < to }.map { points[it] }
        return listOf(pointAt(from)) + inside + pointAt(to)
    }

    /** Arc length of the track point nearest to ([x], [y]), for dragging. */
    fun project(x: Float, y: Float): Float {
        var best = 0
        var bestDistance = Float.MAX_VALUE
        for (i in points.indices) {
            val d = (points[i].x - x).let { it * it } + (points[i].y - y).let { it * it }
            if (d < bestDistance) {
                bestDistance = d
                best = i
            }
        }
        return lengths[best]
    }

    private fun indexAt(u: Float): Int {
        var low = 0
        var high = lengths.lastIndex
        while (low < high) {
            val mid = (low + high + 1) / 2
            if (lengths[mid] <= u) low = mid else high = mid - 1
        }
        return low
    }

    companion object {
        /**
         * Like Samsung's Edge handle, only a swipe toward the middle of the screen opens the tab: along the
         * track's inward [normal], longer than [threshold] and more inward than sideways. Taps never do.
         */
        fun opensTab(dx: Float, dy: Float, normal: TrackPoint, threshold: Float): Boolean {
            val along = dx * normal.x + dy * normal.y
            val across = abs(dx * normal.y - dy * normal.x)
            return along >= threshold && along > across
        }

        /** Side the finger is on while dragging: the handle follows it to the nearest side. */
        fun sideFor(x: Float, width: Float): HandleSide = if (x < width / 2) HandleSide.LEFT else HandleSide.RIGHT

        /** Bounds (left, top, right, bottom) of [points], grown by [margin] and kept inside the window space. */
        fun bounds(points: List<TrackPoint>, margin: Float, width: Float, height: Float): FloatArray = floatArrayOf(
            max(0f, points.minOf { it.x } - margin),
            max(0f, points.minOf { it.y } - margin),
            min(width, points.maxOf { it.x } + margin),
            min(height, points.maxOf { it.y } + margin),
        )
    }
}
