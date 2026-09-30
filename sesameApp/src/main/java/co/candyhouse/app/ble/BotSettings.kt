package co.candyhouse.app.ble

import co.candyhouse.sesame.ble.CHDeviceLoginStatus
import co.candyhouse.sesame.ble.os3.bot2.CHSesameBot2
import co.candyhouse.sesame.utils.CHEmpty
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import kotlin.time.Duration.Companion.milliseconds

/** Bot script byte protocol only; aliases, ordering and cloud persistence belong to Biz. */
object BotSettings {
    suspend fun handle(ble: BleController, json: JSONObject): JSONObject = withTimeout(8000.milliseconds) {
        val device = ble.device(json.getString("deviceUUID")) as? CHSesameBot2 ?: error("Unsupported Bot")
        check(device.deviceStatus.value == CHDeviceLoginStatus.logined)
        val index = json.getInt("index"); require(index in 0..9)
        when (json.getString("operation")) {
            "read" -> {
                val event = LockSettings.awaitResult { device.getCurrentScript(index.toUByte(), it) }
                JSONObject().put("nameLength", event.nameLength.toInt())
                    .put("name", JSONArray(event.name.map { it.toInt() and 255 }))
                    .put("actions", JSONArray().apply {
                        event.actions.orEmpty().forEach { put(JSONObject().put("type", it.action.value.toInt()).put("time", it.time.toInt())) }
                    })
            }

            "select" -> {
                LockSettings.awaitResult<CHEmpty> { device.selectScript(index.toUByte(), it) }
                device.scripts.curIdx = index.toUByte()
                ble.snapshot()
                JSONObject()
            }

            "write" -> {
                val name = json.getJSONArray("name");
                val length = json.getInt("nameLength")
                val actions = json.getJSONArray("actions")
                require(length in 0..20 && name.length() in length..20 && actions.length() <= 20)
                val payload = ByteArray(22 + actions.length() * 2)
                payload[0] = length.toByte()
                repeat(name.length()) { val value = name.getInt(it); require(value in 0..255); payload[1 + it] = value.toByte() }
                payload[21] = actions.length().toByte()
                repeat(actions.length()) { i ->
                    val item = actions.getJSONObject(i);
                    val type = item.getInt("type");
                    val time = item.getInt("time")
                    require(type in 0..3 && time in 0..255)
                    payload[22 + i * 2] = type.toByte(); payload[23 + i * 2] = time.toByte()
                }
                LockSettings.awaitResult<CHEmpty> { device.sendClickScript(index.toUByte(), payload, it) }
                JSONObject().put("actionData", payload.joinToString("") { "%02x".format(it.toInt() and 255) })
            }

            else -> error("Unsupported script operation")
        }
    }
}
