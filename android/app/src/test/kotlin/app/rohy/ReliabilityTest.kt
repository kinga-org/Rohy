package app.rohy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionJournalTest {
    @Test
    fun keepsNewestFirstWithinCapacity() {
        val journal = ConnectionJournal(capacity = 3)
        (1..5).forEach { journal.add(it.toLong(), "event $it") }
        assertEquals(listOf(5L, 4L, 3L), journal.all().map { it.atMillis })
    }

    @Test
    fun roundTripsAndSanitizesSeparators() {
        val journal = ConnectionJournal()
        journal.add(1_000, "Connexion fermée :\tcode=4400\nraison")
        journal.add(2_000, "Connecté à Mac")
        val decoded = ConnectionJournal.decode(journal.encode())
        assertEquals(journal.all(), decoded.all())
        assertEquals("Connexion fermée : code=4400 raison", decoded.all()[1].message)
    }

    @Test
    fun ignoresCorruptedLinesAndRespectsCapacityOnDecode() {
        val decoded = ConnectionJournal.decode("abc\n\tvide\n12\tok\nx\ty\n13\tok2", capacity = 1)
        assertEquals(listOf(ConnectionEvent(12, "ok")), decoded.all())
        assertTrue(ConnectionJournal.decode(null).all().isEmpty())
    }
}

class ReliabilityChecksTest {
    private val healthy = DeviceFacts("samsung", localNetworkGranted = null, notificationsEnabled = true, ignoringBatteryOptimizations = true, backgroundRestricted = false)

    @Test
    fun samsungGetsManualChecksAndReadableStates() {
        val checks = reliabilityChecks(healthy)
        assertEquals(
            listOf(CheckId.NOTIFICATIONS, CheckId.BATTERY_OPTIMIZATION, CheckId.BACKGROUND_RESTRICTION, CheckId.SAMSUNG_NEVER_SLEEPING, CheckId.RECENTS_LOCK),
            checks.map { it.id },
        )
        assertEquals(listOf(CheckState.OK, CheckState.OK, CheckState.OK, CheckState.TO_CHECK, CheckState.TO_CHECK), checks.map { it.state })
        assertTrue(DeviceFacts("SAMSUNG", null, true, true, false).isSamsung)
    }

    @Test
    fun problemsAreFlaggedToFix() {
        val checks = reliabilityChecks(DeviceFacts("Google", false, false, false, true))
        assertEquals(CheckId.LOCAL_NETWORK, checks.first().id)
        assertEquals(4, checks.size)
        assertTrue(checks.all { it.state == CheckState.TO_FIX })
    }

    @Test
    fun retriesShareOneJournalGroup() {
        assertEquals(SyncStatus.Searching("Mac").journalGroup, SyncStatus.Waiting("Mac", 4).journalGroup)
        assertTrue(SyncStatus.Connected("Mac").journalGroup != SyncStatus.Waiting("Mac", 4).journalGroup)
        assertEquals("Stopped", SyncStatus.Stopped.journalGroup)
    }

    @Test
    fun grantedLocalNetworkIsOk() {
        val check = reliabilityChecks(healthy.copy(localNetworkGranted = true)).first()
        assertEquals(CheckId.LOCAL_NETWORK to CheckState.OK, check.id to check.state)
    }

    @Test
    fun reconnectsOnNetworkReturnOnlyWhenNotConnected() {
        assertTrue(SyncStatus.Waiting("Mac", 8).reconnectsOnNetworkReturn)
        assertFalse(SyncStatus.Searching("Mac").reconnectsOnNetworkReturn)
        assertFalse(SyncStatus.Connected("Mac").reconnectsOnNetworkReturn)
        assertFalse(SyncStatus.Rejected("Mac").reconnectsOnNetworkReturn)
        assertFalse(SyncStatus.Stopped.reconnectsOnNetworkReturn)
    }
}
