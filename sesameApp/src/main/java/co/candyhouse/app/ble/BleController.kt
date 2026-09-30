package co.candyhouse.app.ble

import android.os.SystemClock
import android.util.Log
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.connecteddevice.AutoUnlockGeofenceManager
import co.candyhouse.app.connecteddevice.SesameConnectedDeviceService
import co.candyhouse.app.data.BleBackend
import co.candyhouse.app.data.local.DeviceKeyDatabase
import co.candyhouse.sesame.ble.CHBleManager
import co.candyhouse.sesame.ble.CHBleStatusDelegate
import co.candyhouse.sesame.ble.CHDevice
import co.candyhouse.sesame.ble.CHDeviceLoginStatus
import co.candyhouse.sesame.ble.CHDeviceStatus
import co.candyhouse.sesame.ble.CHDevices
import co.candyhouse.sesame.ble.CHProductModel
import co.candyhouse.sesame.ble.CHScanStatus
import co.candyhouse.sesame.ble.CHSesameLock
import co.candyhouse.sesame.ble.os2.bike.CHSesameBike
import co.candyhouse.sesame.ble.os2.bot.CHSesameBot
import co.candyhouse.sesame.ble.os2.sesame2.CHSesame2
import co.candyhouse.sesame.ble.os3.bike2.CHSesameBike2
import co.candyhouse.sesame.ble.os3.bot2.CHSesameBot2
import co.candyhouse.sesame.ble.os3.hub3.CHHub3
import co.candyhouse.sesame.ble.os3.hub3.CHHub3Delegate
import co.candyhouse.sesame.ble.os3.sesame5.CHSesame5
import co.candyhouse.sesame.ble.os3.wm2.CHWifiModule2
import co.candyhouse.sesame.ble.os3.wm2.CHWifiModule2MechSettings
import co.candyhouse.sesame.ble.os3.wm2.CHWifiModule2NetWorkStatus
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

