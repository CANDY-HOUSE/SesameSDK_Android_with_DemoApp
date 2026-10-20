package co.candyhouse.app.ble

import androidx.lifecycle.Observer
import co.candyhouse.sesame.ble.CHDeviceLoginStatus
import co.candyhouse.sesame.ble.CHDeviceStatus
import co.candyhouse.sesame.ble.CHDevices
import co.candyhouse.sesame.ble.CHProductModel
import co.candyhouse.sesame.ble.CHSesameConnector
import co.candyhouse.sesame.ble.CHSesameLock
import co.candyhouse.sesame.ble.os3.biometric.CHSesameBiometricDevice
import co.candyhouse.sesame.ble.os3.biometric.capability.fingerPrint.CHFingerPrintCapable
import co.candyhouse.sesame.ble.os3.biometric.capability.remoteNano.CHRemoteNanoCapable
import co.candyhouse.sesame.ble.os3.biometric.capability.remoteNano.CHRemoteNanoDelegate
import co.candyhouse.sesame.ble.os3.biometric.parseData.CHRemoteNanoTriggerSettings
import co.candyhouse.sesame.ble.os3.wm2.CHWifiModule2
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import kotlin.time.Duration.Companion.milliseconds

/** Device settings transport only; model policy, labels, radar conversion and cloud sync live in Biz. */
class PeripheralSettings(private val ble: BleController, private val scope: CoroutineScope) {
    private val sessions = mutableMapOf<String, String>()
    private val departures = mutableMapOf<String, Job>()
    private val setup = mutableSetOf<String>()
    private val cleanup = mutableMapOf<String, () -> Unit>()
    private val radar = mutableMapOf<String, Int>()
    private val bindingWarnings = mutableMapOf<String, JSONObject>()
    private var warningRevision = 0
    private val closing = mutableMapOf<String, Job>()
    private val credentials = mutableMapOf<String, CredentialSession>()
    private val sleepy = setOf(CHProductModel.RemoteNano, CHProductModel.SSMOpenSensor)

    fun registered(id: String) {
        setup.add(id.uppercase())
    }

    fun statusChanged(device: CHDevices, status: CHDeviceStatus) {
        if (status.value != CHDeviceLoginStatus.logined) closeCredentials(device.deviceId.toString().uppercase())
        if (device.productModel in sleepy && status.value != CHDeviceLoginStatus.logined && status != CHDeviceStatus.ReadyToRegister) {
            setup.remove(device.deviceId.toString().uppercase())
        }
    }

    fun shouldConnect(device: CHDevices): Boolean {
        val id = device.deviceId.toString().uppercase()
        return sessions.containsKey(id) && (device.productModel !in sleepy || id in setup)
    }

    fun snapshot(device: CHDevices): JSONObject {
        val id = device.deviceId.toString().uppercase()
        return JSONObject().apply {
            put("setup", id in setup)
            put("sleeping", device.productModel in sleepy && id !in setup)
            put("capabilities", JSONArray(capabilities(device)))
            val keys = when (device) {
                is CHSesameConnector -> device.ssm2KeysMap.keys
                is CHWifiModule2 -> device.ssm2KeysMap.keys
                else -> emptySet()
            }
            put("bound", JSONArray(keys.toList()))
            radar[id]?.let { put("radar", it) }
            bindingWarnings[id]?.let { put("bindingWarning", it) }
            (device as? CHSesameBiometricDevice)?.triggerDelaySetting?.let { put("triggerDelay", it.triggerDelaySecond.toInt()) }
            credentials[id]?.let { put("credentials", it.snapshot()) }
        }
    }

    private fun capabilities(device: CHDevices): List<String> = when (device) {
        is CHSesameBiometricDevice -> device.supportedCapabilities().map { it.name.lowercase() }
        is CHFingerPrintCapable -> listOf("fingerprint")
        else -> emptyList()
    }

    private fun observe(id: String, device: CHDevices) {
        if (cleanup.containsKey(id) || device !is CHSesameBiometricDevice) return
        val keys = device.getSSM2KeysLiveData()
        val radarData = device.getRadarReceiveLiveData()
        val slotFullData = device.getSSM2SlotFullLiveData()
        val supportData = device.getSSM2SupportLiveDataLiveData()
        fun warn(code: String) {
            bindingWarnings[id] = JSONObject().put("code", code).put("revision", ++warningRevision)
            ble.snapshot()
        }

        val slotObserver = Observer<Event<Boolean>> { if (it.getContentIfNotHandled() == true) warn("deviceFull") }
        val supportObserver = Observer<Event<Boolean>> { if (it.getContentIfNotHandled() == false) warn("deviceUnsupported") }
        val keyObserver = Observer<Map<String, ByteArray>> { ble.snapshot() }
        val radarObserver = Observer<Event<ByteArray>> { event ->
            val data = event.peekContent()
            if (data.size >= 5) {
                radar[id] = (1..4).fold(0) { value, i -> value or ((data[i].toInt() and 255) shl ((i - 1) * 8)) }
                ble.snapshot()
            }
        }
        keys?.observeForever(keyObserver); radarData?.observeForever(radarObserver)
        slotFullData?.observeForever(slotObserver); supportData?.observeForever(supportObserver)
        val nano = device as? CHRemoteNanoCapable
        val nanoDelegate = object : CHRemoteNanoDelegate {
            override fun onTriggerDelaySecondReceived(device: CHSesameConnector, setting: CHRemoteNanoTriggerSettings) {
                scope.launch(Dispatchers.Main) { ble.snapshot() }
            }
        }
        nano?.registerEventDelegate(device, nanoDelegate)
        cleanup[id] = {
            keys?.removeObserver(keyObserver); radarData?.removeObserver(radarObserver)
            slotFullData?.removeObserver(slotObserver); supportData?.removeObserver(supportObserver)
            nano?.unregisterEventDelegate(nanoDelegate)
        }
    }

