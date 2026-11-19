package app.rohy.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VectorTest {
    private val vectors = Json.parseToJsonElement(File(System.getProperty("rohy.vectors")).readText()).jsonObject

    private fun hex(name: String) = Hex.decode(vectors.getValue(name).jsonPrimitive.content)!!

    @Test
    fun keysAndFingerprintMatchSwiftVectors() {
        val client = Identity(hex("client_private"))
        val server = Identity(hex("server_private"))
        assertArrayEquals(hex("client_public"), client.publicKey)
        assertArrayEquals(hex("server_public"), server.publicKey)
        assertArrayEquals(hex("shared_secret"), client.sharedSecret(server.publicKey))
        assertEquals(vectors.getValue("server_fingerprint").jsonPrimitive.content, Fingerprint.of(server.publicKey))

        val keys = SessionKeys.derive(client, server.publicKey, hex("client_nonce"), hex("server_nonce"))
        assertArrayEquals(hex("key_c2s"), keys.clientToServer)
        assertArrayEquals(hex("key_s2c"), keys.serverToClient)
    }

    @Test
    fun framesMatchSwiftVectors() {
        val sealers = mapOf("c2s" to FrameSealer(hex("key_c2s")), "s2c" to FrameSealer(hex("key_s2c")))
        val openers = mapOf("c2s" to FrameOpener(hex("key_c2s")), "s2c" to FrameOpener(hex("key_s2c")))
        for (element in vectors.getValue("frames").jsonArray) {
            val frame = element.jsonObject
            val direction = frame.getValue("direction").jsonPrimitive.content
            val plaintext = frame.getValue("plaintext").jsonPrimitive.content
            val expected = Hex.decode(frame.getValue("frame").jsonPrimitive.content)!!
            assertEquals(frame.getValue("counter").jsonPrimitive.long, sealers.getValue(direction).counter)
            assertArrayEquals(expected, sealers.getValue(direction).seal(plaintext.toByteArray()))
            val opened = openers.getValue(direction).open(expected)
            assertEquals(plaintext, opened.decodeToString())
            assertTrue(SecureMessage.decode(opened) != null)
        }
    }
}

class EncodingTest {
    @Test
    fun base64UrlRoundTripsWithoutPadding() {
        for (length in 0..40) {
            val bytes = ByteArray(length) { ((it * 37 + 250) % 256).toByte() }
            val encoded = Base64Url.encode(bytes)
            assertFalse(encoded.contains('=') || encoded.contains('+') || encoded.contains('/'))
            assertArrayEquals(bytes, Base64Url.decode(encoded))
        }
        assertNull(Base64Url.decode("ab=="))
        assertNull(Base64Url.decode("a\$bc"))
    }

    @Test
    fun hexRejectsInvalidInput() {
        assertNull(Hex.decode("abc"))
        assertNull(Hex.decode("zz"))
        assertArrayEquals(byteArrayOf(0, -1), Hex.decode("00ff"))
        assertEquals(16, Random.bytes(16).size)
    }

    @Test
    fun identityRejectsWrongKeySizes() {
        assertThrows(IllegalArgumentException::class.java) { Identity(ByteArray(31)) }
        assertThrows(IllegalArgumentException::class.java) { Identity.generate().sharedSecret(ByteArray(31)) }
        assertNotEquals(Identity.generate().publicKey.toList(), Identity.generate().publicKey.toList())
    }
}

class FrameTest {
    private val key = Random.bytes(32)

    @Test
    fun rejectsReplayAndReordering() {
        val sealer = FrameSealer(key)
        val opener = FrameOpener(key)
        val first = sealer.seal("a".toByteArray())
        val second = sealer.seal("b".toByteArray())
        assertThrows(FrameException::class.java) { opener.open(second) }
        assertEquals("a", opener.open(first).decodeToString())
        assertThrows(FrameException::class.java) { opener.open(first) }
        assertEquals("b", opener.open(second).decodeToString())
        assertEquals(2L, opener.expectedCounter)
    }

    @Test
    fun rejectsTamperedAndShortFrames() {
        val opener = FrameOpener(key)
        val frame = FrameSealer(key).seal("secret".toByteArray())
        frame[frame.size - 1] = (frame[frame.size - 1].toInt() xor 1).toByte()
        assertThrows(FrameException::class.java) { opener.open(frame) }
        assertThrows(FrameException::class.java) { opener.open(ByteArray(10)) }
        assertEquals(0L, opener.expectedCounter)
    }

    @Test
    fun counterNonceIsBigEndian() {
        assertEquals("000000000000000000000102", Hex.encode(counterNonce(258)))
    }
}

