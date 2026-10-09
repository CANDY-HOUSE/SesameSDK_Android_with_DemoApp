package co.candyhouse.app.data

import android.content.Context
import androidx.core.content.edit
import co.candyhouse.app.data.local.DeviceKeyDatabase
import co.candyhouse.sesame.ble.CHDevice
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject

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
    suspend fun saved(): List<CHDevice> {
        val ids = preferences.getStringSet("visible", null)
        return all().filter { ids == null || it.deviceUUID.lowercase() in ids || it.deviceUUID.lowercase() in pendingIds() }
    }

    fun remember(rows: JSONArray) {
        val ids = (0 until rows.length()).map { rows.getJSONObject(it).getString("deviceUUID").lowercase() }.toSet()
        preferences.edit { putStringSet("visible", ids) }
        legacy.edit {
            for (index in 0 until rows.length()) {
                val row = rows.getJSONObject(index)
                val id = row.getString("deviceUUID").lowercase()
                putString(id, row.optString("deviceName", row.getString("deviceModel")))
                putInt("l$id", row.optInt("keyLevel", 0))
            }
        }
    }

    suspend fun offlineDevices() = JSONArray().apply {
        saved().forEach { key ->
            put(
                JSONObject().put("deviceUUID", key.deviceUUID.uppercase()).put("deviceModel", key.deviceModel)
                    .put("deviceName", name(key)).put("keyLevel", level(key))
            )
        }
    }

    private suspend fun all(): List<CHDevice> = suspendCancellableCoroutine { continuation ->
        DeviceKeyDatabase.Keys.getAllDB { if (continuation.isActive) continuation.resumeWith(it) }
    }
}
