package app.rohy.core

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.util.concurrent.TimeUnit

/**
 * One WebSocket connection to the Mac, driven by a [ClientSession].
 * Listener callbacks run on OkHttp threads.
 */
class RohyClient(
    private val session: ClientSession,
    private val listener: Listener,
    private val http: OkHttpClient = defaultHttpClient,
) {
    interface Listener {
        fun onPaired(macName: String) {}
        fun onReady() {}
        fun onClip(clip: Clip) {}
        fun onClosed(code: Int?, reason: String) {}
    }

    private val lock = Any()
    private var socket: WebSocket? = null
    private var closedReported = false

    fun connect(host: String, port: Int) {
        val request = Request.Builder().url("ws://$host:$port${RohyProtocol.PATH}").build()
        socket = http.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) = apply(webSocket) { session.hello() }

            override fun onMessage(webSocket: WebSocket, text: String) = apply(webSocket) { session.receiveText(text) }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) =
                apply(webSocket) { session.receiveBinary(bytes.toByteArray()) }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                reportClosed(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = reportClosed(code, reason)

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) =
                reportClosed(null, t.message ?: t.javaClass.simpleName)
        })
    }

    fun send(clip: Clip) {
        socket?.let { apply(it) { session.send(clip) } } ?: synchronized(lock) { session.send(clip) }
    }

    /** Clips that could not be sent before the connection ended. */
    fun drainPending(): List<Clip> = synchronized(lock) { session.drainPending() }

    fun close() {
        socket?.close(1000, null)
    }

    private fun apply(webSocket: WebSocket, step: () -> List<ClientAction>) {
        val actions = synchronized(lock) { step() }
        for (action in actions) {
            when (action) {
                is ClientAction.SendText -> webSocket.send(action.text)
                is ClientAction.SendBinary -> webSocket.send(action.bytes.toByteString())
                is ClientAction.Close -> webSocket.close(action.code.code, null)
                is ClientAction.Paired -> listener.onPaired(action.macName)
                ClientAction.Ready -> listener.onReady()
                is ClientAction.Received -> listener.onClip(action.clip)
            }
        }
    }

    private fun reportClosed(code: Int?, reason: String) {
        val first = synchronized(lock) { (!closedReported).also { closedReported = true } }
        if (first) listener.onClosed(code, reason)
    }

    companion object {
        val defaultHttpClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .build()
    }
}
