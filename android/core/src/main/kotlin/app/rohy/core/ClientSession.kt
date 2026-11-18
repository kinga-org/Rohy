package app.rohy.core

/** What the network layer must do after the session processed an input. */
sealed class ClientAction {
    data class SendText(val text: String) : ClientAction()
    class SendBinary(val bytes: ByteArray) : ClientAction()
    data class Close(val code: CloseCode) : ClientAction()
    data class Paired(val macName: String) : ClientAction()
    data object Ready : ClientAction()
    data class Received(val clip: Clip) : ClientAction()
}

/**
 * Phone side of one connection, without networking (see docs/protocol.md).
 * Clips sent before the session is ready are queued and flushed once it is.
 * Not thread-safe: callers serialize access.
 */
class ClientSession(
    private val identity: Identity,
    private val macPublicKey: ByteArray,
    private val deviceName: String,
    private val pairingToken: ByteArray? = null,
    private val clientNonce: ByteArray = Random.bytes(RohyProtocol.NONCE_LENGTH),
) {
    private class Channel(val sealer: FrameSealer, val opener: FrameOpener)

    private sealed class State {
        data object New : State()
        data object AwaitingWelcome : State()
        class AwaitingPaired(val channel: Channel) : State()
        class Ready(val channel: Channel) : State()
        data object Closed : State()
    }

    private var state: State = State.New
    private val pending = ArrayDeque<Clip>()

    val isReady: Boolean get() = state is State.Ready
    val isClosed: Boolean get() = state is State.Closed

    fun hello(): List<ClientAction> {
        if (state != State.New) return emptyList()
        state = State.AwaitingWelcome
        val hello = Hello(pk = Base64Url.encode(identity.publicKey), n = Base64Url.encode(clientNonce), name = deviceName)
        return listOf(ClientAction.SendText(json.encodeToString(Hello.serializer(), hello)))
    }

    fun receiveText(text: String): List<ClientAction> {
        if (state != State.AwaitingWelcome) return fail(CloseCode.INVALID_FRAME)
        val welcome = runCatching { json.decodeFromString(Welcome.serializer(), text) }.getOrNull()
            ?.takeIf { it.t == "welcome" } ?: return fail(CloseCode.INVALID_FRAME)
        if (welcome.v != RohyProtocol.VERSION) return fail(CloseCode.UNSUPPORTED_VERSION)
        val serverKey = Base64Url.decode(welcome.pk)
        if (serverKey == null || !serverKey.contentEquals(macPublicKey)) return fail(CloseCode.FORBIDDEN)
        val serverNonce = Base64Url.decode(welcome.n)?.takeIf { it.size == RohyProtocol.NONCE_LENGTH }
            ?: return fail(CloseCode.INVALID_FRAME)

        val keys = SessionKeys.derive(identity, macPublicKey, clientNonce, serverNonce)
        val channel = Channel(FrameSealer(keys.clientToServer), FrameOpener(keys.serverToClient))
        return when (welcome.status) {
            Welcome.TRUSTED -> {
                state = State.Ready(channel)
                listOf(ClientAction.Ready) + flush(channel)
            }
            Welcome.PAIRING -> {
                val token = pairingToken ?: return fail(CloseCode.FORBIDDEN)
                state = State.AwaitingPaired(channel)
                listOf(seal(channel, SecureMessage.Pair(token)))
            }
            else -> fail(CloseCode.INVALID_FRAME)
        }
    }

    fun receiveBinary(frame: ByteArray): List<ClientAction> {
        val current = state
        val channel = when (current) {
            is State.AwaitingPaired -> current.channel
            is State.Ready -> current.channel
            else -> return fail(CloseCode.INVALID_FRAME)
        }
        val message = runCatching { SecureMessage.decode(channel.opener.open(frame)) }.getOrNull()
            ?: return fail(CloseCode.INVALID_FRAME)
        return when {
            current is State.AwaitingPaired && message is SecureMessage.Paired -> {
                state = State.Ready(channel)
                listOf(ClientAction.Paired(message.name), ClientAction.Ready) + flush(channel)
            }
            current is State.Ready && message is SecureMessage.ClipMessage -> listOf(ClientAction.Received(message.clip))
            current is State.Ready && message is SecureMessage.Ping -> listOf(seal(channel, SecureMessage.Pong))
            current is State.Ready && message is SecureMessage.Pong -> emptyList()
            else -> fail(CloseCode.INVALID_FRAME)
        }
    }

    /** Sends a clip now when ready, otherwise queues it (dropped if the session closes). */
    fun send(clip: Clip): List<ClientAction> {
        if (clip.text.isEmpty() || clip.text.toByteArray().size > RohyProtocol.MAX_TEXT_BYTES) return emptyList()
        return when (val current = state) {
            is State.Ready -> listOf(seal(current.channel, SecureMessage.ClipMessage(clip)))
            is State.Closed -> emptyList()
            else -> {
                pending.addLast(clip)
                emptyList()
            }
        }
    }

    /** Clips still waiting for an authenticated session. */
    fun drainPending(): List<Clip> = pending.toList().also { pending.clear() }

    private fun flush(channel: Channel): List<ClientAction> {
        val actions = pending.map { seal(channel, SecureMessage.ClipMessage(it)) }
        pending.clear()
        return actions
    }

    private fun seal(channel: Channel, message: SecureMessage) = ClientAction.SendBinary(channel.sealer.seal(message.encode()))

    private fun fail(code: CloseCode): List<ClientAction> {
        state = State.Closed
        return listOf(ClientAction.Close(code))
    }
}

/** Exponential reconnection delay, capped. */
class Backoff(private val baseMillis: Long = 1_000, private val maxMillis: Long = 30_000) {
    private var attempt = 0

    fun nextDelayMillis(): Long {
        val delay = (baseMillis shl attempt.coerceAtMost(20)).coerceAtMost(maxMillis)
        attempt += 1
        return delay
    }

    fun reset() {
        attempt = 0
    }
}
