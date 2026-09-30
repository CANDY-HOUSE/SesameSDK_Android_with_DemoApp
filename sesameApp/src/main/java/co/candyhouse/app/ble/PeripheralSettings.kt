package co.candyhouse.app.ble

import androidx.lifecycle.Observer
import co.candyhouse.sesame.ble.CHDeviceLoginStatus
import co.candyhouse.sesame.ble.CHDeviceStatus
import co.candyhouse.sesame.ble.CHDevices
import co.candyhouse.sesame.ble.CHProductModel
import co.candyhouse.sesame.ble.CHSesameConnector
import co.candyhouse.sesame.ble.CHSesameLock
import co.candyhouse.sesame.ble.os3.biometric.CHSesameBiometricDevice
import co.candyhouse.sesame.ble.os3.biometric.capability.baseCapbale.CHCapabilityHost
import co.candyhouse.sesame.ble.os3.biometric.capability.card.CHCardCapable
import co.candyhouse.sesame.ble.os3.biometric.capability.card.CHCardDelegate
import co.candyhouse.sesame.ble.os3.biometric.capability.face.CHFaceCapable
import co.candyhouse.sesame.ble.os3.biometric.capability.face.CHFaceDelegate
import co.candyhouse.sesame.ble.os3.biometric.capability.fingerPrint.CHFingerPrintCapable
import co.candyhouse.sesame.ble.os3.biometric.capability.fingerPrint.CHFingerPrintDelegate
import co.candyhouse.sesame.ble.os3.biometric.capability.palm.CHPalmCapable
import co.candyhouse.sesame.ble.os3.biometric.capability.palm.CHPalmDelegate
import co.candyhouse.sesame.ble.os3.biometric.capability.passcode.CHPassCodeCapable
import co.candyhouse.sesame.ble.os3.biometric.capability.passcode.CHPassCodeDelegate
import co.candyhouse.sesame.ble.os3.biometric.capability.remoteNano.CHRemoteNanoCapable
import co.candyhouse.sesame.ble.os3.biometric.capability.remoteNano.CHRemoteNanoDelegate
import co.candyhouse.sesame.ble.os3.biometric.parseData.CHRemoteNanoTriggerSettings
import co.candyhouse.sesame.ble.os3.biometric.parseData.CHSesameTouchFace
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
                    val session = CredentialSession(device, kind, json.getString("session"))
                    credentials[id] = session
                    try {
                        session.open()
                    } catch (error: Throwable) {
                        if (credentials[id] === session) closeCredentials(id)
                        throw error
                    }
                }

                "credentialMode", "credentialRename", "credentialDelete" -> {
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

    private inner class CredentialSession(val device: CHDevices, val kind: String, val token: String) {
        private val host = device as CHCapabilityHost
        private val items = linkedMapOf<String, JSONObject>()
        private var revision = 0
        private var failureRevision = 0
        private var complete = false
        private var loading = true
        private var listStarted = false
        private var mode = 0
        private var closed = false
        private var loadFailed = false
        private var loadTimer: Job? = null
        private var modeRead: Job? = null
        private var detach: () -> Unit = {}
        fun snapshot() = JSONObject().put("kind", kind).put("session", token).put("revision", revision)
            .put("complete", complete).put("loading", loading).put("loadFailed", loadFailed).put("mode", mode).put("failureRevision", failureRevision)
            .put("items", JSONArray(items.values.toList()))

        private fun update(block: () -> Unit) {
            scope.launch(Dispatchers.Main.immediate) {
                if (!closed) {
                    block(); ble.snapshot()
                }
            }
        }

        private fun waitForList() {
            loadTimer?.cancel()
            loadTimer = scope.launch {
                delay(30000.milliseconds)
                listFailed()
            }
        }

        private fun listFailed() = update {
            if (!complete) {
                loading = false; loadFailed = true; loadTimer?.cancel()
            }
        }

        private fun start() = update { listStarted = true; loading = true; complete = false; loadFailed = false; items.clear(); waitForList() }
        private fun end() = update { loading = false; complete = true; loadFailed = false; loadTimer?.cancel(); revision++ }
        private fun acknowledged() = update {
            // Empty card/passcode reads can ACK without FIRST/LAST. Release the UI, but only
            // LAST confirms a full list for cloud replacement; a later FIRST starts loading again.
            if (!listStarted && !complete) {
                loading = false; loadFailed = false; loadTimer?.cancel()
            }
        }

        private fun readMode(read: suspend () -> Byte) {
            modeRead = scope.launch {
                runCatching { withTimeout(4000.milliseconds) { read() } }.onSuccess { changedMode(it) }
            }
        }

        private fun receive(id: String, name: String, type: Byte, changed: Boolean = false) = update {
            items[id] = JSONObject().put("credentialId", id).put("rawName", name).put("type", type.toInt() and 255)
            if (changed) revision++ else if (!complete) {
                listStarted = true; loading = true; waitForList()
            }
        }

        private fun deleted(id: String) = update { items.remove(id); revision++ }
        private fun failed() = update { failureRevision++ }
        private fun changedMode(value: Byte) = update { mode = value.toInt() }
        suspend fun open() {
            // Mode reads must not block list requests. Only LAST confirms a full list for cloud sync.
            waitForList()
            when (kind) {
                "card" -> {
                    val capable = device as CHCardCapable
                    val listener = object : CHCardDelegate {
                        override fun onCardReceive(device: CHDevices, cardID: String, hexName: String, type: Byte) {
                            receive(cardID, hexName, type)
                        }

                        override fun onCardChanged(device: CHDevices, cardID: String, hexName: String, type: Byte) {
                            receive(cardID, hexName, type, true)
                        }

                        override fun onCardReceiveStart(device: CHDevices) {
                            start()
                        }

                        override fun onCardReceiveEnd(device: CHDevices) {
                            end()
                        }

                        override fun onCardModeChanged(device: CHDevices, mode: Byte) {
                            changedMode(mode)
                        }

                        override fun onCardDelete(device: CHDevices, cardID: String) {
                            deleted(cardID)
                        }
                    }
                    capable.registerEventDelegate(host, listener)
                    detach = { capable.unregisterEventDelegate(host, listener) }
                    readMode { LockSettings.awaitResult(capable::cardModeGet) }
                    capable.sendNfcCardsDataGetCmd(device.deviceId.toString().uppercase()) { result ->
                        result.onSuccess { acknowledged() }.onFailure { listFailed() }
                    }
                }

                "fingerprint" -> {
                    val capable = device as CHFingerPrintCapable
                    val listener = object : CHFingerPrintDelegate {
                        override fun onFingerPrintReceive(device: CHDevices, ID: String, hexName: String, type: Byte) {
                            receive(ID, hexName, type)
                        }

                        override fun onFingerPrintChanged(device: CHDevices, ID: String, hexName: String, type: Byte) {
                            receive(ID, hexName, type, true)
                        }

                        override fun onFingerPrintReceiveStart(device: CHDevices) {
                            start()
                        }

                        override fun onFingerPrintReceiveEnd(device: CHDevices) {
                            end()
                        }

                        override fun onFingerModeChange(device: CHDevices, mode: Byte) {
                            changedMode(mode)
                        }

                        override fun onFingerDelete(device: CHDevices, ID: String) {
                            deleted(ID)
                        }
                    }
                    capable.registerEventDelegate(host, listener)
                    detach = { capable.unregisterEventDelegate(host, listener) }
                    readMode { LockSettings.awaitResult(capable::fingerPrintModeGet) }
                    capable.fingerPrints { result -> result.onFailure { listFailed() } }
                }

                "passcode" -> {
                    val capable = device as CHPassCodeCapable
                    val listener = object : CHPassCodeDelegate {
                        override fun onKeyBoardReceive(device: CHDevices, ID: String, hexName: String, type: Byte) {
                            receive(ID, hexName, type)
                        }

                        override fun onKeyBoardChanged(device: CHDevices, ID: String, hexName: String, type: Byte) {
                            receive(ID, hexName, type, true)
                        }

                        override fun onKeyBoardReceiveStart(device: CHDevices) {
                            start()
                        }

                        override fun onKeyBoardReceiveEnd(device: CHDevices) {
                            end()
                        }

                        override fun onKeyBoardModeChange(device: CHDevices, mode: Byte) {
                            changedMode(mode)
                        }

                        override fun onKeyBoardDelete(device: CHDevices, ID: String) {
                            deleted(ID)
                        }
                    }
                    capable.registerEventDelegate(host, listener)
                    detach = { capable.unregisterEventDelegate(host, listener) }
                    readMode { LockSettings.awaitResult(capable::keyBoardPassCodeModeGet) }
                    capable.sendKeyBoardPassCodeDataGetCmd { result ->
                        result.onSuccess { acknowledged() }.onFailure { listFailed() }
                    }
                }

                "face" -> {
                    val capable = device as CHFaceCapable
                    val listener = object : CHFaceDelegate {
                        override fun onFaceReceive(device: CHDevices, face: CHSesameTouchFace) {
                            val item = face; receive(item.id, item.nameUUID, item.type)
                        }

                        override fun onFaceChanged(device: CHDevices, face: CHSesameTouchFace) {
                            val item = face; receive(item.id, item.nameUUID, item.type, true)
                        }

                        override fun onFaceReceiveStart(device: CHDevices) {
                            start()
                        }

                        override fun onFaceReceiveEnd(device: CHDevices) {
                            end()
                        }

                        override fun onFaceModeChanged(device: CHDevices, mode: Byte) {
                            changedMode(mode)
                        }

                        override fun onFaceDeleted(device: CHDevices, faceID: Byte, isSuccess: Boolean) {
                            if (isSuccess) deleted("%02x".format(faceID.toInt() and 255)) else failed()
                        }
                    }
                    capable.registerEventDelegate(host, listener)
                    detach = { capable.unregisterEventDelegate(host, listener) }
                    readMode { LockSettings.awaitResult(capable::faceModeGet) }
                    capable.faceListGet { result -> result.onFailure { listFailed() } }
                }

                "palm" -> {
                    val capable = device as CHPalmCapable
                    val listener = object : CHPalmDelegate {
                        override fun onPalmReceive(device: CHDevices, tochface: CHSesameTouchFace) {
                            val item = tochface; receive(item.id, item.nameUUID, item.type)
                        }

                        override fun onPalmChanged(device: CHDevices, tochface: CHSesameTouchFace) {
                            val item = tochface; receive(item.id, item.nameUUID, item.type, true)
                        }

                        override fun onPalmReceiveStart(device: CHDevices) {
                            start()
                        }

                        override fun onPalmReceiveEnd(device: CHDevices) {
                            end()
                        }

                        override fun onPalmModeChanged(device: CHDevices, mode: Byte) {
                            changedMode(mode)
                        }

                        override fun onPalmDeleted(device: CHDevices, palmID: Byte, isSuccess: Boolean) {
                            if (isSuccess) deleted("%02x".format(palmID.toInt() and 255)) else failed()
                        }
                    }
                    capable.registerEventDelegate(host, listener)
                    detach = { capable.unregisterEventDelegate(host, listener) }
                    readMode { LockSettings.awaitResult(capable::palmModeGet) }
                    capable.palmListGet { result -> result.onFailure { listFailed() } }
                }
            }
        }

        private suspend fun setMode(value: Int) {
            modeRead?.cancel()
            require(value == 1 || value == if (kind == "fingerprint") 2 else 0)
            LockSettings.awaitResult<CHEmpty> { cb ->
                when (kind) {
                    "card" -> (device as CHCardCapable).cardModeSet(value.toByte(), cb)
                    "fingerprint" -> (device as CHFingerPrintCapable).fingerPrintModeSet(value.toByte(), cb)
                    "passcode" -> (device as CHPassCodeCapable).keyBoardPassCodeModeSet(value.toByte(), cb)
                    "face" -> (device as CHFaceCapable).faceModeSet(value.toByte(), cb)
                    "palm" -> (device as CHPalmCapable).palmModeSet(value.toByte(), cb)
                }
            }
            mode = value
        }

        suspend fun command(op: String, json: JSONObject) {
            if (op == "credentialMode") {
                setMode(json.getInt("value")); return
            }
            val id = json.getString("id"); require(items.containsKey(id))
            if (op == "credentialRename") {
                val name = json.getString("nameUUID").replace("-", "")
                require(name.matches(Regex("[0-9a-fA-F]{32}")))
                LockSettings.awaitResult<CHEmpty> { cb ->
                    when (kind) {
                        "card" -> (device as CHCardCapable).cardChange(id, name, cb)
                        "fingerprint" -> (device as CHFingerPrintCapable).fingerPrintsChange(id, name, cb)
                        "passcode" -> (device as CHPassCodeCapable).keyBoardPassCodeChange(id, name, cb)
                        else -> error("Name is managed in Biz")
                    }
                }
                items[id]?.put("rawName", name)
                revision++
            } else {
                LockSettings.awaitResult<CHEmpty> { cb ->
                    when (kind) {
                        "card" -> (device as CHCardCapable).cardDelete(id, cb)
                        "fingerprint" -> (device as CHFingerPrintCapable).fingerPrintDelete(id, device.deviceId.toString(), cb)
                        "passcode" -> (device as CHPassCodeCapable).keyBoardPassCodeDelete(id, device.deviceId.toString(), cb)
                        "face" -> (device as CHFaceCapable).faceDelete(id, device.deviceId.toString(), cb)
                        "palm" -> (device as CHPalmCapable).palmDelete(id, device.deviceId.toString(), cb)
                    }
                }
                // Face/palm deletion completes on its device notification, not the initial command ACK.
                if (kind !in setOf("face", "palm")) deleted(id)
            }
        }

        fun close(): Job? {
            if (closed) return null
            closed = true; loadTimer?.cancel(); modeRead?.cancel(); detach()
            return if (device.deviceStatus.value == CHDeviceLoginStatus.logined) scope.launch {
                runCatching { withTimeout(4000.milliseconds) { setMode(if (kind == "fingerprint") 2 else 0) } }
            } else null
        }
    }
}
