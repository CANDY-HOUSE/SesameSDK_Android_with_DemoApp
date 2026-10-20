package co.candyhouse.app.ble

import co.candyhouse.app.connecteddevice.AutoUnlockGeofenceManager
import co.candyhouse.app.connecteddevice.SesameConnectedDeviceService
import co.candyhouse.app.data.BleBackend
import co.candyhouse.app.data.KeyHandoff
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
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.util.UUID

/** BLE state and commands only; the web app owns lists, ordering and navigation. */
class BleController(
    private val scope: CoroutineScope, private val backend: BleBackend,
    private val publish: (JSONArray) -> Unit
) {
    private val devices = linkedMapOf<String, CHDevices>()
    val peripheralSettings = PeripheralSettings(this, scope)
    val hubSettings = HubSettings(this, scope)
    private val updates = Mutex()
    private val appliedKeys = mutableMapOf<String, CHDevice>()
    var foreground = false
        private set
    private var receivedCloudList = false
    var background = false
    var cloudRows = JSONArray()
        private set
    val observers = mutableSetOf<(JSONArray) -> Unit>()
    fun allDevices(): List<CHDevices> = devices.values.toList()
    suspend fun restoreSaved() = updates.withLock {
        if (devices.isNotEmpty() || receivedCloudList) return@withLock
        val keys = KeyHandoff(DeviceKeyDatabase.context).saved()
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

    // OS2 devices (Sesame 3/4, Bike 1, Bot 1) record a text tag; like the native app it is the account name supplied by Biz.
    private var os2History: ByteArray? = localPreferences.getString("historyName", null)?.takeIf { it.isNotEmpty() }?.toByteArray()
    private val delegate = object : CHHub3Delegate {
        override fun onBleDeviceStatusChanged(device: CHDevices, status: CHDeviceStatus) {
            scope.launch(Dispatchers.Main) {
                if (device is CHWifiModule2) hubSettings.statusChanged(device, status)
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
            scope.launch(Dispatchers.Main) { hubSettings.wifiFound(device, ssid, rssi) }
        }

        override fun onWifiScanStarted(device: CHHub3) {
            scope.launch(Dispatchers.Main) { hubSettings.wifiScanStarted(device) }
        }

        override fun onWifiScanFinished(device: CHHub3) {
            scope.launch(Dispatchers.Main) { hubSettings.wifiScanFinished(device) }
        }

        override fun onOTAProgress(device: CHWifiModule2, percent: Byte) {
            scope.launch(Dispatchers.Main) { hubSettings.otaProgress(device, percent) }
        }

        override fun onMechStatus(device: CHDevices) {
            scope.launch(Dispatchers.Main) { snapshot() }
        }
    }

    fun accept(rows: JSONArray, historyTag: String?, historyName: String? = null): Job {
        val keyHandoff = KeyHandoff(DeviceKeyDatabase.context)
        val revision = keyHandoff.scopeRevision()
        return scope.launch {
            updates.withLock {
                if (revision != keyHandoff.scopeRevision()) return@withLock
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
                keyHandoff.remember(rows)
                // Keep unsent keys on disk; an H5 empty list must not erase legacy guest keys.
                devices.keys.filter { it !in incoming }.forEach { id ->
                    peripheralSettings.forget(id)
                    hubSettings.forget(id)
                    appliedKeys.remove(id)
                    devices.remove(id)?.let {
                        if (it.isRegistered) {
                            it.delegate = null
                            it.disconnect {}
                        }
                    }
                }
                if (!historyTag.isNullOrBlank()) {
                    history = historyBytes(historyTag)
                    localPreferences.edit().putString("environmentId", historyTag).apply()
                } else if (rows.length() == 0) {
                    history = historyBytes(null)
                    localPreferences.edit().remove("environmentId").apply()
                }
                if (!historyName.isNullOrEmpty()) {
                    os2History = historyName.toByteArray()
                    localPreferences.edit().putString("historyName", historyName).apply()
                } else if (rows.length() == 0) {
                    os2History = null
                    localPreferences.edit().remove("historyName").apply()
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

    fun connect(device: CHDevices) {
        if ((foreground || background) && !BleFirmwareUpdate.active(device.deviceId.toString()) && (device is CHSesameLock || (foreground && peripheralSettings.shouldConnect(device)) || (foreground && device is CHWifiModule2 && hubSettings.watching(
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
        hubSettings.endGrace()
    }

    fun close() {
        peripheralSettings.close()
        if (CHBleManager.statusDelegate === scanDelegate) CHBleManager.statusDelegate = null
        background = false
        hubSettings.close()
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

    fun testDevices(): List<CHDevices> = devices.values.toList()
    fun deviceName(device: CHDevices): String = appliedKeys[device.deviceId.toString().uppercase()]?.historyTag?.toString(Charsets.UTF_8) ?: device.productModel.deviceModelName()
    fun historyTag(): ByteArray = history.copyOf()
    fun os2HistoryTag(): ByteArray? = os2History?.copyOf()

    fun snapshot() {
        val rows = JSONArray().apply {
            devices.forEach { (id, device) ->
                val connected = device.deviceStatus.value == CHDeviceLoginStatus.logined
                val row = JSONObject().put("deviceUUID", id).put("bleConnected", connected)
                    .put("bluetoothOff", CHBleManager.mScanning == CHScanStatus.BleClose)
                    .put("bleState", device.deviceStatus.webBleState()).put("showBle", device is CHSesameLock)
                    .put("position", if (connected && (device is CHSesame2 || device is CHSesame5)) device.mechStatus?.position else JSONObject.NULL)
                    .put("batteryPercentage", if (connected) device.batteryPercentage else JSONObject.NULL)
                row.put("settings", LockSettings.snapshot(device))
                row.put("peripheral", peripheralSettings.snapshot(device))
                if (device is CHWifiModule2) row.put("hub", hubSettings.snapshot(id, device, connected))
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
            hubSettings.forget(id.uppercase())
            val device = devices.remove(id.uppercase())
            appliedKeys.remove(id.uppercase())
            if (device != null) {
                device.delegate = null; LockSettings.awaitResult<CHEmpty>(device::dropKey)
            } else suspendCancellableCoroutine<Int> { continuation -> DeviceKeyDatabase.Keys.deleteByDeviceId(id) { if (continuation.isActive) continuation.resumeWith(it) } }
        }
        cloudRows = JSONArray((0 until cloudRows.length()).map { cloudRows.getJSONObject(it) }.filterNot { it.optString("deviceUUID").equals(id, true) })
        snapshot()
    }

    /** Native shortcuts (NFC, widget, notification) run without a Biz page: BLE when logged in, else the cloud gateway. */
    suspend fun quickCommand(id: String, operation: String) {
        val device = requireNotNull(device(id))
        if (device.deviceStatus.value == CHDeviceLoginStatus.logined) suspendCancellableCoroutine { continuation ->
            handle("home.command", JSONObject().put("deviceUUID", id).put("operation", operation)) { ok, error ->
                if (continuation.isActive) continuation.resumeWith(if (ok) Result.success(Unit) else Result.failure(IllegalStateException(error)))
            }
        } else requireNotNull(backend.requestCloud)("widgetCommand", JSONObject().put("operation", operation), id)
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
                "lock" -> device.lock(historytag = os2HistoryTag(), result = result)
                "unlock" -> device.unlock(historytag = os2HistoryTag(), result = result)
                else -> device.toggle(historytag = os2HistoryTag(), result = result)
            }

            is CHSesameBike -> device.unlock(historytag = os2HistoryTag(), result = result)
            is CHSesameBike2 -> device.unlock(historytag = history, result = result)
            is CHSesameBot -> device.click(historyTag = os2HistoryTag(), result = result)
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
