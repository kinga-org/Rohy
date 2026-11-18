package app.rohy

import android.app.Activity
import android.app.PendingIntent
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

/** Shared by the tile, the notification action and the share sheet. */
internal fun Context.sendToMac(text: String?) {
    when {
        Storage(this).pairedMac == null -> {
            Toast.makeText(this, "Associe d'abord un ordinateur dans Rohy", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        text.isNullOrEmpty() -> Toast.makeText(this, "Presse-papiers vide", Toast.LENGTH_SHORT).show()
        else -> {
            SyncService.send(this, text)
            Toast.makeText(this, "Envoi vers l'ordinateur", Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Invisible activity: Android only lets the app in the foreground read the clipboard,
 * so the tile and the notification open this for a moment, read, send, and close.
 */
class SendClipboardActivity : Activity() {
    private var handled = false

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || handled) return
        handled = true
        val clip = getSystemService(ClipboardManager::class.java).primaryClip
        sendToMac(clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString())
        finish()
    }
}

/** "Share → Mac (Rohy)" target for selected text. */
class ShareReceiverActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent?.takeIf { it.action == Intent.ACTION_SEND }?.getStringExtra(Intent.EXTRA_TEXT)
        sendToMac(text)
        finish()
    }
}

/** "Envoyer au Mac" entry in the text selection menu (next to Copier, Partager). */
class ProcessTextActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sendToMac(intent?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString())
        finish()
    }
}

/** Quick Settings tile "Envoyer au Mac". */
class ClipTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = if (Storage(this@ClipTileService).pairedMac == null) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, SendClipboardActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
