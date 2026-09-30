package co.candyhouse.app.connecteddevice

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import org.json.JSONObject
import java.util.UUID

/** Preserve the original installation's per-device preference keys. */
class DevicePreferences(private val context: Context) {
    private val prefs = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
    private fun id(value: String) = UUID.fromString(value).toString()
    fun enabled(prefix: String, uuid: String) = prefs.getBoolean(prefix + id(uuid), false)
    fun setEnabled(prefix: String, uuid: String, value: Boolean) {
        prefs.edit().putBoolean(prefix + id(uuid), value).apply()
    }

    fun nfc(uuid: String) = prefs.getString("nfc" + id(uuid), "") ?: ""
    fun setNfc(uuid: String, value: String?) {
        prefs.edit().putString("nfc" + id(uuid), value).apply()
    }

    fun snapshot(uuid: String) = JSONObject().apply {
        val key = id(uuid)
        put("nfc", nfc(uuid)); put("widget", enabled("wid", uuid)); put("autoUnlock", enabled("nohand", uuid))
        put("latitude", prefs.getFloat("getNOHandLeft$key", 0f).toDouble())
        put("longitude", prefs.getFloat("getNOHandRight$key", 0f).toDouble())
        put("radius", prefs.getFloat("getNOHandRadius$key", 150f).coerceIn(20f, 500f).toDouble())
        put("notificationsEnabled", NotificationManagerCompat.from(context).areNotificationsEnabled())
    }

    fun setRegion(uuid: String, data: JSONObject) {
        val key = id(uuid);
        val edit = prefs.edit()
        if (data.has("latitude")) {
            val value = data.getDouble("latitude"); require(value.isFinite() && value in -90.0..90.0); edit.putFloat("getNOHandLeft$key", value.toFloat())
        }
        if (data.has("longitude")) {
            val value = data.getDouble("longitude"); require(value.isFinite() && value in -180.0..180.0); edit.putFloat("getNOHandRight$key", value.toFloat())
        }
        if (data.has("radius")) {
            val value = data.getDouble("radius"); require(value.isFinite() && value in 20.0..500.0); edit.putFloat("getNOHandRadius$key", value.toFloat())
        }
        edit.apply()
    }

    fun clear(uuid: String) {
        val key = id(uuid);
        val edit = prefs.edit(); listOf(
            "wid",
            "nohand",
            "nohandg",
            "nfc",
            "getNOHandLeft",
            "getNOHandRight",
            "getNOHandRadius",
            "l",
            ""
        ).forEach { edit.remove(it + key) }; edit.apply()
    }
}
