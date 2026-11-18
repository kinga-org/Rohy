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
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import app.rohy.core.PairingPayload

class MainActivity : ComponentActivity() {
    private lateinit var storage: Storage
    private var pairedMac by mutableStateOf<PairedMac?>(null)
    private var checks by mutableStateOf<List<ReliabilityCheck>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storage = Storage(this)
        pairedMac = storage.pairedMac
        requestStartupPermissions()
        if (pairedMac != null) SyncService.start(this)
        setContent {
            val context = LocalContext.current
            val scheme = if (Build.VERSION.SDK_INT >= 31) dynamicLightColorScheme(context) else lightColorScheme()
            MaterialTheme(colorScheme = scheme) {
                val status by SyncService.status.collectAsState()
                val journal by SyncService.journalEvents.collectAsState()
                HomeScreen(
                    mac = pairedMac,
                    status = status,
                    onScan = ::scanQrCode,
                    onPasteCode = ::pairFromClipboard,
                    onSendClipboard = { sendToMac(readClipboard()) },
                    onReconnect = { SyncService.start(this) },
                    onForget = ::forgetMac,
                    onAddTile = ::requestTile,
                    checks = checks,
                    onFixCheck = ::fix,
                    journal = journal,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        pairedMac = storage.pairedMac
        checks = reliabilityChecks(readFacts())
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
        if (Build.VERSION.SDK_INT < 33) return toast("Ajoute la tuile « Envoyer au Mac » depuis l'édition des réglages rapides")
        getSystemService(StatusBarManager::class.java).requestAddTileService(
            ComponentName(this, ClipTileService::class.java), getString(R.string.tile_label),
            Icon.createWithResource(this, R.drawable.ic_link), mainExecutor,
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
        val intent = when (id) {
            CheckId.LOCAL_NETWORK -> return requestPermissions(arrayOf(LOCAL_NETWORK_PERMISSION), REQUEST_LOCAL_NETWORK)
            CheckId.NOTIFICATIONS -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            CheckId.BATTERY_OPTIMIZATION -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri)
            CheckId.BACKGROUND_RESTRICTION -> appDetails
            CheckId.SAMSUNG_NEVER_SLEEPING -> SAMSUNG_BATTERY_SCREENS.map { Intent().setComponent(it) }
                .firstOrNull { it.resolveActivity(packageManager) != null } ?: appDetails
            CheckId.RECENTS_LOCK -> return toast("Ouvre les apps récentes, touche l'icône de Rohy, puis « Garder ouvert ».")
        }
        runCatching { startActivity(intent) }.onFailure { startActivity(appDetails) }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

@Composable
private fun HomeScreen(
    mac: PairedMac?,
    status: SyncStatus,
    onScan: () -> Unit,
    onPasteCode: () -> Unit,
    onSendClipboard: () -> Unit,
    onReconnect: () -> Unit,
    onForget: () -> Unit,
    onAddTile: () -> Unit,
    checks: List<ReliabilityCheck>,
    onFixCheck: (CheckId) -> Unit,
    journal: List<ConnectionEvent>,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Rohy", style = MaterialTheme.typography.headlineMedium)
            if (mac == null) {
                Section("Associer un Mac", Icons.Filled.Info) {
                    Text("Sur le Mac, ouvre Rohy dans la barre de menus puis « Associer un téléphone ».")
                    Button(onClick = onScan, modifier = Modifier.fillMaxWidth()) { Text("Scanner le QR code") }
                    OutlinedButton(onClick = onPasteCode, modifier = Modifier.fillMaxWidth()) {
                        Text("Coller le code d'association")
                    }
                }
            } else {
                val icon = when (status) {
                    is SyncStatus.Connected -> Icons.Filled.CheckCircle
                    is SyncStatus.Rejected, is SyncStatus.LocalNetworkDenied -> Icons.Filled.Warning
                    else -> Icons.Filled.Refresh
                }
                Section(mac.name, icon) {
                    Text(if (status == SyncStatus.NotPaired) SyncStatus.Stopped.label else status.label)
                    Button(onClick = onSendClipboard, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Envoyer le presse-papiers au Mac")
                    }
                    if (!status.isConnected) {
                        OutlinedButton(onClick = onReconnect, modifier = Modifier.fillMaxWidth()) { Text("Reconnecter") }
                    }
                    var confirmForget by remember { mutableStateOf(false) }
                    if (confirmForget) {
                        AlertDialog(
                            onDismissRequest = { confirmForget = false },
                            title = { Text("Oublier ${mac.name} ?") },
                            text = { Text("Il faudra scanner à nouveau le QR code du Mac pour le réassocier.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    confirmForget = false
                                    onForget()
                                }) { Text("Oublier") }
                            },
                            dismissButton = { TextButton(onClick = { confirmForget = false }) { Text("Annuler") } },
                        )
                    }
                    OutlinedButton(onClick = { confirmForget = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Oublier ce Mac")
                    }
                }
                Section("Envoyer depuis le téléphone", Icons.Filled.Share) {
                    Text("Sélectionne un texte puis choisis « Envoyer au Mac » dans le menu (souvent sous ⋮). Ou copie un texte, puis touche la tuile « Envoyer au Mac » des réglages rapides, ou l'action de la notification. Tu peux aussi sélectionner un texte et choisir Partager → Mac (Rohy).")
                    OutlinedButton(onClick = onAddTile, modifier = Modifier.fillMaxWidth()) { Text("Ajouter la tuile") }
                }
                Section("Garder la connexion", Icons.Filled.Settings) {
                    Text("Android coupe les apps en arrière-plan pour économiser la batterie. Ces réglages gardent Rohy connecté.")
                    checks.forEach { CheckRow(it, onFixCheck) }
                }
                Section("Journal de connexion", Icons.Filled.Info) {
                    if (journal.isEmpty()) Text("Aucun événement pour l'instant.")
                    journal.take(20).forEach { event ->
                        Text(
                            "${formatTime(event.atMillis)}  ${event.message}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

private const val REQUEST_STARTUP = 1
private const val REQUEST_LOCAL_NETWORK = 2

private val SAMSUNG_BATTERY_SCREENS = listOf(
    ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"),
    ComponentName("com.samsung.android.sm", "com.samsung.android.sm.battery.ui.BatteryActivity"),
)

private fun formatTime(millis: Long): String =
    SimpleDateFormat("dd/MM HH:mm:ss", Locale.FRANCE).format(Date(millis))

@Composable
private fun CheckRow(check: ReliabilityCheck, onFix: (CheckId) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val (icon, tint, label) = when (check.state) {
                CheckState.OK -> Triple(Icons.Filled.CheckCircle, MaterialTheme.colorScheme.primary, "OK")
                CheckState.TO_FIX -> Triple(Icons.Filled.Warning, MaterialTheme.colorScheme.error, "À corriger")
                CheckState.TO_CHECK -> Triple(Icons.Filled.Info, MaterialTheme.colorScheme.onSurfaceVariant, "À vérifier")
            }
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(check.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            if (check.state != CheckState.OK) {
                TextButton(onClick = { onFix(check.id) }) {
                    Text(if (check.state == CheckState.TO_FIX) "Corriger" else "Ouvrir")
                }
            }
        }
        Text(check.explanation, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Section(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}
