package co.candyhouse.app.data

import android.content.Context
import androidx.core.content.edit
import co.candyhouse.app.data.local.DeviceKeyDatabase
import co.candyhouse.sesame.ble.CHDevice
import kotlinx.coroutines.suspendCancellableCoroutine

/** Unsent guest/registration keys survive refresh and failed web uploads. */
class KeyHandoff(context: Context) {
    private val preferences = context.getSharedPreferences("key_handoff", 0)
    private val legacy = context.getSharedPreferences("${context.packageName}_preferences", 0)
    suspend fun initialize(hasLegacyUser: Boolean) {
        if (preferences.getBoolean("initialized", false)) return
        val keys = all()
        preferences.edit {
            putBoolean("initialized", true)
                .putStringSet("pending", if (hasLegacyUser) emptySet() else keys.map { it.deviceUUID.lowercase() }.toSet())
        }
    }

    @Synchronized
    fun add(key: CHDevice) {
        preferences.edit { putStringSet("pending", pendingIds() + key.deviceUUID.lowercase()) }
    }

    @Synchronized
    fun acknowledge(id: String) {
        preferences.edit { putStringSet("pending", pendingIds() - id.lowercase()) }
    }

    private fun pendingIds() = preferences.getStringSet("pending", emptySet()).orEmpty().toSet()
    suspend fun pending() = all().filter { it.deviceUUID.lowercase() in pendingIds() }
    suspend fun key(id: String) = all().first { it.deviceUUID.equals(id, ignoreCase = true) }
    fun name(key: CHDevice) = legacy.getString(key.deviceUUID.lowercase(), key.deviceModel) ?: key.deviceModel
    fun level(key: CHDevice) = legacy.getInt("l${key.deviceUUID.lowercase()}", 0)
    private suspend fun all(): List<CHDevice> = suspendCancellableCoroutine { continuation ->
        DeviceKeyDatabase.Keys.getAllDB { if (continuation.isActive) continuation.resumeWith(it) }
    }
}
