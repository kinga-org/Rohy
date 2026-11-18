package app.rohy

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Last clipboard transfer, shown under the connection banner. */
data class Activity(val atMillis: Long, val sent: Boolean)

/** "Texte envoyé à l'ordinateur · il y a 2 min". */
fun Activity.describe(nowMillis: Long): String =
    (if (sent) "Texte envoyé à l'ordinateur" else "Texte reçu de l'ordinateur") +
        " · " + relativeTime(nowMillis - atMillis)

/** "à l'instant", "il y a 12 s", "il y a 3 min", "il y a 2 h", "il y a 4 j". */
fun relativeTime(elapsedMillis: Long): String {
    val seconds = (elapsedMillis / 1000).coerceAtLeast(0)
    return when {
        seconds < 5 -> "à l'instant"
        seconds < 60 -> "il y a $seconds s"
        seconds < 3600 -> "il y a ${seconds / 60} min"
        seconds < 86_400 -> "il y a ${seconds / 3600} h"
        else -> "il y a ${seconds / 86_400} j"
    }
}

private val MONTHS = listOf("janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc.")

/** "18 nov." in the given time zone. */
fun shortDate(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    return "${date.dayOfMonth} ${MONTHS[date.monthValue - 1]}"
}

/** "Associé le 18 nov. · 192.168.88.3", or only the address when the date is unknown. */
fun pairedDetail(pairedAtMillis: Long?, hosts: List<String>, zone: ZoneId = ZoneId.systemDefault()): String =
    listOfNotNull(pairedAtMillis?.let { "Associé le ${shortDate(it, zone)}" }, hosts.firstOrNull())
        .joinToString(" · ")
        .ifEmpty { "Adresse trouvée sur le réseau local" }

/** Progress of the "Garder la connexion" checklist. */
data class ReliabilitySummary(val ready: Int, val total: Int, val toFix: Int, val toCheck: Int) {
    val title: String get() = "$ready réglage${if (ready > 1) "s" else ""} sur $total prêt${if (ready > 1) "s" else ""}"

    /** Line under "Garder la connexion" on the home screen. */
    val status: String
        get() = when {
            toFix > 0 -> "$toFix réglage${if (toFix > 1) "s" else ""} à corriger"
            toCheck > 0 -> "$toCheck réglage${if (toCheck > 1) "s" else ""} à vérifier"
            else -> "Tout est prêt"
        }

    val needsAttention: Boolean get() = toFix > 0
}

fun summarize(checks: List<ReliabilityCheck>) = ReliabilitySummary(
    ready = checks.count { it.state == CheckState.OK },
    total = checks.size,
    toFix = checks.count { it.state == CheckState.TO_FIX },
    toCheck = checks.count { it.state == CheckState.TO_CHECK },
)

/** Journal events grouped by day, newest first: "Aujourd'hui", "Hier", then "16 nov.". */
data class JournalDay(val title: String, val events: List<ConnectionEvent>)

fun journalDays(events: List<ConnectionEvent>, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): List<JournalDay> {
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    return events.sortedByDescending { it.atMillis }
        .groupBy { Instant.ofEpochMilli(it.atMillis).atZone(zone).toLocalDate() }
        .map { (day, dayEvents) -> JournalDay(dayTitle(day, today, dayEvents.first().atMillis, zone), dayEvents) }
}

private fun dayTitle(day: LocalDate, today: LocalDate, millis: Long, zone: ZoneId) = when (day) {
    today -> "Aujourd'hui"
    today.minusDays(1) -> "Hier"
    else -> shortDate(millis, zone)
}

/** Marker color of a journal line. */
enum class EventKind { CONNECTED, SEARCHING, PROBLEM, INFO }

fun eventKind(message: String): EventKind = when {
    message.startsWith("Connecté") -> EventKind.CONNECTED
    message.startsWith("Recherche") || message.contains("nouvel essai") -> EventKind.SEARCHING
    listOf("injoignable", "refusé", "perdu", "Autorise", "Échec", "impossible").any { message.contains(it) } -> EventKind.PROBLEM
    else -> EventKind.INFO
}

/** What the top of the home screen shows for a connection status. */
sealed interface HomeHeader {
    data class Connected(val computer: String) : HomeHeader
    /** [rejected]: the computer no longer knows this phone, only a new pairing helps. */
    data class Unreachable(
        val title: String,
        val detail: String,
        val canReconnect: Boolean,
        val rejected: Boolean = false,
    ) : HomeHeader
    data class LocalNetworkDenied(val computer: String) : HomeHeader
}

fun homeHeader(status: SyncStatus, computer: String): HomeHeader = when (status) {
    is SyncStatus.Connected -> HomeHeader.Connected(status.macName)
    is SyncStatus.LocalNetworkDenied -> HomeHeader.LocalNetworkDenied(status.macName)
    is SyncStatus.Searching -> HomeHeader.Unreachable(
        "Recherche de ${status.macName}…", "Sur le même Wi-Fi que l'ordinateur ?", canReconnect = false,
    )
    is SyncStatus.Waiting -> HomeHeader.Unreachable(
        "${status.macName} injoignable", "Nouvel essai dans ${status.retryInSeconds} s", canReconnect = true,
    )
    is SyncStatus.Rejected -> HomeHeader.Unreachable(
        "${status.macName} a refusé la connexion", "Il ne reconnaît plus ce téléphone.", canReconnect = false,
        rejected = true,
    )
    SyncStatus.Stopped, SyncStatus.NotPaired -> HomeHeader.Unreachable(
        "Synchronisation arrêtée", "Relance-la pour rejoindre $computer.", canReconnect = true,
    )
}
