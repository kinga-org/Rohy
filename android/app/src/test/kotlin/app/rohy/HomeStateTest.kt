package app.rohy

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeStateTest {
    private val zone = ZoneId.of("Indian/Antananarivo")
    private fun at(day: Int, hour: Int, minute: Int = 0) =
        ZonedDateTime.of(2026, 11, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun relativeTimesReadNaturally() {
        assertEquals("à l'instant", relativeTime(-3_000))
        assertEquals("à l'instant", relativeTime(4_000))
        assertEquals("il y a 12 s", relativeTime(12_000))
        assertEquals("il y a 3 min", relativeTime(180_000))
        assertEquals("il y a 2 h", relativeTime(7_200_000))
        assertEquals("il y a 4 j", relativeTime(4 * 86_400_000L))
        assertEquals("Texte envoyé à l'ordinateur · il y a 2 min", Activity(0, sent = true).describe(120_000))
        assertEquals("Texte reçu de l'ordinateur · à l'instant", Activity(0, sent = false).describe(1_000))
    }

    @Test
    fun pairedDetailShowsDateAndAddress() {
        assertEquals("18 nov.", shortDate(at(18, 16, 49), zone))
        assertEquals("Associé le 18 nov. · 192.168.88.3", pairedDetail(at(18, 16), listOf("192.168.88.3", "10.0.0.2"), zone))
        assertEquals("192.168.88.3", pairedDetail(null, listOf("192.168.88.3"), zone))
        assertEquals("Associé le 2 janv.", pairedDetail(ZonedDateTime.of(2027, 1, 2, 9, 0, 0, 0, zone).toInstant().toEpochMilli(), emptyList(), zone))
        assertEquals("Adresse trouvée sur le réseau local", pairedDetail(null, emptyList(), zone))
    }

    @Test
    fun summaryCountsChecks() {
        fun check(state: CheckState) = ReliabilityCheck(CheckId.NOTIFICATIONS, "t", "e", state)
        val mixed = summarize(
            listOf(CheckState.OK, CheckState.OK, CheckState.OK, CheckState.OK, CheckState.TO_FIX, CheckState.TO_FIX).map(::check),
        )
        assertEquals("4 réglages sur 6 prêts", mixed.title)
        assertEquals("2 réglages à corriger", mixed.status)
        assertTrue(mixed.needsAttention)
        val toCheck = summarize(listOf(check(CheckState.OK), check(CheckState.TO_CHECK)))
        assertEquals("1 réglage sur 2 prêt", toCheck.title)
        assertEquals("1 réglage à vérifier", toCheck.status)
        assertFalse(toCheck.needsAttention)
        assertEquals("1 réglage à corriger", summarize(listOf(check(CheckState.TO_FIX))).status)
        assertEquals("2 réglages à vérifier", summarize(listOf(check(CheckState.TO_CHECK), check(CheckState.TO_CHECK))).status)
        assertEquals("Tout est prêt", summarize(listOf(check(CheckState.OK))).status)
    }

    @Test
    fun journalIsGroupedByDayNewestFirst() {
        val events = listOf(
            ConnectionEvent(at(16, 10), "Démarrage"),
            ConnectionEvent(at(18, 16, 30), "MacBook-Pro injoignable"),
            ConnectionEvent(at(17, 22, 12), "Connecté à MacBook-Pro"),
            ConnectionEvent(at(18, 16, 49), "Connecté à MacBook-Pro"),
        )
        val days = journalDays(events, at(18, 17), zone)
        assertEquals(listOf("Aujourd'hui", "Hier", "16 nov."), days.map { it.title })
        assertEquals(listOf(at(18, 16, 49), at(18, 16, 30)), days[0].events.map { it.atMillis })
        assertTrue(journalDays(emptyList(), 0, zone).isEmpty())
    }

    @Test
    fun eventKindsFollowMessages() {
        assertEquals(EventKind.CONNECTED, eventKind("Connecté à MacBook-Pro (192.168.88.3)"))
        assertEquals(EventKind.SEARCHING, eventKind("Recherche de MacBook-Pro…"))
        assertEquals(EventKind.SEARCHING, eventKind("Mac injoignable, nouvel essai dans 8 s"))
        assertEquals(EventKind.PROBLEM, eventKind("Connexion perdue (code=-, EOF)"))
        assertEquals(EventKind.PROBLEM, eventKind("Mac a refusé la connexion. Associe-le à nouveau."))
        assertEquals(EventKind.PROBLEM, eventKind("Réseau perdu"))
        assertEquals(EventKind.INFO, eventKind("Wi-Fi retrouvé"))
    }

    @Test
    fun headerFollowsStatus() {
        assertEquals(HomeHeader.Connected("Mac"), homeHeader(SyncStatus.Connected("Mac"), "Mac"))
        assertEquals(HomeHeader.LocalNetworkDenied("Mac"), homeHeader(SyncStatus.LocalNetworkDenied("Mac"), "Mac"))
        val waiting = homeHeader(SyncStatus.Waiting("Mac", 8), "Mac") as HomeHeader.Unreachable
        assertEquals("Mac injoignable", waiting.title)
        assertEquals("Nouvel essai dans 8 s", waiting.detail)
        assertTrue(waiting.canReconnect)
        assertFalse((homeHeader(SyncStatus.Searching("Mac"), "Mac") as HomeHeader.Unreachable).canReconnect)
        val rejected = homeHeader(SyncStatus.Rejected("Mac"), "Mac") as HomeHeader.Unreachable
        assertFalse(rejected.canReconnect)
        assertTrue(rejected.rejected)
        assertFalse(waiting.rejected)
        val stopped = homeHeader(SyncStatus.Stopped, "Mac") as HomeHeader.Unreachable
        assertEquals("Synchronisation arrêtée", stopped.title)
        assertTrue(stopped.canReconnect)
        assertEquals(stopped, homeHeader(SyncStatus.NotPaired, "Mac"))
    }
}
