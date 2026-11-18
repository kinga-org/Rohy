package app.rohy

/** One timestamped line of the connection journal. */
data class ConnectionEvent(val atMillis: Long, val message: String)

/**
 * Last connection events, newest first, bounded to [capacity]. Persisted as text so an endurance test
 * (hours with the screen off) can be checked afterwards in the app or through adb.
 */
class ConnectionJournal(private val capacity: Int = DEFAULT_CAPACITY, initial: List<ConnectionEvent> = emptyList()) {
    private val events = ArrayDeque(initial.take(capacity))

    @Synchronized
    fun add(atMillis: Long, message: String) {
        events.addFirst(ConnectionEvent(atMillis, message.replace(Regex("[\\t\\n\\r]+"), " ").trim()))
        while (events.size > capacity) events.removeLast()
    }

    @Synchronized
    fun all(): List<ConnectionEvent> = events.toList()

    @Synchronized
    fun encode(): String = events.joinToString("\n") { "${it.atMillis}\t${it.message}" }

    companion object {
        const val DEFAULT_CAPACITY = 100

        fun decode(text: String?, capacity: Int = DEFAULT_CAPACITY): ConnectionJournal {
            val events = text.orEmpty().lineSequence().mapNotNull { line ->
                val tab = line.indexOf('\t')
                val at = if (tab > 0) line.substring(0, tab).toLongOrNull() else null
                at?.let { ConnectionEvent(it, line.substring(tab + 1)) }
            }.toList()
            return ConnectionJournal(capacity, events)
        }
    }
}

/** What Android lets the app read about the settings that keep the connection alive. */
data class DeviceFacts(
    val manufacturer: String,
    val notificationsEnabled: Boolean,
    val ignoringBatteryOptimizations: Boolean,
    val backgroundRestricted: Boolean,
) {
    val isSamsung: Boolean get() = manufacturer.equals("samsung", ignoreCase = true)
}

enum class CheckState { OK, TO_FIX, TO_CHECK }

enum class CheckId { NOTIFICATIONS, BATTERY_OPTIMIZATION, BACKGROUND_RESTRICTION, SAMSUNG_NEVER_SLEEPING, RECENTS_LOCK }

data class ReliabilityCheck(val id: CheckId, val title: String, val explanation: String, val state: CheckState)

/**
 * Checklist shown in "Garder la connexion". States are real when Android exposes them;
 * Samsung-only settings cannot be read by apps, so they stay "à vérifier".
 */
fun reliabilityChecks(facts: DeviceFacts): List<ReliabilityCheck> = buildList {
    add(
        ReliabilityCheck(
            CheckId.NOTIFICATIONS, "Notifications",
            "La notification permanente garde le service actif et porte le bouton d'envoi.",
            if (facts.notificationsEnabled) CheckState.OK else CheckState.TO_FIX,
        ),
    )
    add(
        ReliabilityCheck(
            CheckId.BATTERY_OPTIMIZATION, "Optimisation de batterie désactivée",
            "Sinon Android coupe la connexion quand l'écran est éteint.",
            if (facts.ignoringBatteryOptimizations) CheckState.OK else CheckState.TO_FIX,
        ),
    )
    add(
        ReliabilityCheck(
            CheckId.BACKGROUND_RESTRICTION, "Batterie non restreinte",
            "Le mode « Restreint » interdit à Rohy de tourner en arrière-plan.",
            if (facts.backgroundRestricted) CheckState.TO_FIX else CheckState.OK,
        ),
    )
    if (facts.isSamsung) {
        add(
            ReliabilityCheck(
                CheckId.SAMSUNG_NEVER_SLEEPING, "Applis jamais en veille",
                "Entretien de l'appareil → Batterie → Limites d'utilisation en arrière-plan → Applis jamais en veille → ajouter Rohy.",
                CheckState.TO_CHECK,
            ),
        )
        add(
            ReliabilityCheck(
                CheckId.RECENTS_LOCK, "Garder ouvert dans les apps récentes",
                "Ouvre les apps récentes, touche l'icône de Rohy, puis « Garder ouvert ».",
                CheckState.TO_CHECK,
            ),
        )
    }
}

/** A Wi-Fi comeback should not wait for the backoff delay (a search already in progress continues). */
val SyncStatus.reconnectsOnNetworkReturn: Boolean
    get() = this is SyncStatus.Waiting
