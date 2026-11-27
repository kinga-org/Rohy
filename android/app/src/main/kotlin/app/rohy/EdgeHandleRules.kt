package app.rohy

import kotlin.math.roundToInt

/** What the edge handle shows: the thin bar, the open tab, or a short result before going back to the bar. */
enum class HandleMode { REST, OPEN, SENT, EMPTY }

/**
 * Rules of the edge handle (phase 10), kept free of Android types so they are unit tested.
 * The position is a fraction of the usable height, so it means the same place in portrait and landscape.
 */
object EdgeHandleRules {
    const val DEFAULT_POSITION = 0.7f
    const val MIN_POSITION = 0.1f
    const val MAX_POSITION = 0.9f

    /** The open tab folds back by itself after this delay. */
    const val OPEN_TIMEOUT_MS = 4_000L

    /** "Envoyé" / "Rien à envoyer" stay this long before the bar comes back. */
    const val RESULT_MS = 1_600L

    fun visible(connected: Boolean, enabled: Boolean, canDrawOverlays: Boolean): Boolean =
        connected && enabled && canDrawOverlays

    fun clampPosition(position: Float): Float =
        if (position.isNaN()) DEFAULT_POSITION else position.coerceIn(MIN_POSITION, MAX_POSITION)

    /** Top offset of a view of [viewHeight] px placed at [position] within [available] px. */
    fun topOffset(position: Float, available: Int, viewHeight: Int): Int =
        ((available - viewHeight).coerceAtLeast(0) * clampPosition(position)).roundToInt()

    /** Inverse of [topOffset], used when the handle is dragged. */
    fun positionFrom(top: Int, available: Int, viewHeight: Int): Float {
        val range = (available - viewHeight).coerceAtLeast(1)
        return clampPosition(top.toFloat() / range)
    }

    /**
     * Like Samsung's Edge handle: only a swipe toward the middle of the screen (leftward, from the right
     * edge) opens the tab, and it must be more horizontal than vertical. Taps and vertical moves never do.
     */
    fun opensTab(dx: Float, dy: Float, threshold: Float): Boolean = dx <= -threshold && -dx > kotlin.math.abs(dy)

    /** Result shown after "Envoyer": nothing is sent when the clipboard holds no text. */
    fun resultMode(text: String?): HandleMode = if (text.isNullOrEmpty()) HandleMode.EMPTY else HandleMode.SENT

    /** How long a mode lasts before the handle goes back to the bar; null for the bar itself. */
    fun timeoutMillis(mode: HandleMode): Long? = when (mode) {
        HandleMode.REST -> null
        HandleMode.OPEN -> OPEN_TIMEOUT_MS
        HandleMode.SENT, HandleMode.EMPTY -> RESULT_MS
    }
}
