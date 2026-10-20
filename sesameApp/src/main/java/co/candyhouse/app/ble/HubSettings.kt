package co.candyhouse.app.ble

import android.os.SystemClock
import co.candyhouse.app.BuildConfig
import co.candyhouse.sesame.ble.CHDeviceLoginStatus
import co.candyhouse.sesame.ble.CHDeviceStatus
import co.candyhouse.sesame.ble.CHProductModel
import co.candyhouse.sesame.ble.os3.hub3.CHHub3
import co.candyhouse.sesame.ble.os3.wm2.CHWifiModule2
import co.candyhouse.sesame.ble.os3.wm2.CHWifiModule2NetWorkStatus
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.L
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import kotlin.time.Duration.Companion.milliseconds

/** Hub3/WM2 BLE session, WiFi scan and OTA transport; network setup flow and pages live in Biz. */
class HubSettings(private val ble: BleController, private val scope: CoroutineScope) {
    private val sessions = mutableMapOf<String, String>()

    private class WifiScan {
        val results = linkedMapOf<String, Short>()
        val startedAt = SystemClock.elapsedRealtime()
        var scanning = true
        var failed = false
        var commandPending = true
        var endReceived = false
        var timeout: Job? = null
    }

    private val wifi = mutableMapOf<String, WifiScan>()
    private val disconnects = mutableMapOf<String, Job>()
    private val watchStarted = mutableMapOf<String, Long>()
    private var wifiUpdate: Job? = null

    private class Ota {
        var progress = 0
        var receivedProgress = false
        var timeout: Job? = null
    }

    private val ota = mutableMapOf<String, Ota>()

    fun watching(id: String) = sessions.containsKey(id)

    fun statusChanged(device: CHWifiModule2, status: CHDeviceStatus) {
        val id = device.deviceId.toString().uppercase()
        trace(id, "ble_status=${status.name}")
        if (status.value != CHDeviceLoginStatus.logined) clearScan(id)
    }

    fun wifiFound(device: CHWifiModule2, ssid: String, rssi: Short) {
        val id = device.deviceId.toString().uppercase()
        val scan = wifi[id]?.takeIf { it.scanning } ?: return
        val first = scan.results.isEmpty()
        val old = scan.results[ssid]
        if (old != null && old >= rssi) return
        scan.results[ssid] = rssi
        if (first) {
            trace(id, "wifi_first elapsed_ms=${SystemClock.elapsedRealtime() - scan.startedAt}")
            ble.snapshot()
        } else if (wifiUpdate == null) {
            wifiUpdate = scope.launch(Dispatchers.Main) {
                delay(100.milliseconds)
                wifiUpdate = null
                ble.snapshot()
            }
        }
    }

    fun wifiScanStarted(device: CHHub3) {
        trace(device.deviceId.toString().uppercase(), "wifi_start_marker")
    }

    fun wifiScanFinished(device: CHHub3) {
        val id = device.deviceId.toString().uppercase()
        wifi[id]?.takeIf { it.scanning }?.let { scan ->
            scan.endReceived = true
            trace(id, "wifi_end_marker")
            if (!scan.commandPending) finishScan(id, scan, false, "end_marker")
        }
    }

    fun otaProgress(device: CHWifiModule2, percent: Byte) {
        val id = device.deviceId.toString().uppercase()
        val progress = percent.toInt() and 255
        if (progress !in 0..100) return
        val state = ota[id] ?: if (progress < 100) Ota().also { ota[id] = it } else return
        state.progress = progress
        state.receivedProgress = true
        armOtaTimeout(id, state)
        trace(id, "ota_progress=$progress")
        ble.snapshot()
    }

    fun forget(id: String) {
        clearSession(id)
        ota.remove(id)?.timeout?.cancel()
    }

    /** A grace connection is only useful while the app stays in the foreground. */
    fun endGrace() {
        disconnects.keys.toList().forEach { id ->
            clearSession(id)
            ble.device(id)?.disconnect {}
        }
    }