    fun forget(id: String) {
        departures.remove(id)?.cancel(); sessions.remove(id); setup.remove(id)
        closeCredentials(id); cleanup.remove(id)?.invoke(); radar.remove(id)
        bindingWarnings.remove(id)
    }

    private fun closeCredentials(id: String) {
        credentials.remove(id)?.close()?.let { closing[id] = it }
    }

    fun close() {
        (sessions.keys + setup + credentials.keys).toSet().forEach { id ->
            forget(id)
            val device = ble.device(id)
            scope.launch {
                closing[id]?.join()
                if (!sessions.containsKey(id) && device !is CHSesameLock) device?.disconnect {}
            }
        }
    }

    suspend fun handle(json: JSONObject): JSONObject = withTimeout(10000.milliseconds) {
        val id = json.getString("deviceUUID").uppercase()
        val device = requireNotNull(ble.device(id))
        val op = json.getString("operation")
        if (op == "watch") {
            departures.remove(id)?.cancel()
            sessions[id] = json.getString("session")
            observe(id, device)
            if (closing[id]?.isCompleted == true) closing.remove(id)
            if (shouldConnect(device) && device.deviceStatus == CHDeviceStatus.ReceivedAdV) device.connect {}
        } else if (op == "unwatch") {
            if (sessions[id] == json.getString("session")) {
                departures.remove(id)?.cancel()
                // Allow navigation between the setting and its child pages without losing the registration connection.
                departures[id] = scope.launch {
                    delay(600.milliseconds)
                    closeCredentials(id)
                    closing[id]?.join()
                    forget(id)
                    if (device !is CHSesameLock) device.disconnect {}
                    ble.snapshot()
                }
            }
        } else if (op == "credentialsClose") {
            if (credentials[id]?.token == json.getString("session")) closeCredentials(id)
        } else {
            check(device.deviceStatus.value == CHDeviceLoginStatus.logined)
            check(device.productModel !in sleepy || id in setup)
            when (op) {
                "version" -> return@withTimeout JSONObject().put("version", LockSettings.awaitResult(device::getVersionTag))
                "txPower" -> {
                    val value = json.getInt("value"); require(value in -4..20)
                    require(device.bleTxPower.toInt() != CHDevices.UNSET_BLE_TX_POWER_VALUE)
                    LockSettings.awaitResult<CHEmpty> { device.setBleTxPower(value.toByte(), it) }
                }

                "radar" -> {
                    val command = json.getInt("command");
                    val value = json.getInt("value")
                    require(command in setOf(0x33, 0x53) && value in 0..512)
                    val payload = byteArrayOf(command.toByte()) + ByteArray(4) { (value shr (8 * it)).toByte() }
                    LockSettings.awaitResult<CHEmpty> { (device as CHSesameConnector).setRadarSensitivity(payload, it) }
                    radar[id] = value
                }

                "triggerDelay" -> {
                    require(device.productModel == CHProductModel.RemoteNano)
                    val value = json.getInt("value"); require(value in setOf(0, 10))
                    LockSettings.awaitResult<CHEmpty> { (device as CHRemoteNanoCapable).setTriggerDelayTime(value.toUByte(), it) }
                }

                "bind", "unbind" -> {
                    val target = json.getString("target")
                    if (op == "bind") {
                        val lock = requireNotNull(ble.device(target))
                        LockSettings.awaitResult<CHEmpty> { cb ->
                            when (device) {
                                is CHWifiModule2 -> device.insertSesames(lock, cb)
                                is CHSesameConnector -> device.insertSesame(lock, cb)
                                else -> error("Unsupported binding")
                            }
                        }
                    } else LockSettings.awaitResult<CHEmpty> { cb ->
                        when (device) {
                            is CHWifiModule2 -> device.removeSesame(target, cb)
                            is CHSesameConnector -> device.removeSesame(target, cb)
                            else -> error("Unsupported binding")
                        }
                    }
                }

                "credentials" -> {
                    val kind = json.getString("kind")
                    require(kind in capabilities(device))
                    closeCredentials(id)
                    closing.remove(id)?.join()
                    val session = CredentialSession(device, kind, json.getString("session"), scope, ble::snapshot)
                    credentials[id] = session
                    try {
                        session.open()
                    } catch (error: Throwable) {
                        if (credentials[id] === session) closeCredentials(id)
                        throw error
                    }
                }

                "credentialMode", "credentialRename", "credentialDelete", "credentialAdd", "credentialImport" -> {
                    val session = requireNotNull(credentials[id])
                    require(session.token == json.getString("session"))
                    session.command(op, json)
                }

                else -> error("Unsupported peripheral operation")
            }
        }
        ble.snapshot()
        snapshot(device)
    }
}
