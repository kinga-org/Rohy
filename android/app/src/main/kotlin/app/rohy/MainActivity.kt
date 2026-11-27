package app.rohy

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationManager
import android.net.Uri
import android.os.PowerManager
import android.app.StatusBarManager
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import app.rohy.core.PairingPayload
import kotlinx.coroutines.delay

/** Screens reached from the home screen. */
private enum class Screen { HOME, RELIABILITY, JOURNAL, HANDLE_PERMISSION }

class MainActivity : ComponentActivity() {
    private lateinit var storage: Storage
    private var pairedMac by mutableStateOf<PairedMac?>(null)
    private var checks by mutableStateOf<List<ReliabilityCheck>>(emptyList())
    private var handleEnabled by mutableStateOf(false)
    /** Set while the user is in Android's "Afficher par-dessus les autres apps" screen for the edge handle. */
    private var handlePermissionAsked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storage = Storage(this)
        pairedMac = storage.pairedMac
        handleEnabled = storage.handleEnabled && Settings.canDrawOverlays(this)
        requestStartupPermissions()
        if (pairedMac != null) SyncService.start(this)
        enableEdgeToEdge()
        setContent {
            RohyTheme {
                val status by SyncService.status.collectAsState()
                val journal by SyncService.journalEvents.collectAsState()
                val activity by SyncService.lastActivity.collectAsState()
                var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(15_000)
                        now = System.currentTimeMillis()
                    }
                }
                var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
                BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }
                val mac = pairedMac
                when {
                    mac == null -> OnboardingScreen(onScan = ::scanQrCode, onPasteCode = ::pairFromClipboard)
                    screen == Screen.RELIABILITY -> ReliabilityScreen(checks, ::fix) { screen = Screen.HOME }
                    screen == Screen.HANDLE_PERMISSION -> {
                        // Back on the home screen as soon as the permission came back granted.
                        LaunchedEffect(handleEnabled) { if (handleEnabled) screen = Screen.HOME }
                        HandlePermissionScreen(onAllow = ::askHandlePermission, onLater = { screen = Screen.HOME })
                    }
                    screen == Screen.JOURNAL ->
                        JournalScreen(journalDays(journal, System.currentTimeMillis())) { screen = Screen.HOME }
                    else -> HomeScreen(
                        content = HomeContent(
                            computer = mac.displayName,
                            pairedDetail = pairedDetail(storage.pairedAtMillis, mac.hosts),
                            header = homeHeader(status, mac.displayName),
                            activity = activity?.describe(now),
                            reliability = summarize(checks),
                        ),
                        onSend = { sendToMac(readClipboard()) },
                        onReconnect = { SyncService.start(this, "reconnexion depuis l'app") },
                        onAllowLocalNetwork = { fix(CheckId.LOCAL_NETWORK) },
                        onAddTile = ::requestTile,
                        handleEnabled = handleEnabled,
                        onToggleHandle = { on ->
                            if (on && !Settings.canDrawOverlays(this)) screen = Screen.HANDLE_PERMISSION else setHandle(on)
                        },
                        onOpenReliability = { screen = Screen.RELIABILITY },
                        onOpenJournal = { screen = Screen.JOURNAL },
                        onForget = ::forgetMac,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        pairedMac = storage.pairedMac
        checks = reliabilityChecks(readFacts())
        if (handlePermissionAsked && Settings.canDrawOverlays(this)) {
            handlePermissionAsked = false
            setHandle(true)
        }
        // The permission can be withdrawn in Android's settings: the switch follows.
        if (handleEnabled && !Settings.canDrawOverlays(this)) handleEnabled = false
    }

    private fun askHandlePermission() {
        handlePermissionAsked = true
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        runCatching { startActivity(intent) }.onFailure {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }

    private fun setHandle(on: Boolean) {
        storage.handleEnabled = on
        if (on) storage.handleHidden = false
        handleEnabled = on
        SyncService.refreshHandle(this)
    }

    private fun scanQrCode() {
        val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        GmsBarcodeScanning.getClient(this, options).startScan()
            .addOnSuccessListener { pair(it.rawValue) }
            .addOnFailureListener { toast("Scan impossible : ${it.message}") }
    }

    private fun pairFromClipboard() = pair(readClipboard())

    private fun pair(code: String?) {
        val payload = code?.trim()?.let(PairingPayload::parse) ?: return toast("Code d'association invalide")
        // Saved here, synchronously: onResume (after the scanner closes) must already see it.
        storage.pairedMac = PairedMac.from(payload)
        pairedMac = storage.pairedMac
        SyncService.pair(this, payload.uri)
    }

    private fun forgetMac() {
        SyncService.forget(this)
        pairedMac = null
    }

    private fun readClipboard(): String? =
        getSystemService(ClipboardManager::class.java).primaryClip
            ?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()

    private fun requestTile() {
        if (Build.VERSION.SDK_INT < 33) return toast("Ajoute la tuile « Envoyer à l'ordinateur » depuis l'édition des réglages rapides")
        getSystemService(StatusBarManager::class.java).requestAddTileService(
            ComponentName(this, ClipTileService::class.java), getString(R.string.tile_label),
            Icon.createWithResource(this, R.drawable.ic_rohy), mainExecutor,
        ) {}
    }

    private fun requestStartupPermissions() {
        val missing = buildList {
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
            if (Build.VERSION.SDK_INT >= LOCAL_NETWORK_MIN_SDK) add(LOCAL_NETWORK_PERMISSION)
        }.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), REQUEST_STARTUP)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        checks = reliabilityChecks(readFacts())
        val index = permissions.indexOf(LOCAL_NETWORK_PERMISSION)
        if (index < 0) return
        if (grantResults.getOrNull(index) == PackageManager.PERMISSION_GRANTED) {
            if (storage.pairedMac != null) SyncService.start(this, "accès au réseau local autorisé")
        } else if (requestCode == REQUEST_LOCAL_NETWORK && !shouldShowRequestPermissionRationale(LOCAL_NETWORK_PERMISSION)) {
            // Denied for good: only the app settings can grant it now.
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }

    private fun readFacts() = DeviceFacts(
        manufacturer = Build.MANUFACTURER,
        localNetworkGranted = if (Build.VERSION.SDK_INT >= LOCAL_NETWORK_MIN_SDK) {
            checkSelfPermission(LOCAL_NETWORK_PERMISSION) == PackageManager.PERMISSION_GRANTED
        } else {
            null
        },
        notificationsEnabled = getSystemService(NotificationManager::class.java).areNotificationsEnabled(),
        ignoringBatteryOptimizations = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName),
        backgroundRestricted = getSystemService(ActivityManager::class.java).isBackgroundRestricted,
    )

    private fun fix(id: CheckId) {
        val packageUri = Uri.parse("package:$packageName")
        val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
        val candidates = when (id) {
            CheckId.LOCAL_NETWORK -> return requestPermissions(arrayOf(LOCAL_NETWORK_PERMISSION), REQUEST_LOCAL_NETWORK)
            CheckId.NOTIFICATIONS -> listOf(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
            )
            CheckId.BATTERY_OPTIMIZATION -> listOf(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri))
            CheckId.BACKGROUND_RESTRICTION -> emptyList()
            CheckId.SAMSUNG_NEVER_SLEEPING -> SAMSUNG_BATTERY_SCREENS.map { Intent().setComponent(it) }
            // Huawei protects this screen on some versions: the app's info page is opened instead.
            CheckId.HUAWEI_APP_LAUNCH -> HUAWEI_APP_LAUNCH_SCREENS.map { Intent().setComponent(it) }
            CheckId.RECENTS_LOCK -> return toast(checks.first { it.id == id }.explanation)
        }
        // Each screen is tried in turn: since Android 11, resolveActivity cannot see other apps' screens
        // without declaring them, and a protected screen only fails when started.
        val opened = candidates.any { runCatching { startActivity(it) }.isSuccess }
        if (!opened) startActivity(appDetails)
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

private const val REQUEST_STARTUP = 1
private const val REQUEST_LOCAL_NETWORK = 2

private val HUAWEI_APP_LAUNCH_SCREENS = listOf(
    ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
    ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"),
)

private val SAMSUNG_BATTERY_SCREENS = listOf(
    ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"),
    ComponentName("com.samsung.android.sm", "com.samsung.android.sm.battery.ui.BatteryActivity"),
)