    fun close() {
        (sessions.keys + disconnects.keys + wifi.keys).toSet().forEach(::clearSession)
        wifiUpdate?.cancel(); wifiUpdate = null
        ota.values.forEach { it.timeout?.cancel() }; ota.clear()
    }

    fun snapshot(id: String, device: CHWifiModule2, connected: Boolean): JSONObject {
        val hub = JSONObject()
        if (connected) {
            device.mechSetting?.let { hub.put("wifiSsid", it.wifiSSID).put("wifiPwd", it.wifiPassWord) }
            (device.mechStatus as? CHWifiModule2NetWorkStatus)?.let {
                hub.put(
                    "network", JSONObject().put("isAPWork", it.isAPWork == true)
                        .put("isNetwork", it.isNetWork == true).put("isIoTWork", it.isIOTWork == true)
                        .put("isBindingAPWork", it.isAPConnecting).put("isConnectingNetwork", it.isConnectingNet)
                        .put("isConnectingIoT", it.isConnectingIOT)
                )
            }
        }
        wifi[id]?.let { scan ->
            hub.put("wifiScanning", scan.scanning).put("wifiScanFailed", scan.failed)
            hub.put("wifiScan", JSONArray().apply {
                scan.results.forEach { (ssid, rssi) -> put(JSONObject().put("ssid", ssid).put("rssi", rssi.toInt())) }
            })
        }
        ota[id]?.let { hub.put("otaProgress", it.progress) }
        return hub
    }

    private fun trace(id: String, event: String) {
        if (BuildConfig.DEBUG) {
            val elapsed = watchStarted[id]?.let { SystemClock.elapsedRealtime() - it } ?: 0
            L.d("Hub3Timing", "hub=${id.takeLast(8)} watch_ms=$elapsed $event")
        }
    }

    private fun clearScan(id: String) {
        wifi.remove(id)?.timeout?.cancel()
    }

    private fun clearSession(id: String) {
        disconnects.remove(id)?.cancel()
        sessions.remove(id)
        watchStarted.remove(id)
        clearScan(id)
    }

    private fun finishScan(id: String, scan: WifiScan, failed: Boolean, reason: String) {
        if (wifi[id] !== scan || !scan.scanning) return
        scan.scanning = false
        scan.failed = failed
        scan.timeout?.cancel()
        scan.timeout = null
        trace(id, "wifi_finished reason=$reason count=${scan.results.size} elapsed_ms=${SystemClock.elapsedRealtime() - scan.startedAt}")
        ble.snapshot()
    }

    private fun scanWifi(id: String, hub: CHWifiModule2, refresh: Boolean) {
        check(sessions.containsKey(id) && hub.deviceStatus.value == CHDeviceLoginStatus.logined)
        if (ota[id]?.progress?.let { it in 0..99 } == true) return
        val previous = wifi[id]
        if (previous?.scanning == true || previous?.commandPending == true || (!refresh && previous != null && !previous.failed &&
                    SystemClock.elapsedRealtime() - previous.startedAt < 30000)
        ) {
            trace(id, "wifi_reused scanning=${previous?.scanning}")
            return
        }
        val scan = WifiScan()
        wifi[id] = scan
        trace(id, "wifi_requested refresh=$refresh")
        ble.snapshot()
        // Older firmware may not publish an end marker. Never start a second scan meanwhile.
        scan.timeout = scope.launch(Dispatchers.Main) {
            delay(15000.milliseconds)
            finishScan(id, scan, scan.results.isEmpty() || scan.commandPending, "timeout")
            if (scan.commandPending) {
                // Reset an unacknowledged SDK command before allowing another physical scan.
                trace(id, "wifi_ack_timeout_disconnect")
                hub.disconnect {}
            }
        }
        hub.scanWifiSSID { result ->
            scope.launch(Dispatchers.Main) {
                if (wifi[id] !== scan) return@launch
                scan.commandPending = false
                trace(id, "wifi_ack success=${result.isSuccess} elapsed_ms=${SystemClock.elapsedRealtime() - scan.startedAt}")
                if (result.isFailure) finishScan(id, scan, true, "rejected")
                else if (scan.endReceived) finishScan(id, scan, false, "end_marker")
            }
        }
    }

