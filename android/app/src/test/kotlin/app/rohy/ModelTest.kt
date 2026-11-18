package app.rohy

import app.rohy.core.Fingerprint
import app.rohy.core.PairingPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelTest {
    private val mac = PairedMac(ByteArray(32) { 5 }, "Mac | test", listOf("192.168.1.2", "10.0.0.3"), 47823)

    @Test
    fun pairedMacRoundTripsThroughPreferencesFormat() {
        assertEquals(mac, PairedMac.decode(mac.encode()))
        assertEquals(mac.hashCode(), PairedMac.decode(mac.encode()).hashCode())
        assertEquals(Fingerprint.of(mac.publicKey), mac.fingerprint)
        assertEquals(emptyList<String>(), PairedMac.decode(mac.copy(hosts = emptyList()).encode())!!.hosts)
    }

    @Test
    fun pairedMacRejectsCorruptedValues() {
        assertNull(PairedMac.decode(null))
        assertNull(PairedMac.decode("a|b"))
        assertNull(PairedMac.decode("AAAA|1|h|n"))
        assertNull(PairedMac.decode(mac.encode().replace("|47823|", "|x|")))
        assertNotEquals(mac, mac.copy(port = 1))
        assertNotEquals(mac, Any())
    }

    @Test
    fun pairedMacFromPairingPayload() {
        val payload = PairingPayload(ByteArray(32) { 1 }, ByteArray(32) { 2 }, "Mac", listOf("h"), 9)
        assertEquals(PairedMac(payload.publicKey, "Mac", listOf("h"), 9), PairedMac.from(payload))
    }

    @Test
    fun statusLabelsAreUserFacing() {
        assertEquals("Aucun ordinateur associé", SyncStatus.NotPaired.label)
        assertEquals("Synchronisation arrêtée", SyncStatus.Stopped.label)
        assertEquals("Recherche de Mac…", SyncStatus.Searching("Mac").label)
        assertEquals("Connecté à Mac", SyncStatus.Connected("Mac").label)
        assertEquals("Mac injoignable, nouvel essai dans 4 s", SyncStatus.Waiting("Mac", 4).label)
        assertTrue(SyncStatus.Rejected("Mac").label.startsWith("Mac a refusé"))
        assertEquals("Autorise l'accès au réseau local pour joindre Mac.", SyncStatus.LocalNetworkDenied("Mac").label)
        assertTrue(SyncStatus.Connected("Mac").isConnected)
        assertFalse(SyncStatus.Searching("Mac").isConnected)
    }

    @Test
    fun candidatesPutDiscoveredAddressFirstWithoutDuplicates() {
        assertEquals(listOf("192.168.1.9", "192.168.1.2", "10.0.0.3"), connectionCandidates("192.168.1.9", mac))
        assertEquals(listOf("192.168.1.2", "10.0.0.3"), connectionCandidates("192.168.1.2", mac))
        assertEquals(mac.hosts, connectionCandidates(null, mac))
    }
}
