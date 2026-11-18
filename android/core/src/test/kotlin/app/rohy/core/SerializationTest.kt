package app.rohy.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SerializationTest {
    @Test
    fun handshakeMessagesUseDefaultsWhenFieldsAreOmitted() {
        val hello = json.decodeFromString(Hello.serializer(), """{"pk":"a","n":"b","name":"c"}""")
        assertEquals(Hello("hello", 1, "a", "b", "c"), hello)
        val full = json.decodeFromString(Hello.serializer(), """{"t":"x","v":2,"pk":"a","n":"b","name":"c"}""")
        assertEquals(Hello("x", 2, "a", "b", "c"), full)
        assertEquals(full, json.decodeFromString(Hello.serializer(), json.encodeToString(Hello.serializer(), full)))

        val welcome = json.decodeFromString(Welcome.serializer(), """{"pk":"a","n":"b","status":"trusted"}""")
        assertEquals(Welcome("welcome", 1, "a", "b", Welcome.TRUSTED), welcome)
        val fullWelcome = json.decodeFromString(Welcome.serializer(), """{"t":"w","v":3,"pk":"a","n":"b","status":"x"}""")
        assertEquals(Welcome("w", 3, "a", "b", "x"), fullWelcome)
    }

    @Test
    fun envelopeAcceptsEveryOptionalFieldCombination() {
        val token = Base64Url.encode(ByteArray(32))
        val all = """{"t":"clip","token":"$token","name":"n","id":"i","text":"x","sensitive":true}"""
        assertEquals(SecureMessage.ClipMessage(Clip("i", "x", true)), SecureMessage.decode(all.toByteArray()))
        assertEquals(SecureMessage.Paired("n"), SecureMessage.decode(all.replace("\"clip\"", "\"paired\"").toByteArray()))
        assertEquals(SecureMessage.Pair(ByteArray(32)), SecureMessage.decode(all.replace("\"clip\"", "\"pair\"").toByteArray()))
        assertEquals(SecureMessage.Ping, SecureMessage.decode("""{"t":"ping"}""".toByteArray()))
        assertNull(SecureMessage.decode("""{"name":"n"}""".toByteArray()))
    }

    @Test
    fun pairingPayloadEqualityComparesEveryField() {
        val base = PairingPayload(ByteArray(32) { 1 }, ByteArray(32) { 2 }, "Mac", listOf("h"), 1)
        assertEquals(base, base.copy())
        assertNotEquals(base, base.copy(publicKey = ByteArray(32)))
        assertNotEquals(base, base.copy(token = ByteArray(32)))
        assertNotEquals(base, base.copy(name = "Other"))
        assertNotEquals(base, base.copy(hosts = emptyList()))
        assertNotEquals(base, base.copy(port = 2))
        assertNotEquals(base, Any())
        assertNull(PairingPayload.parse("rohy://pair?v=1&pk=*&t=*&n=M&p=1"))
        assertNull(PairingPayload.parse("rohy://pair?v=1&pk=${Base64Url.encode(ByteArray(32))}&t=*&n=M&p=1"))
        val pk = Base64Url.encode(ByteArray(32))
        assertNull(PairingPayload.parse("rohy://pair?v=1&pk=$pk&t=$pk&p=1"))
        assertNull(PairingPayload.parse("rohy://pair?v=1&pk=$pk&t=$pk&n=M&p=0"))
        assertNull(PairingPayload.parse("rohy://pair?v=1&pk=$pk&t=$pk&n=M&p=x"))
    }
}
