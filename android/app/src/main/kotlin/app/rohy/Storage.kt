package app.rohy

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import app.rohy.core.Base64Url
import app.rohy.core.Identity
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the paired Mac and the phone's X25519 identity. The private key is encrypted
 * with an AES key that lives in the Android Keystore and cannot be exported.
 */
class Storage(context: Context) {
    private val prefs = context.getSharedPreferences("rohy", Context.MODE_PRIVATE)

    var pairedMac: PairedMac?
        get() = PairedMac.decode(prefs.getString(KEY_MAC, null))
        set(value) {
            val samePairing = value != null && pairedMac?.publicKey?.contentEquals(value.publicKey) == true
            prefs.edit().apply {
                if (value == null) remove(KEY_MAC).remove(KEY_PAIRED_AT) else putString(KEY_MAC, value.encode())
                if (value != null && !samePairing) putLong(KEY_PAIRED_AT, System.currentTimeMillis())
            }.apply()
        }

    /** When the current computer was paired; null for pairings made before this was recorded. */
    val pairedAtMillis: Long?
        get() = prefs.getLong(KEY_PAIRED_AT, 0).takeIf { it > 0 }

    var journalText: String?
        get() = prefs.getString(KEY_JOURNAL, null)
        set(value) = prefs.edit().putString(KEY_JOURNAL, value).apply()

    /** Edge handle (phase 10): switched on in the app, hidden with its ✕, and where it sits on the edge. */
    var handleEnabled: Boolean
        get() = prefs.getBoolean(KEY_HANDLE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_HANDLE_ENABLED, value).apply()

    /** Whether the Quick Settings tile is on the panel, as last reported by the tile or the add request. */
    var tileAdded: Boolean
        get() = prefs.getBoolean(KEY_TILE_ADDED, false)
        set(value) = prefs.edit().putBoolean(KEY_TILE_ADDED, value).apply()

    /** Side of the screen the edge handle sits on (right by default, like Samsung's). */
    var handleSide: HandleSide
        get() = if (prefs.getString(KEY_HANDLE_SIDE, null) == "left") HandleSide.LEFT else HandleSide.RIGHT
        set(value) = prefs.edit().putString(KEY_HANDLE_SIDE, if (value == HandleSide.LEFT) "left" else "right").apply()

    /** Where the handle's middle is along its side, from 0 (top) to 1 (bottom). */
    var handlePosition: Float
        get() = EdgeHandleRules.clampPosition(prefs.getFloat(KEY_HANDLE_POSITION, EdgeHandleRules.DEFAULT_POSITION))
        set(value) = prefs.edit().putFloat(KEY_HANDLE_POSITION, EdgeHandleRules.clampPosition(value)).apply()

    @Synchronized
    fun identity(): Identity {
        prefs.getString(KEY_IDENTITY, null)?.let(::unwrap)?.let { return Identity(it) }
        val identity = Identity.generate()
        prefs.edit().putString(KEY_IDENTITY, wrap(identity.privateKey)).apply()
        return identity
    }

    private fun wrap(secret: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey())
        return Base64Url.encode(cipher.iv) + "." + Base64Url.encode(cipher.doFinal(secret))
    }

    private fun unwrap(stored: String): ByteArray? = runCatching {
        val (iv, data) = stored.split('.').map { Base64Url.decode(it)!! }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, wrappingKey(), GCMParameterSpec(128, iv))
        cipher.doFinal(data)
    }.getOrNull()

    private fun wrappingKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEY_MAC = "paired_mac"
        const val KEY_PAIRED_AT = "paired_at"
        const val KEY_IDENTITY = "identity"
        const val KEY_JOURNAL = "journal"
        const val KEY_HANDLE_ENABLED = "handle_enabled"
        const val KEY_HANDLE_POSITION = "handle_position"
        const val KEY_HANDLE_SIDE = "handle_side"
        const val KEY_TILE_ADDED = "tile_added"
        const val KEY_ALIAS = "rohy_identity_wrap"
        const val KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
