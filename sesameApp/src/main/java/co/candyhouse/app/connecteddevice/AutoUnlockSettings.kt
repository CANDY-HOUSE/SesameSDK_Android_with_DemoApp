package co.candyhouse.app.connecteddevice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import co.candyhouse.app.SesameApp
import org.json.JSONObject

/** Stores Biz's auto-unlock choices for the geofence and service; the map and permission flow belong to Biz. */
object AutoUnlockSettings {
    private fun permission(context: Context) = when {
        AutoUnlockGeofenceManager.hasPermission(context) -> "granted"
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> "foreground"
        else -> "none"
    }

    private fun sync(context: Context) {
        AutoUnlockGeofenceManager.sync(context); SesameConnectedDeviceService.sync(context)
    }

    fun handle(context: Context, json: JSONObject, requestPermission: () -> Unit, openSettings: () -> Unit): JSONObject {
        val app = context.applicationContext as SesameApp
        val uuid = json.getString("deviceUUID")
        require(app.ble.device(uuid) != null)
        when (json.optString("operation")) {
            "get" -> Unit
            "region" -> {
                app.devicePreferences.setRegion(uuid, json); sync(context)
            }

            "enable" -> if (!json.getBoolean("value")) {
                app.devicePreferences.setEnabled("nohand", uuid, false); app.devicePreferences.setEnabled("nohandg", uuid, false); sync(context)
            } else if (AutoUnlockGeofenceManager.hasPermission(context)) {
                app.devicePreferences.setEnabled("nohand", uuid, true); sync(context)
            }

            "requestPermission" -> requestPermission()
            "openSettings" -> openSettings()
            else -> error("Unsupported auto-unlock operation")
        }
        return app.devicePreferences.snapshot(uuid).put("permission", permission(context))
    }
}
