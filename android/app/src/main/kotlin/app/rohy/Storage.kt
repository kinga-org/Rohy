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
            prefs.edit().apply { if (value == null) remove(KEY_MAC) else putString(KEY_MAC, value.encode()) }.apply()
        }

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
        const val KEY_IDENTITY = "identity"
        const val KEY_ALIAS = "rohy_identity_wrap"
        const val KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
