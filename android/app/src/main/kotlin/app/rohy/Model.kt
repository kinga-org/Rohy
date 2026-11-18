package app.rohy

import app.rohy.core.Base64Url
import app.rohy.core.Fingerprint
import app.rohy.core.PairingPayload

/** The Mac this phone is paired with. Its public key is not a secret. */
data class PairedMac(val publicKey: ByteArray, val name: String, val hosts: List<String>, val port: Int) {
    val fingerprint: String get() = Fingerprint.of(publicKey)

    /** Single-line form stored in preferences: key|port|hosts|name. */
    fun encode(): String = listOf(Base64Url.encode(publicKey), port.toString(), hosts.joinToString(","), name).joinToString("|")

    override fun equals(other: Any?) = other is PairedMac && publicKey.contentEquals(other.publicKey) &&
        name == other.name && hosts == other.hosts && port == other.port

    override fun hashCode() = publicKey.contentHashCode()

    companion object {
        fun from(payload: PairingPayload) = PairedMac(payload.publicKey, payload.name, payload.hosts, payload.port)

        fun decode(value: String?): PairedMac? {
            val parts = value?.split("|", limit = 4)?.takeIf { it.size == 4 } ?: return null
            val key = Base64Url.decode(parts[0])?.takeIf { it.size == 32 } ?: return null
            val port = parts[1].toIntOrNull() ?: return null
            return PairedMac(key, parts[3], parts[2].split(',').filter { it.isNotEmpty() }, port)
        }
    }
}

/** Connection state shown in the app, the notification and the tile. */
sealed interface SyncStatus {
    data object NotPaired : SyncStatus
    data object Stopped : SyncStatus
    data class Searching(val macName: String) : SyncStatus
    data class Connected(val macName: String) : SyncStatus
    data class Waiting(val macName: String, val retryInSeconds: Long) : SyncStatus
    data class Rejected(val macName: String) : SyncStatus
    data class LocalNetworkDenied(val macName: String) : SyncStatus

    val label: String
        get() = when (this) {
            NotPaired -> "Aucun ordinateur associé"
            Stopped -> "Synchronisation arrêtée"
            is Searching -> "Recherche de $macName…"
            is Connected -> "Connecté à $macName"
            is Waiting -> "$macName injoignable, nouvel essai dans $retryInSeconds s"
            is Rejected -> "$macName a refusé la connexion. Associe-le à nouveau."
            is LocalNetworkDenied -> "Autorise l'accès au réseau local pour joindre $macName."
        }

    val isConnected: Boolean get() = this is Connected
}

/** Host candidates in order: address found by Bonjour/NSD first, then the ones from the QR code. */
fun connectionCandidates(discovered: String?, mac: PairedMac): List<String> =
    (listOfNotNull(discovered) + mac.hosts).distinct()
