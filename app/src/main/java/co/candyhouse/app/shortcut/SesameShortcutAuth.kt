package co.candyhouse.app.shortcut

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

/**
 * Signs and verifies shortcut capability tokens with a per-install AndroidKeyStore HMAC key.
 *
 * ShortcutExecuteActivity is exported so other apps (e.g. MacroDroid) can replay a saved shortcut Intent, which would otherwise let anyone forge a (deviceId, action) pair. The key never leaves the KeyStore, so only this install can mint a valid token; clearing app data invalidates every existing shortcut.
 */
object SesameShortcutAuth {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "sesame_shortcut_hmac_key"

    fun sign(deviceId: String, action: SesameShortcutAction): String =
        hmac(deviceId, action).joinToString(separator = "") { "%02x".format(it) }

    fun verify(deviceId: String, action: SesameShortcutAction, token: String?): Boolean {
        if (token.isNullOrEmpty()) return false
        val expected = sign(deviceId, action)
        return MessageDigest.isEqual(
            expected.toByteArray(Charsets.US_ASCII),
            token.toByteArray(Charsets.US_ASCII),
        )
    }

    private fun hmac(deviceId: String, action: SesameShortcutAction): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(getOrCreateKey())
        return mac.doFinal(SesameShortcutContract.tokenMessage(deviceId, action))
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_HMAC_SHA256,
            ANDROID_KEYSTORE,
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY).build(),
        )
        return keyGenerator.generateKey()
    }
}
