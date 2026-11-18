package app.rohy

import android.Manifest
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import app.rohy.core.PairingPayload

class MainActivity : ComponentActivity() {
    private lateinit var storage: Storage
    private var pairedMac by mutableStateOf<PairedMac?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storage = Storage(this)
        pairedMac = storage.pairedMac
        requestNotificationPermission()
        if (pairedMac != null) SyncService.start(this)
        setContent {
            val context = LocalContext.current
            val scheme = if (Build.VERSION.SDK_INT >= 31) dynamicLightColorScheme(context) else lightColorScheme()
            MaterialTheme(colorScheme = scheme) {
                val status by SyncService.status.collectAsState()
                HomeScreen(
                    mac = pairedMac,
                    status = status,
                    onScan = ::scanQrCode,
                    onPasteCode = ::pairFromClipboard,
                    onSendClipboard = { sendToMac(readClipboard()) },
                    onReconnect = { SyncService.start(this) },
                    onForget = ::forgetMac,
                    onAddTile = ::requestTile,
                    onBatterySettings = { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        pairedMac = storage.pairedMac
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
        pairedMac = PairedMac.from(payload)
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

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
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
    onBatterySettings: () -> Unit,
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
                    is SyncStatus.Rejected -> Icons.Filled.Warning
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
                    OutlinedButton(onClick = onForget, modifier = Modifier.fillMaxWidth()) {
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
                    Text("Sur Samsung, mets Rohy dans « Applis jamais en veille » et la batterie sur « Non restreinte ».")
                    OutlinedButton(onClick = onBatterySettings, modifier = Modifier.fillMaxWidth()) {
                        Text("Ouvrir les réglages de batterie")
                    }
                }
            }
        }
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