/** BLE state and commands only; the web app owns lists, ordering and navigation. */
class BleController(
    private val scope: CoroutineScope, private val backend: BleBackend,
    private val publish: (JSONArray) -> Unit
) {
    private val devices = linkedMapOf<String, CHDevices>()
    val peripheralSettings = PeripheralSettings(this, scope)
    private val hubSessions = mutableMapOf<String, String>()

    private class HubWifiScan {
        val results = linkedMapOf<String, Short>()
        val startedAt = SystemClock.elapsedRealtime()
        var scanning = true
        var failed = false
        var commandPending = true
        var endReceived = false
        var timeout: Job? = null
    }

    private val hubWifi = mutableMapOf<String, HubWifiScan>()
    private val hubDisconnects = mutableMapOf<String, Job>()
    private val hubWatchStarted = mutableMapOf<String, Long>()
    private var hubWifiUpdate: Job? = null

    private class HubOta {
        var progress = 0
        var receivedProgress = false
        var timeout: Job? = null
    }

    private val hubOta = mutableMapOf<String, HubOta>()
    private val updates = Mutex()
    private val appliedKeys = mutableMapOf<String, CHDevice>()
    private var foreground = false
    private var receivedCloudList = false
    var background = false
    var cloudRows = JSONArray()
        private set
    val observers = mutableSetOf<(JSONArray) -> Unit>()
    fun allDevices(): List<CHDevices> = devices.values.toList()
    suspend fun restoreSaved() {
        if (devices.isNotEmpty() || receivedCloudList) return
        val keys = suspendCancellableCoroutine<List<CHDevice>> { continuation ->
            DeviceKeyDatabase.Keys.getAllDB { if (continuation.isActive) continuation.resumeWith(it) }
        }
        keys.forEach { key ->
            CHBleManager.restoreDevice(key)?.let { device ->
                appliedKeys[key.deviceUUID.uppercase()] = key
                devices[key.deviceUUID.uppercase()] = device
                device.delegate = delegate
            }
        }
        snapshot()
    }

    private val scanDelegate = object : CHBleStatusDelegate {
        override fun didScanChange(ss: CHScanStatus) {
            scope.launch { snapshot() }
        }
    }
    private val localPreferences get() = DeviceKeyDatabase.context.let { it.getSharedPreferences("${it.packageName}_preferences", 0) }
    private fun historyBytes(value: String?) = runCatching {
        val uuid = UUID.fromString(value)
        ByteBuffer.allocate(16).putLong(uuid.mostSignificantBits).putLong(uuid.leastSignificantBits).array()
    }.getOrDefault(ByteArray(16) { 0xff.toByte() })

    private var history = historyBytes(localPreferences.getString("environmentId", null))
    private val delegate = object : CHHub3Delegate {
        override fun onBleDeviceStatusChanged(device: CHDevices, status: CHDeviceStatus) {
            scope.launch(Dispatchers.Main) {
                if (device is CHWifiModule2) {
                    val id = device.deviceId.toString().uppercase()
                    hubTrace(id, "ble_status=${status.name}")
                    if (status.value != CHDeviceLoginStatus.logined) clearHubScan(id)
                }
                peripheralSettings.statusChanged(device, status)
                connect(device)
                if (device is CHSesameBot2 && status.value == CHDeviceLoginStatus.logined) {
                    device.getScriptNameList { scope.launch(Dispatchers.Main) { snapshot() } }
                }
                snapshot()
            }
        }

        override fun onSensorDetectIntervalReceive(device: CHDevices, intervalMs: Short) {
            scope.launch { snapshot() }
        }

        override fun onBleTxPowerReceive(device: CHDevices, txPower: Byte) {
            scope.launch { snapshot() }
        }

        override fun onLockUnlockSwitchPointReceive(device: CHDevices, point: Short) {
            scope.launch { snapshot() }
        }

        override fun onSSM2KeysChanged(device: CHWifiModule2, ssm2keys: Map<String, String>) {
            scope.launch(Dispatchers.Main) { snapshot() }
        }

        override fun onAPSettingChanged(device: CHWifiModule2, settings: CHWifiModule2MechSettings) {
            scope.launch(Dispatchers.Main) { snapshot() }
        }

        override fun onScanWifiSID(device: CHWifiModule2, ssid: String, rssi: Short) {
            scope.launch(Dispatchers.Main) {
                val id = device.deviceId.toString().uppercase()
                val scan = hubWifi[id]?.takeIf { it.scanning } ?: return@launch
                val first = scan.results.isEmpty()
                val old = scan.results[ssid]
                if (old != null && old >= rssi) return@launch
                scan.results[ssid] = rssi
                if (first) {
                    hubTrace(id, "wifi_first elapsed_ms=${SystemClock.elapsedRealtime() - scan.startedAt}")
                    snapshot()
                } else if (hubWifiUpdate == null) {
                    hubWifiUpdate = scope.launch(Dispatchers.Main) {
                        delay(100.milliseconds)
                        hubWifiUpdate = null
                        snapshot()
                    }
                }
            }
        }

        override fun onWifiScanStarted(device: CHHub3) {
            scope.launch(Dispatchers.Main) {
                hubTrace(device.deviceId.toString().uppercase(), "wifi_start_marker")
            }
        }

        override fun onWifiScanFinished(device: CHHub3) {
            scope.launch(Dispatchers.Main) {
                val id = device.deviceId.toString().uppercase()
                hubWifi[id]?.takeIf { it.scanning }?.let { scan ->
                    scan.endReceived = true
                    hubTrace(id, "wifi_end_marker")
                    if (!scan.commandPending) finishHubScan(id, scan, false, "end_marker")
                }
            }
        }

        override fun onOTAProgress(device: CHWifiModule2, percent: Byte) {
            scope.launch(Dispatchers.Main) {
                val id = device.deviceId.toString().uppercase()
                val progress = percent.toInt() and 255
                if (progress !in 0..100) return@launch
                val ota = hubOta[id] ?: if (progress < 100) HubOta().also { hubOta[id] = it } else return@launch
                ota.progress = progress
                ota.receivedProgress = true
                armHubOtaTimeout(id, ota)
                hubTrace(id, "ota_progress=$progress")
                snapshot()
            }
        }

        override fun onMechStatus(device: CHDevices) {
            scope.launch(Dispatchers.Main) { snapshot() }
        }
    }

    fun accept(rows: JSONArray, historyTag: String?) {
        scope.launch {
            updates.withLock {
                val keys = runCatching {
                    (0 until rows.length()).map { index ->
                        val row = rows.getJSONObject(index)
                        require(CHProductModel.getByModel(row.getString("deviceModel")) != null)
                        CHDevice(
                            UUID.fromString(row.getString("deviceUUID")).toString(), row.getString("deviceModel"),
                            row.optString("deviceName").toByteArray(), row.getString("keyIndex"),
                            row.getString("secretKey"), row.getString("sesame2PublicKey")
                        )
                    }.also { require(it.map { key -> key.deviceUUID }.distinct().size == it.size) }
                }.getOrNull() ?: return@withLock
                val incoming = keys.map { it.deviceUUID.uppercase() }.toSet()
                receivedCloudList = true
                cloudRows = rows
                // Keep unsent keys on disk; an H5 empty list must not erase legacy guest keys.
                devices.keys.filter { it !in incoming }.forEach { id ->
                    peripheralSettings.forget(id)
                    clearHubSession(id)
                    hubOta.remove(id)?.timeout?.cancel()
                    appliedKeys.remove(id)
                    devices.remove(id)?.let { it.delegate = null; it.disconnect {} }
                }
                if (!historyTag.isNullOrBlank()) {
                    history = historyBytes(historyTag)
                    localPreferences.edit().putString("environmentId", historyTag).apply()
                } else if (rows.length() == 0) {
                    history = historyBytes(null)
                    localPreferences.edit().remove("environmentId").apply()
                }
                backend.gatewayDevices = (0 until rows.length()).map { rows.getJSONObject(it) }
                    .filter { it.optJSONObject("stateInfo")?.optBoolean("wm2State") == true }
                    .map { it.getString("deviceUUID").uppercase() }.toSet()
                for (key in keys) {
                    val previous = appliedKeys[key.deviceUUID.uppercase()]
                    if (previous != null && previous.deviceModel == key.deviceModel && previous.keyIndex == key.keyIndex &&
                        previous.secretKey == key.secretKey && previous.sesame2PublicKey == key.sesame2PublicKey &&
                        previous.historyTag.contentEquals(key.historyTag)
                    ) continue
                    val stored = suspendCancellableCoroutine<Boolean> { continuation ->
                        DeviceKeyDatabase.Keys.insert(key) { if (continuation.isActive) continuation.resumeWith(Result.success(it.isSuccess)) }
                    }
                    if (!stored) continue
                    CHBleManager.restoreDevice(key)?.let { device ->
                        appliedKeys[key.deviceUUID.uppercase()] = key
                        devices[key.deviceUUID.uppercase()] = device
                        device.delegate = delegate
                        connect(device)
                    }
                }
                snapshot()
                val context = DeviceKeyDatabase.context
                AutoUnlockGeofenceManager.sync(context)
                SesameConnectedDeviceService.sync(context)
            }
        }
    }

    private fun connect(device: CHDevices) {
        if ((foreground || background) && !BleFirmwareUpdate.active(device.deviceId.toString()) && (device is CHSesameLock || (foreground && peripheralSettings.shouldConnect(device)) || (foreground && device is CHWifiModule2 && hubSessions.containsKey(
                device.deviceId.toString().uppercase()
            ))) && device.deviceStatus == CHDeviceStatus.ReceivedAdV
        ) device.connect {}
    }

    fun resume() {
        CHBleManager.statusDelegate = scanDelegate
        foreground = true; CHBleManager.enableScan {}; devices.values.forEach(::connect); snapshot()
    }

    fun startBackground() {
        background = true
        CHBleManager.statusDelegate = scanDelegate
        runCatching { CHBleManager.enableScan {}; reconnect() }
    }

    fun stopBackground() {
        background = false; if (!foreground) CHBleManager.disableScan {}
    }

    fun reconnect() {
        devices.values.forEach(::connect)
    }

    fun pause() {
        foreground = false; if (!background) CHBleManager.disableScan {}
        // A grace connection is only useful while the app stays in the foreground.
        hubDisconnects.keys.toList().forEach { id ->
            clearHubSession(id)
            devices[id]?.disconnect {}
        }
    }

    fun close() {
        peripheralSettings.close()
        if (CHBleManager.statusDelegate === scanDelegate) CHBleManager.statusDelegate = null
        background = false
        (hubSessions.keys + hubDisconnects.keys + hubWifi.keys).toSet().forEach(::clearHubSession)
        hubWifiUpdate?.cancel(); hubWifiUpdate = null
        hubOta.values.forEach { it.timeout?.cancel() }; hubOta.clear()
        pause(); devices.values.forEach { it.delegate = null; it.disconnect {} }; devices.clear(); appliedKeys.clear()
        receivedCloudList = false; cloudRows = JSONArray(); backend.gatewayDevices = emptySet()
    }

    suspend fun saveModel(device: CHDevices) {
        val id = device.deviceId.toString().uppercase()
        val key = requireNotNull(appliedKeys[id]).copy(deviceModel = device.productModel.deviceModel())
        suspendCancellableCoroutine<String> { continuation ->
            DeviceKeyDatabase.Keys.insert(key) { if (continuation.isActive) continuation.resumeWith(it) }
        }
        appliedKeys[id] = key
        snapshot()
    }

    fun device(id: String): CHDevices? = devices[id.uppercase()]

    private fun hubTrace(id: String, event: String) {
        if (BuildConfig.DEBUG) {
            val elapsed = hubWatchStarted[id]?.let { SystemClock.elapsedRealtime() - it } ?: 0
            Log.d("Hub3Timing", "hub=${id.takeLast(8)} watch_ms=$elapsed $event")
        }
    }

    private fun clearHubScan(id: String) {
        hubWifi.remove(id)?.timeout?.cancel()
    }

    private fun clearHubSession(id: String) {
        hubDisconnects.remove(id)?.cancel()
        hubSessions.remove(id)
        hubWatchStarted.remove(id)
        clearHubScan(id)
    }

    private fun finishHubScan(id: String, scan: HubWifiScan, failed: Boolean, reason: String) {
        if (hubWifi[id] !== scan || !scan.scanning) return
        scan.scanning = false
        scan.failed = failed
        scan.timeout?.cancel()
        scan.timeout = null
        hubTrace(id, "wifi_finished reason=$reason count=${scan.results.size} elapsed_ms=${SystemClock.elapsedRealtime() - scan.startedAt}")
        snapshot()
    }

    private fun scanHubWifi(id: String, hub: CHWifiModule2, refresh: Boolean) {
        check(hubSessions.containsKey(id) && hub.deviceStatus.value == CHDeviceLoginStatus.logined)
        if (hubOta[id]?.progress?.let { it in 0..99 } == true) return
        val previous = hubWifi[id]
        if (previous?.scanning == true || previous?.commandPending == true || (!refresh && previous != null && !previous.failed &&
                    SystemClock.elapsedRealtime() - previous.startedAt < 30000)
        ) {
            hubTrace(id, "wifi_reused scanning=${previous?.scanning}")
            return
        }
        val scan = HubWifiScan()
        hubWifi[id] = scan
        hubTrace(id, "wifi_requested refresh=$refresh")
        snapshot()
        // Older firmware may not publish an end marker. Never start a second scan meanwhile.
        scan.timeout = scope.launch(Dispatchers.Main) {
            delay(15000.milliseconds)
            finishHubScan(id, scan, scan.results.isEmpty() || scan.commandPending, "timeout")
            if (scan.commandPending) {
                // Reset an unacknowledged SDK command before allowing another physical scan.
                hubTrace(id, "wifi_ack_timeout_disconnect")
                hub.disconnect {}
            }
        }
        hub.scanWifiSSID { result ->
            scope.launch(Dispatchers.Main) {
                if (hubWifi[id] !== scan) return@launch
                scan.commandPending = false
                hubTrace(id, "wifi_ack success=${result.isSuccess} elapsed_ms=${SystemClock.elapsedRealtime() - scan.startedAt}")
                if (result.isFailure) finishHubScan(id, scan, true, "rejected")
                else if (scan.endReceived) finishHubScan(id, scan, false, "end_marker")
            }
        }
    }

    private fun armHubOtaTimeout(id: String, ota: HubOta) {
        ota.timeout?.cancel()
        ota.timeout = null
        if (ota.progress !in 0..99) return
        ota.timeout = scope.launch(Dispatchers.Main) {
            delay(180000.milliseconds)
            if (hubOta[id] !== ota) return@launch
            ota.progress = -7
            hubTrace(id, "ota_timeout")
            snapshot()
        }
    }

    suspend fun hubSettings(json: JSONObject): JSONObject = withTimeoutOrNull(8000.milliseconds) {
        val id = json.getString("deviceUUID").uppercase()
        val hub = device(id) as? CHWifiModule2 ?: error("Unsupported WiFi device")
        when (json.getString("operation")) {
            "watch" -> {
                hubDisconnects.remove(id)?.cancel()
                hubSessions[id] = json.getString("session")
                hubWatchStarted[id] = SystemClock.elapsedRealtime()
                hubTrace(id, "watch connected=${hub.deviceStatus.value == CHDeviceLoginStatus.logined}")
                connect(hub)
            }

            "unwatch" -> if (hubSessions[id] == json.getString("session")) {
                hubSessions.remove(id)
                hubDisconnects.remove(id)?.cancel()
                hubTrace(id, "unwatch grace_ms=${if (foreground) 10000 else 0}")
                hubDisconnects[id] = scope.launch(Dispatchers.Main) {
                    if (foreground) delay(10000.milliseconds)
                    hubTrace(id, "disconnect_after_unwatch")
                    clearHubSession(id)
                    hub.disconnect {}
                }
            }

            "scan" -> scanHubWifi(id, hub, json.optBoolean("refresh"))
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
                if (hubOta[id]?.progress?.let { it in 0..99 } != true) {
                    // WiFi scanning must not compete with OTA or disconnect it on a scan timeout.
                    clearHubScan(id)
                    val ota = HubOta()
                    hubOta[id] = ota
                    armHubOtaTimeout(id, ota)
                    hubTrace(id, "ota_requested")
                    // Like the original bridge, dispatch the command and follow OTA notifications.
                    // A reboot can drop the command reply while the Hub continues downloading.
                    hub.updateFirmwareBleOnly { result ->
                        scope.launch(Dispatchers.Main) {
                            if (hubOta[id] !== ota || ota.receivedProgress) return@launch
                            if (result.isFailure) {
                                ota.progress = -7
                                armHubOtaTimeout(id, ota)
                                hubTrace(id, "ota_rejected")
                                snapshot()
                            }
                        }
                    }
                }
            }

            "firmwareFinish" -> if (hubOta[id]?.progress == 100) hubOta.remove(id)?.timeout?.cancel()
            "version" -> {
                check(hub.deviceStatus.value == CHDeviceLoginStatus.logined)
                val version = LockSettings.awaitResult(hub::getVersionTag)
                hubTrace(id, "version_read")
                return@withTimeoutOrNull JSONObject().put("version", version)
            }

            else -> error("Unsupported Hub operation")
        }
        snapshot()
        JSONObject()
    } ?: error("Hub operation timed out")

    fun testDevices(): List<CHDevices> = devices.values.toList()
    fun deviceName(device: CHDevices): String = appliedKeys[device.deviceId.toString().uppercase()]?.historyTag?.toString(Charsets.UTF_8) ?: device.productModel.deviceModelName()
    fun historyTag(): ByteArray = history.copyOf()

    fun snapshot() {
        val rows = JSONArray().apply {
            devices.forEach { (id, device) ->
                val connected = device.deviceStatus.value == CHDeviceLoginStatus.logined
                val row = JSONObject().put("deviceUUID", id).put("bleConnected", connected)
                    .put("scanStatus", CHBleManager.mScanning.name).put("connectionCount", CHBleManager.getConnectRSize())
                    .put("bleStatus", device.deviceStatus.name).put("showBle", device is CHSesameLock)
                    .put("position", if (connected && (device is CHSesame2 || device is CHSesame5)) device.mechStatus?.position else JSONObject.NULL)
                    .put("batteryPercentage", if (connected) device.batteryPercentage else JSONObject.NULL)
                row.put("settings", LockSettings.snapshot(device))
                row.put("peripheral", peripheralSettings.snapshot(device))
                if (device is CHWifiModule2) {
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
                    hubWifi[id]?.let { scan ->
                        hub.put("wifiScanning", scan.scanning).put("wifiScanFailed", scan.failed)
                        hub.put("wifiScan", JSONArray().apply {
                            scan.results.forEach { (ssid, rssi) -> put(JSONObject().put("ssid", ssid).put("rssi", rssi.toInt())) }
                        })
                    }
                    hubOta[id]?.let { hub.put("otaProgress", it.progress) }
                    row.put("hub", hub)
                }
                if (device is CHSesameBot2) {
                    row.put("scriptIndex", device.scripts.curIdx.toInt())
                    row.put("scripts", JSONArray().apply {
                        device.scripts.events.forEachIndexed { index, script ->
                            put(JSONObject().put("id", index).put("name", String(script.name, Charsets.UTF_8)))
                        }
                    })
                }
                put(row)
            }
        }
        publish(rows)
        observers.toList().forEach { it(rows) }
    }

    suspend fun drop(id: String) {
        updates.withLock {
            peripheralSettings.forget(id.uppercase())
            clearHubSession(id.uppercase())
            hubOta.remove(id.uppercase())?.timeout?.cancel()
            val device = devices.remove(id.uppercase())
            appliedKeys.remove(id.uppercase())
            if (device != null) {
                device.delegate = null; LockSettings.awaitResult<CHEmpty>(device::dropKey)
            } else suspendCancellableCoroutine<Int> { continuation -> DeviceKeyDatabase.Keys.deleteByDeviceId(id) { if (continuation.isActive) continuation.resumeWith(it) } }
        }
        cloudRows = JSONArray((0 until cloudRows.length()).map { cloudRows.getJSONObject(it) }.filterNot { it.optString("deviceUUID").equals(id, true) })
        snapshot()
    }

    fun handle(action: String, json: JSONObject, reply: (Boolean, String?) -> Unit) {
        val device = devices[json.optString("deviceUUID").uppercase()]
            ?: run { reply(false, "Bluetooth device unavailable"); return }
        if (device.deviceStatus.value != CHDeviceLoginStatus.logined) {
            reply(false, "Bluetooth disconnected"); return
        }
        val result: CHResult<CHEmpty> = { reply(it.isSuccess, if (it.isFailure) "Bluetooth command failed" else null) }
        if (action == "home.scripts") {
            if (device is CHSesameBot2) device.getScriptNameList {
                scope.launch { snapshot(); reply(it.isSuccess, if (it.isFailure) "Unable to read scripts" else null) }
            } else reply(false, "Unsupported device")
            return
        }
        when (device) {
            is CHSesame5 -> when (json.optString("operation")) {
                "lock" -> device.lock(historytag = history, result = result)
                "unlock" -> device.unlock(historytag = history, result = result)
                else -> device.toggle(historytag = history, result = result)
            }

            is CHSesame2 -> when (json.optString("operation")) {
                "lock" -> device.lock(result = result)
                "unlock" -> device.unlock(result = result)
                else -> device.toggle(result = result)
            }

            is CHSesameBike -> device.unlock(result = result)
            is CHSesameBike2 -> device.unlock(historytag = history, result = result)
            is CHSesameBot -> device.click(result = result)
            is CHSesameBot2 -> {
                val cloud = (0 until cloudRows.length()).map { cloudRows.getJSONObject(it) }.firstOrNull { it.optString("deviceUUID").equals(device.deviceId.toString(), true) }
                val scripts = cloud?.optJSONObject("stateInfo")?.optJSONArray("scriptList")
                val defaultIndex =
                    scripts?.let { list -> (0 until list.length()).map { list.getJSONObject(it) }.firstOrNull { it.optInt("isDefault") == 1 }?.optInt("actionIndex") }
                val index = if (json.has("scriptIndex")) json.optInt("scriptIndex", -1) else defaultIndex ?: device.scripts.curIdx.toInt()
                if (index !in 0..9) reply(false, "Invalid script index")
                else device.click(index = index.toUByte(), historytag = history, result = result)
            }

            else -> reply(false, "Unsupported Bluetooth command")
        }
    }
}
