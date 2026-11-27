package app.rohy

/** What the edge handle shows: the thin bar, the open tab, or a short result before going back to the bar. */
enum class HandleMode { REST, OPEN, SENT, EMPTY }

/**
 * Rules of the edge handle (phase 10), kept free of Android types so they are unit tested.
 * The handle's place along the edges is in [HandleTrack].
 */
object EdgeHandleRules {
    /** Fraction of the side's track where the handle starts: low on the right edge, like before. */
    const val DEFAULT_POSITION = 0.7f

    /** The open tab folds back by itself after this delay. */
    const val OPEN_TIMEOUT_MS = 4_000L

    /** "Envoyé" / "Rien à envoyer" stay this long before the bar comes back. */
    const val RESULT_MS = 1_600L

    fun visible(connected: Boolean, enabled: Boolean, canDrawOverlays: Boolean): Boolean =
        connected && enabled && canDrawOverlays

    fun clampPosition(position: Float): Float = if (position.isNaN()) DEFAULT_POSITION else position.coerceIn(0f, 1f)

    /** Result shown after "Envoyer": nothing is sent when the clipboard holds no text. */
    fun resultMode(text: String?): HandleMode = if (text.isNullOrEmpty()) HandleMode.EMPTY else HandleMode.SENT

    /** How long a mode lasts before the handle goes back to the bar; null for the bar itself. */
    fun timeoutMillis(mode: HandleMode): Long? = when (mode) {
        HandleMode.REST -> null
        HandleMode.OPEN -> OPEN_TIMEOUT_MS
        HandleMode.SENT, HandleMode.EMPTY -> RESULT_MS
    }
}
