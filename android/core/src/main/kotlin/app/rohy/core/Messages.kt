package app.rohy.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.UUID

internal val json = Json {
    encodeDefaults = true
    explicitNulls = false
    ignoreUnknownKeys = true
}

@Serializable
data class Hello(val t: String = "hello", val v: Int = RohyProtocol.VERSION, val pk: String, val n: String, val name: String)

@Serializable
data class Welcome(val t: String = "welcome", val v: Int = RohyProtocol.VERSION, val pk: String, val n: String, val status: String) {
    companion object {
        const val TRUSTED = "trusted"
        const val PAIRING = "pairing"
    }
}

data class Clip(val id: String = UUID.randomUUID().toString(), val text: String, val sensitive: Boolean = false)

/** Messages carried inside encrypted frames. */
sealed class SecureMessage {
    data class Pair(val token: ByteArray) : SecureMessage() {
        override fun equals(other: Any?) = other is Pair && token.contentEquals(other.token)
        override fun hashCode() = token.contentHashCode()
    }

    data class Paired(val name: String) : SecureMessage()
    data class ClipMessage(val clip: Clip) : SecureMessage()
    data object Ping : SecureMessage()
    data object Pong : SecureMessage()

    @Serializable
    private data class Envelope(
        val t: String,
        val token: String? = null,
        val name: String? = null,
        val id: String? = null,
        val text: String? = null,
        val sensitive: Boolean? = null,
    )

    fun encode(): ByteArray {
        val envelope = when (this) {
            is Pair -> Envelope("pair", token = Base64Url.encode(token))
            is Paired -> Envelope("paired", name = name)
            is ClipMessage -> Envelope("clip", id = clip.id, text = clip.text, sensitive = clip.sensitive)
            Ping -> Envelope("ping")
            Pong -> Envelope("pong")
        }
        return json.encodeToString(Envelope.serializer(), envelope).toByteArray()
    }

    companion object {
        fun decode(bytes: ByteArray): SecureMessage? {
            val envelope = runCatching { json.decodeFromString(Envelope.serializer(), bytes.decodeToString()) }
                .getOrNull() ?: return null
            return when (envelope.t) {
                "pair" -> envelope.token?.let(Base64Url::decode)?.let { Pair(it) }
                "paired" -> envelope.name?.let { Paired(it) }
                "clip" -> if (envelope.id != null && envelope.text != null) {
                    ClipMessage(Clip(envelope.id, envelope.text, envelope.sensitive ?: false))
                } else {
                    null
                }
                "ping" -> Ping
                "pong" -> Pong
                else -> null
            }
        }
    }
}

/** Content of the QR code shown by the Mac. */
data class PairingPayload(
    val publicKey: ByteArray,
    val token: ByteArray,
    val name: String,
    val hosts: List<String>,
    val port: Int,
) {
    val uri: String
        get() = "rohy://pair?v=${RohyProtocol.VERSION}&pk=${Base64Url.encode(publicKey)}&t=${Base64Url.encode(token)}" +
            "&n=${encode(name)}&h=${hosts.joinToString(",")}&p=$port"

    override fun equals(other: Any?) = other is PairingPayload && publicKey.contentEquals(other.publicKey) &&
        token.contentEquals(other.token) && name == other.name && hosts == other.hosts && port == other.port

    override fun hashCode() = publicKey.contentHashCode()

    companion object {
        private const val PREFIX = "rohy://pair?"

        fun parse(uri: String): PairingPayload? {
            if (!uri.startsWith(PREFIX)) return null
            val query = uri.removePrefix(PREFIX).split('&').mapNotNull { part ->
                val index = part.indexOf('=')
                if (index < 0) null else part.substring(0, index) to decode(part.substring(index + 1))
            }.toMap()
            if (query["v"] != RohyProtocol.VERSION.toString()) return null
            val publicKey = query["pk"]?.let(Base64Url::decode)?.takeIf { it.size == 32 } ?: return null
            val token = query["t"]?.let(Base64Url::decode)?.takeIf { it.size == RohyProtocol.TOKEN_LENGTH } ?: return null
            val name = query["n"]?.takeIf { it.isNotEmpty() } ?: return null
            val port = query["p"]?.toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
            val hosts = query["h"].orEmpty().split(',').filter { it.isNotEmpty() }
            return PairingPayload(publicKey, token, name, hosts, port)
        }

        // A literal "+" stays a "+" (RFC 3986 query), unlike HTML form encoding.
        private fun decode(value: String) = URLDecoder.decode(value.replace("+", "%2B"), Charsets.UTF_8)

        private fun encode(value: String) = URLEncoder.encode(value, Charsets.UTF_8).replace("+", "%20")
    }
}