class MessageTest {
    @Test
    fun secureMessagesRoundTrip() {
        val messages = listOf(
            SecureMessage.Pair(ByteArray(32) { 7 }),
            SecureMessage.Paired("Mac"),
            SecureMessage.ClipMessage(Clip("1", "héllo / \"quoted\"", true)),
            SecureMessage.Ping,
            SecureMessage.Pong,
        )
        for (message in messages) assertEquals(message, SecureMessage.decode(message.encode()))
        assertEquals(SecureMessage.Pair(ByteArray(1)).hashCode(), SecureMessage.Pair(ByteArray(1)).hashCode())
        assertNotEquals(SecureMessage.Pair(ByteArray(1)), SecureMessage.Ping)
    }

    @Test
    fun rejectsMalformedMessages() {
        val invalid = listOf(
            "nope", """{"t":"unknown"}""", """{"t":"pair"}""", """{"t":"pair","token":"*"}""",
            """{"t":"paired"}""", """{"t":"clip","id":"1"}""", """{"t":"clip","text":"x"}""",
        )
        for (text in invalid) assertNull(text, SecureMessage.decode(text.toByteArray()))
        assertEquals(
            SecureMessage.ClipMessage(Clip("1", "x", false)),
            SecureMessage.decode("""{"t":"clip","id":"1","text":"x","extra":1}""".toByteArray()),
        )
    }
}

class PairingPayloadTest {
    private val pk = Base64Url.encode(ByteArray(32) { 1 })
    private val token = Base64Url.encode(ByteArray(32) { 2 })

    @Test
    fun parsesUriProducedByTheMac() {
        // Same encoding as Swift URLComponents: spaces as %20, "&" escaped, "+" kept literally.
        val uri = "rohy://pair?v=1&pk=$pk&t=$token&n=Mac%20de%20test%20%26%20co%20+%20plus&h=192.168.1.10,10.0.0.2&p=47823"
        val payload = PairingPayload.parse(uri)!!
        assertEquals("Mac de test & co + plus", payload.name)
        assertEquals(listOf("192.168.1.10", "10.0.0.2"), payload.hosts)
        assertEquals(47823, payload.port)
        assertEquals(payload, PairingPayload.parse(payload.uri))
        assertEquals(payload.hashCode(), PairingPayload.parse(payload.uri).hashCode())
    }

    @Test
    fun keepsNonAsciiComputerNames() {
        // A real Mac name: typographic apostrophe (3 UTF-8 bytes) and accents, as URLComponents encodes them.
        val uri = "rohy://pair?v=1&pk=$pk&t=$token&n=RAHERINOTOAVINA%E2%80%99s%20MacBook%20Pro%20%C3%A9t%C3%A9&h=192.168.88.3&p=47823"
        val payload = PairingPayload.parse(uri)!!
        assertEquals("RAHERINOTOAVINA’s MacBook Pro été", payload.name)
        assertEquals(uri, payload.uri)
        assertEquals(payload, PairingPayload.parse(payload.uri))
    }

    @Test
    fun rejectsInvalidUris() {
        val invalid = listOf(
            "https://pair?v=1", "rohy://pair?v=2&pk=$pk&t=$token&n=M&p=1",
            "rohy://pair?v=1&pk=AAAA&t=$token&n=M&p=1", "rohy://pair?v=1&pk=$pk&t=AAAA&n=M&p=1",
            "rohy://pair?v=1&pk=$pk&t=$token&n=&p=1", "rohy://pair?v=1&pk=$pk&t=$token&n=M&p=99999",
            "rohy://pair?v=1&pk=$pk&t=$token&n=M", "rohy://pair?v&x",
        )
        for (uri in invalid) assertNull(uri, PairingPayload.parse(uri))
        assertEquals(emptyList<String>(), PairingPayload.parse("rohy://pair?v=1&pk=$pk&t=$token&n=M&p=1")!!.hosts)
        assertNotEquals(PairingPayload.parse("rohy://pair?v=1&pk=$pk&t=$token&n=M&p=1"), "other")
    }
}

class BackoffTest {
    @Test
    fun growsExponentiallyUpToTheCapAndResets() {
        val backoff = Backoff(baseMillis = 1_000, maxMillis = 5_000)
        assertEquals(listOf(1_000L, 2_000L, 4_000L, 5_000L, 5_000L), List(5) { backoff.nextDelayMillis() })
        backoff.reset()
        assertEquals(1_000L, backoff.nextDelayMillis())
        val long = Backoff()
        repeat(40) { long.nextDelayMillis() }
        assertEquals(30_000L, long.nextDelayMillis())
    }
}
