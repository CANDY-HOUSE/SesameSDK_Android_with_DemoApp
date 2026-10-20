package co.candyhouse.app.ble

import android.content.Context
import co.candyhouse.sesame.ble.os3.bot2.CHSesameBot2
import co.candyhouse.sesame.ble.os3.sesame5.CHSesame5
import org.json.JSONArray
import org.json.JSONObject

/** BLE primitives of the Biz internal test page; the loop, counter, intervals and UI live in Biz. */
object InternalTest {
    private const val FIRMWARE_PREFS = "firmware_dir_prefs"

    fun firmwareDir(context: Context): String =
        context.getSharedPreferences(FIRMWARE_PREFS, Context.MODE_PRIVATE).getString("firmware_dir", "prod") ?: "prod"

    fun handle(context: Context, ble: BleController, json: JSONObject, reply: (Boolean, Any?, String?) -> Unit) {
        when (json.optString("operation")) {
            // Same selection as the former native sheet: Sesame 5 family toggles, Bot 2 family clicks.
            "devices" -> reply(true, JSONArray().apply {
                ble.allDevices().forEach { device ->
                    val action = when (device) {
                        is CHSesame5 -> "toggle"
                        is CHSesameBot2 -> "click"
                        else -> null
                    }
                    if (action != null) put(JSONObject().put("deviceUUID", device.deviceId.toString().uppercase()).put("action", action))
                }
            }, null)

            "run" -> when (val device = ble.device(json.getString("deviceUUID"))) {
                is CHSesame5 -> device.toggle(historytag = ble.historyTag()) { reply(it.isSuccess, null, if (it.isFailure) "Bluetooth command failed" else null) }
                is CHSesameBot2 -> device.click(historytag = ble.historyTag()) { reply(it.isSuccess, null, if (it.isFailure) "Bluetooth command failed" else null) }
                else -> reply(false, null, "Unsupported device")
            }

            "firmwareDir" -> {
                if (json.has("value")) {
                    val value = json.getString("value")
                    require(value == "prod" || value == "dev")
                    context.getSharedPreferences(FIRMWARE_PREFS, Context.MODE_PRIVATE).edit().putString("firmware_dir", value).apply()
                }
                reply(true, JSONObject().put("firmwareDir", firmwareDir(context)), null)
            }

            else -> reply(false, null, "Unsupported internal test operation")
        }
    }
}
