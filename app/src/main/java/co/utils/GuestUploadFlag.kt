package co.utils

import androidx.core.content.edit

/**
 * 访客上传标记
 *
 * @author frey on 2025/12/23
 */
object GuestUploadFlag {
    private const val KEY_FP = "guestUploadedFingerprintV1"

    fun getFingerprint(): String? =
        SharedPreferencesUtils.preferences.getString(KEY_FP, null)

    fun setFingerprint(fp: String) {
        SharedPreferencesUtils.preferences.edit { putString(KEY_FP, fp) }
    }

    fun clear() {
        SharedPreferencesUtils.preferences.edit { remove(KEY_FP) }
    }
}