    private fun armOtaTimeout(id: String, state: Ota) {
        state.timeout?.cancel()
        state.timeout = null
        if (state.progress !in 0..99) return
        state.timeout = scope.launch(Dispatchers.Main) {
            delay(180000.milliseconds)
            if (ota[id] !== state) return@launch
            state.progress = -7
            trace(id, "ota_timeout")
            ble.snapshot()
        }
    }

    suspend fun handle(json: JSONObject): JSONObject = withTimeoutOrNull(
        if (json.optString("operation") == "connectWifi") 25000.milliseconds else 8000.milliseconds
    ) {
        val id = json.getString("deviceUUID").uppercase()
        val hub = ble.device(id) as? CHWifiModule2 ?: error("Unsupported WiFi device")
        when (json.getString("operation")) {
            "watch" -> {
                disconnects.remove(id)?.cancel()
                sessions[id] = json.getString("session")
                watchStarted[id] = SystemClock.elapsedRealtime()
                trace(id, "watch connected=${hub.deviceStatus.value == CHDeviceLoginStatus.logined}")
                ble.connect(hub)
            }

            "unwatch" -> if (sessions[id] == json.getString("session")) {
                sessions.remove(id)
                disconnects.remove(id)?.cancel()
                trace(id, "unwatch grace_ms=${if (ble.foreground) 10000 else 0}")
                disconnects[id] = scope.launch(Dispatchers.Main) {
                    if (ble.foreground) delay(10000.milliseconds)
                    trace(id, "disconnect_after_unwatch")
                    clearSession(id)
                    hub.disconnect {}
                }
            }

            "scan" -> scanWifi(id, hub, json.optBoolean("refresh"))
            "ssid" -> {
                check(hub.deviceStatus.value == CHDeviceLoginStatus.logined)
                LockSettings.awaitResult<CHEmpty> { hub.setWifiSSID(json.getString("ssid"), it) }
            }

            "password" -> {
                check(hub.deviceStatus.value == CHDeviceLoginStatus.logined)
                LockSettings.awaitResult<CHEmpty> { hub.setWifiPassword(json.getString("password"), it) }
            }

            "connectWifi" -> {
                require(hub.productModel == CHProductModel.WM2)
                check(hub.deviceStatus.value == CHDeviceLoginStatus.logined)
                LockSettings.awaitResult<CHEmpty>(hub::connectWifi)
            }

            "firmware" -> {
                require(hub is CHHub3)
                check(hub.deviceStatus.value == CHDeviceLoginStatus.logined)
                if (ota[id]?.progress?.let { it in 0..99 } != true) {
                    // WiFi scanning must not compete with OTA or disconnect it on a scan timeout.
                    clearScan(id)
                    val state = Ota()
                    ota[id] = state
                    armOtaTimeout(id, state)
                    trace(id, "ota_requested")
                    // Like the original bridge, dispatch the command and follow OTA notifications.
                    // A reboot can drop the command reply while the Hub continues downloading.
                    hub.updateFirmwareBleOnly { result ->
                        scope.launch(Dispatchers.Main) {
                            if (ota[id] !== state || state.receivedProgress) return@launch
                            if (result.isFailure) {
                                state.progress = -7
                                armOtaTimeout(id, state)
                                trace(id, "ota_rejected")
                                ble.snapshot()
                            }
                        }
                    }
                }
            }

            "firmwareFinish" -> if (ota[id]?.progress == 100) ota.remove(id)?.timeout?.cancel()
            "version" -> {
                check(hub.deviceStatus.value == CHDeviceLoginStatus.logined)
                val version = LockSettings.awaitResult(hub::getVersionTag)
                trace(id, "version_read")
                return@withTimeoutOrNull JSONObject().put("version", version)
            }

            else -> error("Unsupported Hub operation")
        }
        ble.snapshot()
        JSONObject()
    } ?: error("Hub operation timed out")
}
