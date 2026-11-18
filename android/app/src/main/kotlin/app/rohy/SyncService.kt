package app.rohy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PersistableBundle
import android.util.Log
import app.rohy.core.Backoff
import app.rohy.core.ClientSession
import app.rohy.core.Clip
import app.rohy.core.CloseCode
import app.rohy.core.PairingPayload
import app.rohy.core.RohyClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Foreground service (type connectedDevice) that keeps the connection to the Mac,
 * writes received text to the clipboard and sends text handed over by the tile or share sheet.
 * Android 10+ forbids reading the clipboard here: reading happens in foreground activities only.
 */
class SyncService : Service() {
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val backoff = Backoff()
    private lateinit var storage: Storage
    private var client: RohyClient? = null
    private var retry: ScheduledFuture<*>? = null
    private var pairingToken: ByteArray? = null
    private val outbox = ArrayDeque<Clip>()
    private var generation = 0
    private lateinit var journal: ConnectionJournal
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return
            executor.execute {
                val wasUp = wifiUp
                wifiUp = true
                if (wasUp || !status.value.reconnectsOnNetworkReturn) return@execute
                log("Wi-Fi revenu : reconnexion immédiate")
                reconnectNow()
            }
        }

        override fun onLost(network: Network) = executor.execute {
            wifiUp = false
            log("Réseau perdu")
        }
    }
    private var wifiUp = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        storage = Storage(this)
        journal = ConnectionJournal.decode(storage.journalText)
        mutableJournal.value = journal.all()
        createChannel()
        getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(networkCallback)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, notification(status.value), ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        val action = intent?.action ?: ACTION_START
        val text = intent?.getStringExtra(EXTRA_TEXT)
        val uri = intent?.getStringExtra(EXTRA_URI)
        val reason = intent?.getStringExtra(EXTRA_REASON) ?: if (intent == null) "redémarrage par Android" else null
        executor.execute {
            reason?.let { log("Service démarré ($it)") }
            handle(action, text, uri)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(networkCallback) }
        executor.execute { disconnect() }
        executor.shutdown()
        super.onDestroy()
    }

    private fun handle(action: String, text: String?, uri: String?) {
        when (action) {
            ACTION_PAIR -> {
                val payload = uri?.let(PairingPayload::parse) ?: return
                storage.pairedMac = PairedMac.from(payload)
                pairingToken = payload.token
                reconnectNow()
            }
            ACTION_SEND -> {
                if (text.isNullOrEmpty()) return
                val clip = Clip(text = text)
                // The session queues the clip until it is authenticated.
                client?.send(clip) ?: run {
                    outbox.addLast(clip)
                    reconnectNow()
                }
            }
            ACTION_STOP -> {
                disconnect()
                publish(SyncStatus.Stopped)
                main.post { stopSelf() }
            }
            ACTION_FORGET -> {
                log("Mac oublié depuis l'app")
                disconnect()
                storage.pairedMac = null
                publish(SyncStatus.NotPaired)
                main.post { stopSelf() }
            }
            else -> if (client == null) reconnectNow()
        }
    }

    private fun reconnectNow() {
        disconnect()
        backoff.reset()
        connect()
    }

    private fun connect() {
        val mac = storage.pairedMac ?: return publish(SyncStatus.NotPaired)
        if (Build.VERSION.SDK_INT >= LOCAL_NETWORK_MIN_SDK &&
            checkSelfPermission(LOCAL_NETWORK_PERMISSION) != PackageManager.PERMISSION_GRANTED
        ) {
            return publish(SyncStatus.LocalNetworkDenied(mac.name))
        }
        publish(SyncStatus.Searching(mac.name))
        val discovered = MacLocator(this).find(mac.fingerprint)
        Log.i(TAG, "discovered=$discovered stored=${mac.hosts}")
        attempt(mac, connectionCandidates(discovered, mac), 0, ++generation)
    }

    private fun attempt(mac: PairedMac, candidates: List<String>, index: Int, gen: Int) {
        if (gen != generation) return
        if (index >= candidates.size) return scheduleRetry(mac)
        val session = ClientSession(storage.identity(), mac.publicKey, Build.MODEL, pairingToken)
        while (outbox.isNotEmpty()) session.send(outbox.removeFirst())
        val newClient = RohyClient(session, object : RohyClient.Listener {
            override fun onPaired(macName: String) {
                executor.execute { pairingToken = null }
            }

            override fun onReady() = executor.execute {
                if (gen != generation) return@execute
                backoff.reset()
                publish(SyncStatus.Connected(mac.name), detail = candidates[index])
            }

            override fun onClip(clip: Clip) {
                main.post { writeClipboard(clip) }
            }

            override fun onClosed(code: Int?, reason: String) = executor.execute {
                Log.w(TAG, "connection to ${candidates[index]}:${mac.port} closed: code=$code reason=$reason")
                if (gen != generation) return@execute
                val wasReady = status.value.isConnected
                if (wasReady) log("Connexion perdue (code=${code ?: "-"}, $reason)")
                outbox.addAll(client?.drainPending().orEmpty())
                client = null
                when {
                    code == CloseCode.FORBIDDEN.code -> publish(SyncStatus.Rejected(mac.name))
                    wasReady -> connect()
                    else -> attempt(mac, candidates, index + 1, gen)
                }
            }
        })
        client = newClient
        try {
            newClient.connect(candidates[index], mac.port)
        } catch (error: IllegalArgumentException) {
            Log.w(TAG, "invalid address ${candidates[index]}", error)
            client = null
            attempt(mac, candidates, index + 1, gen)
        }
    }

    private fun scheduleRetry(mac: PairedMac) {
        val delay = backoff.nextDelayMillis()
        publish(SyncStatus.Waiting(mac.name, delay / 1000))
        retry = executor.schedule({ connect() }, delay, TimeUnit.MILLISECONDS)
    }

    private fun disconnect() {
        generation += 1
        retry?.cancel(false)
        retry = null
        client?.let { outbox.addAll(it.drainPending()) }
        client?.close()
        client = null
    }

    private fun writeClipboard(clip: Clip) {
        val data = ClipData.newPlainText("Rohy", clip.text)
        if (clip.sensitive) {
            data.description.extras = PersistableBundle().apply {
                val key = if (Build.VERSION.SDK_INT >= 33) ClipDescription.EXTRA_IS_SENSITIVE else "android.content.extra.IS_SENSITIVE"
                putBoolean(key, true)
            }
        }
        getSystemService(ClipboardManager::class.java).setPrimaryClip(data)
    }

    /** Journals status changes only (not every retry countdown) to keep hours of history. */
    private fun publish(newStatus: SyncStatus, detail: String? = null) {
        val previous = status.value
        if (previous.journalGroup != newStatus.journalGroup) log(newStatus.label + (detail?.let { " ($it)" } ?: ""))
        mutableStatus.value = newStatus
        main.post {
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(newStatus))
        }
    }

    private fun log(message: String) {
        Log.i(TAG, message)
        journal.add(System.currentTimeMillis(), message)
        storage.journalText = journal.encode()
        mutableJournal.value = journal.all()
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Synchronisation", NotificationManager.IMPORTANCE_LOW)
        channel.description = "Connexion au Mac"
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun notification(status: SyncStatus): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val send = PendingIntent.getActivity(
            this, 1,
            Intent(this, SendClipboardActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val sendAction = Notification.Action.Builder(null, "Envoyer le presse-papiers", send).build()
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_link)
            .setContentTitle("Rohy")
            .setContentText(status.label)
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(sendAction)
            .build()
    }

    companion object {
        private const val TAG = "Rohy"
        private const val CHANNEL_ID = "sync"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_START = "app.rohy.START"
        private const val ACTION_STOP = "app.rohy.STOP"
        private const val ACTION_FORGET = "app.rohy.FORGET"
        private const val ACTION_SEND = "app.rohy.SEND"
        private const val ACTION_PAIR = "app.rohy.PAIR"
        private const val EXTRA_TEXT = "text"
        private const val EXTRA_URI = "uri"
        private const val EXTRA_REASON = "reason"

        private val mutableStatus = MutableStateFlow<SyncStatus>(SyncStatus.Stopped)
        val status: StateFlow<SyncStatus> = mutableStatus
        private val mutableJournal = MutableStateFlow<List<ConnectionEvent>>(emptyList())
        val journalEvents: StateFlow<List<ConnectionEvent>> = mutableJournal

        fun start(context: Context, reason: String = "app ouverte") =
            launch(context, ACTION_START) { putExtra(EXTRA_REASON, reason) }
        fun stop(context: Context) = launch(context, ACTION_STOP)
        fun forget(context: Context) = launch(context, ACTION_FORGET)
        fun send(context: Context, text: String) = launch(context, ACTION_SEND) { putExtra(EXTRA_TEXT, text) }
        fun pair(context: Context, uri: String) = launch(context, ACTION_PAIR) { putExtra(EXTRA_URI, uri) }

        private fun launch(context: Context, action: String, extras: Intent.() -> Unit = {}) {
            val intent = Intent(context, SyncService::class.java).setAction(action).apply(extras)
            context.startForegroundService(intent)
        }
    }
}
