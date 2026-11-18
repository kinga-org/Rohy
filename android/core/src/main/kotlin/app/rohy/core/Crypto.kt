package app.rohy.core

import org.bouncycastle.crypto.InvalidCipherTextException
import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.modes.ChaCha20Poly1305
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object RohyProtocol {
    const val VERSION = 1
    const val DEFAULT_PORT = 47823
    const val SERVICE_TYPE = "_rohy._tcp."
    const val PATH = "/rohy"
    const val NONCE_LENGTH = 16
    const val TOKEN_LENGTH = 32
    const val MAX_TEXT_BYTES = 1 shl 20
    internal val AAD = "rohy/v1".toByteArray()
    internal val INFO_C2S = "rohy/v1 c2s".toByteArray()
    internal val INFO_S2C = "rohy/v1 s2c".toByteArray()
}

enum class CloseCode(val code: Int) {
    INVALID_FRAME(4400),
    FORBIDDEN(4403),
    UNSUPPORTED_VERSION(4426),
}

/** Base64 URL encoding without padding (RFC 4648 §5). */
object Base64Url {
    fun encode(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    fun decode(text: String): ByteArray? =
        if (text.contains('=')) null else runCatching { Base64.getUrlDecoder().decode(text) }.getOrNull()
}

object Hex {
    fun encode(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    fun decode(text: String): ByteArray? {
        if (text.length % 2 != 0) return null
        return runCatching { ByteArray(text.length / 2) { text.substring(it * 2, it * 2 + 2).toInt(16).toByte() } }
            .getOrNull()
    }
}

object Fingerprint {
    fun of(publicKey: ByteArray): String =
        Hex.encode(MessageDigest.getInstance("SHA-256").digest(publicKey)).take(16)
}

object Random {
    private val secureRandom = SecureRandom()

    fun bytes(count: Int): ByteArray = ByteArray(count).also { secureRandom.nextBytes(it) }
}

/** X25519 identity of a device. The private key never leaves the device. */
class Identity(val privateKey: ByteArray) {
    init {
        require(privateKey.size == 32) { "X25519 private key must be 32 bytes" }
    }

    val publicKey: ByteArray = X25519PrivateKeyParameters(privateKey, 0).generatePublicKey().encoded

    fun sharedSecret(peerPublicKey: ByteArray): ByteArray {
        require(peerPublicKey.size == 32) { "X25519 public key must be 32 bytes" }
        val agreement = X25519Agreement()
        agreement.init(X25519PrivateKeyParameters(privateKey, 0))
        return ByteArray(agreement.agreementSize).also {
            agreement.calculateAgreement(X25519PublicKeyParameters(peerPublicKey, 0), it, 0)
        }
    }

    companion object {
        fun generate(): Identity = Identity(Random.bytes(32))
    }
}

/** Per-direction session keys (see docs/protocol.md). */
class SessionKeys(val clientToServer: ByteArray, val serverToClient: ByteArray) {
    companion object {
        fun derive(identity: Identity, peerPublicKey: ByteArray, clientNonce: ByteArray, serverNonce: ByteArray): SessionKeys {
            val shared = identity.sharedSecret(peerPublicKey)
            val salt = clientNonce + serverNonce
            return SessionKeys(hkdf(shared, salt, RohyProtocol.INFO_C2S), hkdf(shared, salt, RohyProtocol.INFO_S2C))
        }

        private fun hkdf(ikm: ByteArray, salt: ByteArray, info: ByteArray): ByteArray {
            val generator = HKDFBytesGenerator(SHA256Digest())
            generator.init(HKDFParameters(ikm, salt, info))
            return ByteArray(32).also { generator.generateBytes(it, 0, it.size) }
        }
    }
}

class FrameException(message: String) : Exception(message)

internal fun counterNonce(counter: Long): ByteArray =
    ByteArray(12).also { nonce -> for (i in 0 until 8) nonce[4 + i] = (counter ushr (56 - 8 * i)).toByte() }

private fun chacha(forEncryption: Boolean, key: ByteArray, nonce: ByteArray, input: ByteArray, offset: Int): ByteArray {
    val cipher = ChaCha20Poly1305()
    cipher.init(forEncryption, AEADParameters(KeyParameter(key), 128, nonce, RohyProtocol.AAD))
    val length = input.size - offset
    val output = ByteArray(cipher.getOutputSize(length))
    val written = cipher.processBytes(input, offset, length, output, 0)
    cipher.doFinal(output, written)
    return output
}

/** Seals outgoing frames: nonce (12 bytes, counter) ‖ ciphertext ‖ tag. */
class FrameSealer(private val key: ByteArray) {
    var counter = 0L
        private set

    fun seal(plaintext: ByteArray): ByteArray {
        val nonce = counterNonce(counter)
        val sealed = chacha(true, key, nonce, plaintext, 0)
        counter += 1
        return nonce + sealed
    }
}

/** Opens incoming frames, accepting only the next expected counter. */
class FrameOpener(private val key: ByteArray) {
    var expectedCounter = 0L
        private set

    fun open(frame: ByteArray): ByteArray {
        if (frame.size < 12 + 16) throw FrameException("frame too short")
        if (!frame.copyOfRange(0, 12).contentEquals(counterNonce(expectedCounter))) {
            throw FrameException("unexpected counter")
        }
        val plaintext = try {
            chacha(false, key, frame.copyOfRange(0, 12), frame, 12)
        } catch (error: InvalidCipherTextException) {
            throw FrameException("authentication failed")
        }
        expectedCounter += 1
        return plaintext
    }
}
