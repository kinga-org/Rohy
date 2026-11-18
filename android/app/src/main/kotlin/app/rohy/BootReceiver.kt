package app.rohy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Restarts the connection after the phone boots or the app is updated, if a Mac is paired. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reason = when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> "démarrage du téléphone"
            Intent.ACTION_MY_PACKAGE_REPLACED -> "mise à jour de Rohy"
            else -> return
        }
        if (Storage(context).pairedMac != null) SyncService.start(context, reason)
    }
}
