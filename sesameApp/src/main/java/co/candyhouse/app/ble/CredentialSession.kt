package co.candyhouse.app.ble

import co.candyhouse.sesame.ble.CHDeviceLoginStatus
import co.candyhouse.sesame.ble.CHDevices
import co.candyhouse.sesame.utils.CHEmpty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import kotlin.time.Duration.Companion.milliseconds

/** One Biz credential page: device list state and commands; names and cloud sync live in Biz. */
internal class CredentialSession(
    private val device: CHDevices, val kind: String, val token: String,
    private val scope: CoroutineScope, private val onChange: () -> Unit
) : CredentialEvents {
    private val channel = CredentialChannel.of(device, kind)
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
    private var importing = false
    private var importProgress = 0
    private var importRevision = 0
    private var importError: String? = null
    fun snapshot() = JSONObject().put("kind", kind).put("session", token).put("revision", revision)
        .put("complete", complete).put("loading", loading).put("loadFailed", loadFailed).put("mode", mode).put("failureRevision", failureRevision)
        .put("importing", importing).put("importProgress", importProgress).put("importRevision", importRevision).put("importError", importError ?: JSONObject.NULL)
        .put("items", JSONArray(items.values.toList()))

    private var pendingChange: Job? = null

    private fun update(block: () -> Unit) {
        scope.launch(Dispatchers.Main.immediate) {
            if (!closed) {
                block(); scheduleChange()
            }
        }
    }

    /** A device streams hundreds of items within a fraction of a second; one snapshot per 100 ms keeps the page responsive. */
    private fun scheduleChange() {
        if (pendingChange?.isActive == true) return
        pendingChange = scope.launch(Dispatchers.Main) {
            delay(100.milliseconds)
            if (!closed) onChange()
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

    private fun acknowledged() = update {
        // Empty card/passcode reads can ACK without FIRST/LAST. Release the UI, but only
        // LAST confirms a full list for cloud replacement; a later FIRST starts loading again.
        if (!listStarted && !complete) {
            loading = false; loadFailed = false; loadTimer?.cancel()
        }
    }

    override fun started() = update { listStarted = true; loading = true; complete = false; loadFailed = false; items.clear(); waitForList() }
    override fun ended() = update { loading = false; complete = true; loadFailed = false; loadTimer?.cancel(); revision++ }
    override fun received(id: String, name: String, type: Byte, changed: Boolean) = update {
        items[id] = JSONObject().put("credentialId", id).put("rawName", name).put("type", type.toInt() and 255)
        if (changed) revision++ else if (!complete) {
            listStarted = true; loading = true; waitForList()
        }
    }

    override fun deleted(id: String) = update { items.remove(id); revision++ }
    override fun deleteFailed() = update { failureRevision++ }
    override fun modeChanged(mode: Byte) = update { this.mode = mode.toInt() }

    fun open() {
        // Mode reads must not block list requests. Only LAST confirms a full list for cloud sync.
        waitForList()
        detach = channel.attach(this)
        modeRead = scope.launch {
            runCatching { withTimeout(4000.milliseconds) { LockSettings.awaitResult(channel::readMode) } }.onSuccess { modeChanged(it) }
        }
        channel.requestList(::acknowledged, ::listFailed)
    }

    private suspend fun setMode(value: Int) {
        modeRead?.cancel()
        require(value == 1 || value == channel.idleMode.toInt())
        LockSettings.awaitResult<CHEmpty> { channel.setMode(value.toByte(), it) }
        mode = value
    }

    suspend fun command(op: String, json: JSONObject) {
        if (op == "credentialMode") {
            setMode(json.getInt("value")); return
        }
        if (op == "credentialAdd") {
            // Biz encodes the card ID / passcode digits; the device reports the new item as a change.
            LockSettings.awaitResult<CHEmpty> { channel.add(bytes(json.getString("value")), json.getString("name"), it) }
            return
        }
        if (op == "credentialImport") {
            startImport(bytes(json.getString("data"))); return
        }
        val id = json.getString("id"); require(items.containsKey(id))
        if (op == "credentialRename") {
            val name = json.getString("nameUUID").replace("-", "")
            require(name.matches(Regex("[0-9a-fA-F]{32}")))
            LockSettings.awaitResult<CHEmpty> { channel.rename(id, name, it) }
            items[id]?.put("rawName", name)
            revision++
        } else {
            LockSettings.awaitResult<CHEmpty> { channel.delete(id, it) }
            if (!channel.deleteConfirmedByEvent) deleted(id)
        }
    }

    private fun bytes(hex: String): ByteArray {
        require(hex.isNotEmpty() && hex.length % 2 == 0 && hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' })
        return hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    /** The SDK sends 209-byte packets 4 s apart, longer than a bridge request; progress and result go out in the snapshot. */
    private fun startImport(data: ByteArray) {
        check(!importing)
        require(data.size <= Short.MAX_VALUE)
        importing = true; importProgress = 0; importError = null
        channel.batchAdd(data, { current, total -> update { if (importing) importProgress = current * 100 / total } }) { result ->
            update {
                importing = false
                importError = result.exceptionOrNull()?.let { it.message ?: it.javaClass.simpleName }
                if (result.isSuccess) importProgress = 100
                importRevision++
            }
        }
    }

    fun close(): Job? {
        if (closed) return null
        closed = true; loadTimer?.cancel(); modeRead?.cancel(); pendingChange?.cancel(); detach()
        return if (device.deviceStatus.value == CHDeviceLoginStatus.logined) scope.launch {
            runCatching { withTimeout(4000.milliseconds) { setMode(channel.idleMode.toInt()) } }
        } else null
    }
}
