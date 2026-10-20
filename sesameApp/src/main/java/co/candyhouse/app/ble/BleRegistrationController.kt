package co.candyhouse.app.ble

import android.os.SystemClock
import co.candyhouse.sesame.ble.CHBleManager
import co.candyhouse.sesame.ble.CHBleManagerDelegate
import co.candyhouse.sesame.ble.CHBleStatusDelegate
import co.candyhouse.sesame.ble.CHDeviceLoginStatus
import co.candyhouse.sesame.ble.CHDeviceStatus
import co.candyhouse.sesame.ble.CHDeviceStatusDelegate
import co.candyhouse.sesame.ble.CHDevices
import co.candyhouse.sesame.ble.CHProductModel
import co.candyhouse.sesame.ble.CHScanStatus
import co.candyhouse.sesame.ble.os2.sesame2.CHSesame2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

/** Discovery and registration only; all rows, progress and errors are rendered by Biz. */
class BleRegistrationController(
    private val scope: CoroutineScope,
    private val publish: (JSONObject) -> Unit,
    private val historyTag: () -> ByteArray,
    private val registered: (CHDevices) -> Unit
) {
    private val devices = linkedMapOf<String, CHDevices>()
    private var active = false
    private var sessionStartedAt = 0L

    /** Biz 传入的已删除但未复位设备，设备端仍为已注册时也列出，供 Biz 置灰显示。 */
    var removedDevices: Set<String> = emptySet()
    private var previousScanDelegate: CHBleStatusDelegate? = null
    private var registering: CHDevices? = null
    private var previousDelegate: CHDeviceStatusDelegate? = null
    private var complete: ((Boolean, String?) -> Unit)? = null
    private var timeout: Job? = null
    private val scanner = object : CHBleManagerDelegate {
        override fun didDiscoverUnRegisteredCHDevices(found: List<CHDevices>) {
            scope.launch(Dispatchers.Main) {
                if (!active) return@launch
                devices.clear()
                // Face3 只能由 Hub3 等设备经 BLE 添加；手机 App 只能扫 Face3 屏幕上的二维码添加
                val removed = removedDevices.mapNotNull { id -> runCatching { UUID.fromString(id) }.getOrNull()?.let(CHBleManager::scannedDevice) }.filter { it.isRegistered }
                (found + removed).filter { it.deviceId != null && it.rssi != null && it.productModel != CHProductModel.SSMFace3 && seenThisSession(it) }
                    .forEach { devices[it.deviceId.toString().uppercase()] = it }
                if (registering == null) {
                    devices.values.filter { !it.isRegistered }.maxByOrNull { it.rssi ?: Int.MIN_VALUE }?.let { nearest ->
                        if (nearest.deviceStatus == CHDeviceStatus.ReceivedAdV) nearest.connect { }
                    }
                }
                publishDevices()
            }
        }
    }
    private val status = object : CHBleStatusDelegate {
        override fun didScanChange(ss: CHScanStatus) {
            previousScanDelegate?.didScanChange(ss)
            scope.launch(Dispatchers.Main) { if (active) publishDevices() }
        }
    }

    // 只列出本次 scanStart 之后收到过广播的设备；已连接的设备可能停止广播，按连接状态保留
    private fun seenThisSession(device: CHDevices): Boolean =
        device.deviceStatus in connectedStatuses || (CHBleManager.lastAdvertisedAt(device.deviceId) ?: 0L) >= sessionStartedAt

    fun start() {
        if (registering != null) return
        val restarting = active
        sessionStartedAt = SystemClock.elapsedRealtime()
        if (!restarting) previousScanDelegate = CHBleManager.statusDelegate
        active = true
        CHBleManager.delegate = scanner
        CHBleManager.statusDelegate = status
        if (restarting) CHBleManager.disableScan { CHBleManager.enableScan { } }
        else CHBleManager.enableScan { }
        publishDevices()
    }

    private fun publishDevices() {
        publish(JSONObject().put("bluetoothOff", CHBleManager.mScanning == CHScanStatus.BleClose).put("devices", JSONArray().apply {
            devices.values.sortedByDescending { it.rssi }.forEach { device ->
                put(
                    JSONObject().put("deviceUUID", device.deviceId.toString().uppercase())
                        .put("name", device.productModel.deviceModelName()).put("rssi", device.rssi)
                        .put("bleState", device.deviceStatus.webBleState()).put("registered", device.isRegistered)
                )
            }
        }))
    }

    fun register(id: String, reply: (Boolean, String?) -> Unit) {
        if (CHBleManager.mScanning == CHScanStatus.BleClose) {
            reply(false, "Bluetooth disconnected"); return
        }
        if (registering != null) {
            reply(false, "Registration in progress"); return
        }
        val device = devices[id.uppercase()]?.takeIf { !it.isRegistered } ?: run { reply(false, "Device no longer available; scan again"); return }
        registering = device
        complete = reply
        previousDelegate = device.delegate
        var started = false
        fun attempt() {
            if (started || device.deviceStatus != CHDeviceStatus.ReadyToRegister || registering !== device) return
            if (CHBleManager.mScanning == CHScanStatus.BleClose) {
                finish(false, "Bluetooth disconnected"); return
            }
            started = true
            device.register { result ->
                result.onSuccess { registered(device) }
                scope.launch(Dispatchers.Main) {
                    result.onSuccess {
                        device.setHistoryTag(historyTag()) { }
                        if (device is CHSesame2) device.configureLockPosition(0, 90) { }
                        if (registering === device) finish(true, null)
                    }.onFailure { if (registering === device) finish(false, "Registration failed") }
                }
            }
        }
        device.delegate = object : CHDeviceStatusDelegate {
            override fun onBleDeviceStatusChanged(device: CHDevices, status: CHDeviceStatus) {
                scope.launch(Dispatchers.Main) { attempt(); if (active) publishDevices() }
            }
        }
        timeout = scope.launch { delay(60000.milliseconds); finish(false, "Registration timed out; check the device list before retrying") }
        attempt()
        if (!started) device.connect { result ->
            scope.launch(Dispatchers.Main) { if (result.isFailure) finish(false, "Bluetooth connection failed") else attempt() }
        }
    }

    private fun finish(success: Boolean, error: String?) {
        timeout?.cancel()
        registering?.delegate = previousDelegate
        registering = null
        previousDelegate = null
        val callback = complete
        complete = null
        callback?.invoke(success, error)
    }

    fun stop() {
        active = false
        if (CHBleManager.delegate === scanner) CHBleManager.delegate = null
        if (CHBleManager.statusDelegate === status) {
            CHBleManager.statusDelegate = previousScanDelegate
            previousScanDelegate?.didScanChange(CHBleManager.mScanning)
        }
        previousScanDelegate = null
        devices.values.filter { !it.isRegistered && it.deviceStatus.value != CHDeviceLoginStatus.logined && it !== registering }.forEach { it.disconnect { } }
        devices.clear()
    }

    fun close() {
        stop(); finish(false, "Registration page closed")
    }

    private companion object {
        val connectedStatuses = setOf(
            CHDeviceStatus.BleConnecting, CHDeviceStatus.WaitingGatt, CHDeviceStatus.DiscoverServices, CHDeviceStatus.BleLogining,
            CHDeviceStatus.ReadyToRegister, CHDeviceStatus.WaitingForAuth, CHDeviceStatus.Registering
        )
    }
}
