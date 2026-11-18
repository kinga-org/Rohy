package app.rohy.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Minimal Mac-side implementation used to drive the client session. */
private class TestMac(val identity: Identity = Identity.generate()) {
    val nonce = Random.bytes(RohyProtocol.NONCE_LENGTH)
    lateinit var sealer: FrameSealer
    lateinit var opener: FrameOpener

    fun welcome(helloText: String, status: String, version: Int = 1): String {
        val hello = json.decodeFromString(Hello.serializer(), helloText)
        val keys = SessionKeys.derive(identity, Base64Url.decode(hello.pk)!!, Base64Url.decode(hello.n)!!, nonce)
        sealer = FrameSealer(keys.serverToClient)
        opener = FrameOpener(keys.clientToServer)
        return json.encodeToString(
            Welcome.serializer(),
            Welcome(v = version, pk = Base64Url.encode(identity.publicKey), n = Base64Url.encode(nonce), status = status),
        )
    }

    fun seal(message: SecureMessage) = sealer.seal(message.encode())

    fun open(action: ClientAction) = SecureMessage.decode(opener.open((action as ClientAction.SendBinary).bytes))
}

class ClientSessionTest {
    private val phone = Identity.generate()
    private val mac = TestMac()
    private val token = ByteArray(32) { 42 }

    private fun session(token: ByteArray? = null, macKey: ByteArray = mac.identity.publicKey) =
        ClientSession(phone, macKey, "Galaxy", token)

    private fun helloText(session: ClientSession) = (session.hello().single() as ClientAction.SendText).text

    @Test
    fun pairsThenFlushesQueuedClipsAndExchanges() {
        val session = session(token)
        assertTrue(session.send(Clip("q", "en attente")).isEmpty())
        val welcome = mac.welcome(helloText(session), Welcome.PAIRING)
        assertTrue(session.hello().isEmpty())

        val pairActions = session.receiveText(welcome)
        assertEquals(SecureMessage.Pair(token), mac.open(pairActions.single()))
        assertFalse(session.isReady)

        val paired = session.receiveBinary(mac.seal(SecureMessage.Paired("MacBook")))
        assertEquals(ClientAction.Paired("MacBook"), paired[0])
        assertEquals(ClientAction.Ready, paired[1])
        assertEquals(SecureMessage.ClipMessage(Clip("q", "en attente")), mac.open(paired[2]))
        assertTrue(session.isReady)

        val clip = Clip("m", "depuis le Mac", sensitive = true)
        assertEquals(listOf(ClientAction.Received(clip)), session.receiveBinary(mac.seal(SecureMessage.ClipMessage(clip))))
        assertEquals(SecureMessage.Pong, mac.open(session.receiveBinary(mac.seal(SecureMessage.Ping)).single()))
        assertTrue(session.receiveBinary(mac.seal(SecureMessage.Pong)).isEmpty())
        assertEquals(SecureMessage.ClipMessage(Clip("p", "x")), mac.open(session.send(Clip("p", "x")).single()))
    }

    @Test
    fun trustedDeviceIsReadyImmediately() {
        val session = session()
        session.send(Clip("q", "queued"))
        val actions = session.receiveText(mac.welcome(helloText(session), Welcome.TRUSTED))
        assertEquals(ClientAction.Ready, actions[0])
        assertEquals(SecureMessage.ClipMessage(Clip("q", "queued")), mac.open(actions[1]))
        assertTrue(session.drainPending().isEmpty())
    }

    @Test
    fun rejectsSubstitutedMacKey() {
        val session = session(token, macKey = Identity.generate().publicKey)
        val actions = session.receiveText(mac.welcome(helloText(session), Welcome.PAIRING))
        assertEquals(listOf(ClientAction.Close(CloseCode.FORBIDDEN)), actions)
        assertTrue(session.isClosed)
        assertTrue(session.send(Clip("x", "x")).isEmpty())
    }

    @Test
    fun pairingWithoutTokenIsForbidden() {
        val session = session()
        assertEquals(
            listOf(ClientAction.Close(CloseCode.FORBIDDEN)),
            session.receiveText(mac.welcome(helloText(session), Welcome.PAIRING)),
        )
    }

    @Test
    fun rejectsInvalidWelcomes() {
        val cases = listOf(
            "not json" to CloseCode.INVALID_FRAME,
            """{"t":"hello","v":1,"pk":"","n":"","name":""}""" to CloseCode.INVALID_FRAME,
        )
        for ((text, code) in cases) {
            val session = session()
            session.hello()
            assertEquals(listOf(ClientAction.Close(code)), session.receiveText(text))
        }
        val versioned = session()
        assertEquals(
            listOf(ClientAction.Close(CloseCode.UNSUPPORTED_VERSION)),
            versioned.receiveText(mac.welcome(helloText(versioned), Welcome.TRUSTED, version = 2)),
        )
        val badNonce = session()
        helloText(badNonce)
        val text = """{"t":"welcome","v":1,"pk":"${Base64Url.encode(mac.identity.publicKey)}","n":"AAAA","status":"trusted"}"""
        assertEquals(listOf(ClientAction.Close(CloseCode.INVALID_FRAME)), badNonce.receiveText(text))
        val badStatus = session()
        assertEquals(
            listOf(ClientAction.Close(CloseCode.INVALID_FRAME)),
            badStatus.receiveText(mac.welcome(helloText(badStatus), "other")),
        )
    }

    @Test
    fun rejectsFramesInWrongStateOrOrder() {
        val fresh = session()
        assertEquals(listOf(ClientAction.Close(CloseCode.INVALID_FRAME)), fresh.receiveText("{}"))
        assertEquals(listOf(ClientAction.Close(CloseCode.INVALID_FRAME)), fresh.receiveBinary(ByteArray(40)))

        val pairing = session(token)
        pairing.receiveText(mac.welcome(helloText(pairing), Welcome.PAIRING))
        assertEquals(
            listOf(ClientAction.Close(CloseCode.INVALID_FRAME)),
            pairing.receiveBinary(mac.seal(SecureMessage.ClipMessage(Clip("x", "x")))),
        )

        val ready = session()
        ready.receiveText(mac.welcome(helloText(ready), Welcome.TRUSTED))
        val frame = mac.seal(SecureMessage.Ping)
        ready.receiveBinary(frame)
        assertEquals(listOf(ClientAction.Close(CloseCode.INVALID_FRAME)), ready.receiveBinary(frame))
    }

    @Test
    fun ignoresEmptyOrOversizedClipsAndDrainsQueue() {
        val session = session()
        assertTrue(session.send(Clip("e", "")).isEmpty())
        assertTrue(session.send(Clip("b", "a".repeat(RohyProtocol.MAX_TEXT_BYTES + 1))).isEmpty())
        session.send(Clip("k", "kept"))
        assertEquals(listOf(Clip("k", "kept")), session.drainPending())
        assertTrue(session.drainPending().isEmpty())
    }
}
