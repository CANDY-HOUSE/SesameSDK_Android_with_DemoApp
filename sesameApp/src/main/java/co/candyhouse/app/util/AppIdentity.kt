package co.candyhouse.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.core.content.edit
import java.util.UUID

object AppIdentity {
    @SuppressLint("HardwareIds")
    @Synchronized
    fun get(context: Context): String {
        val legacy = context.getSharedPreferences("${context.packageName}_preferences", 0)
        val push = context.getSharedPreferences("push_subscription", 0)
        val saved = legacy.getString("appIdentifyId", null)?.takeIf { it.isNotBlank() }
            ?: push.getString("installationId", null)?.takeIf { it.isNotBlank() }
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?.trim()?.takeUnless { it.isBlank() || it in setOf("unknown", "null", "9774d56d682e549c") }
        val identity = saved ?: "ap-northeast-1:${androidId ?: UUID.randomUUID()}"
        legacy.edit { putString("appIdentifyId", identity) }
        return identity
    }
}
