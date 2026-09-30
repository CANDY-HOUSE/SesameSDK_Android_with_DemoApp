package co.candyhouse.app.ble

import android.app.Activity
import android.content.Context
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.SesameActivity
import co.candyhouse.sesame.ble.CHDeviceLoginStatus
import co.candyhouse.sesame.ble.CHDevices
import co.candyhouse.sesame.utils.L
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.nordicsemi.android.dfu.DfuBaseService
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter
import no.nordicsemi.android.dfu.DfuServiceInitiator
import no.nordicsemi.android.dfu.DfuServiceListenerHelper
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipFile
import javax.net.ssl.HttpsURLConnection

class FirmwareDfuService : DfuBaseService() {
    override fun getNotificationTarget(): Class<out Activity> = SesameActivity::class.java
    override fun isDebug() = BuildConfig.DEBUG
}

/** One application-scoped DFU session, surviving page navigation and Activity recreation. */
object BleFirmwareUpdate {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val cachedFiles = mutableMapOf<String, File>()
    private var sessionId = 0L
    private var deviceId: String? = null
    private var progress = 0
    private var state = "idle"
    fun active(id: String) = deviceId == id.uppercase() && state in setOf("downloading", "updating")
    fun snapshot() = JSONObject().put("deviceUUID", deviceId).put("state", state).put("progress", progress).put("sessionId", sessionId)

    fun start(context: Context, device: CHDevices, url: String, reconnect: () -> Unit): JSONObject {
        check(state !in setOf("downloading", "updating"))
        check(device.deviceStatus.value == CHDeviceLoginStatus.logined)
        val source = URL(url)
        require(source.protocol == "https" && source.host == "firmware.candyhouse.co" && source.port == -1 && source.userInfo == null)
        require(source.path.endsWith(".zip") && (!source.path.removePrefix("/").contains("/") || source.path.startsWith("/dev/")))
        val app = context.applicationContext
        sessionId = System.currentTimeMillis()
        deviceId = device.deviceId.toString().uppercase(); progress = 0; state = "downloading"
        L.d("SesameFirmware", "session=$sessionId transport=ble start device=$deviceId")
        val currentSession = sessionId
        scope.launch {
            val key = MessageDigest.getInstance("SHA-256").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }
            val file = File(app.cacheDir, "firmware-runtime-$key.zip")
            val cached = cachedFiles[url]?.let { it.exists() && it.length() > 0 } == true
            L.d("SesameFirmware", "session=$sessionId cache=${if (cached) "hit" else "miss"}")
            var listener: DfuProgressListenerAdapter? = null
            try {
                if (!cached) withContext(Dispatchers.IO) {
                    val connection = source.openConnection() as HttpsURLConnection
                    connection.connectTimeout = 15000; connection.readTimeout = 15000; connection.instanceFollowRedirects = false
                    try {
                        check(connection.responseCode == 200)
                        connection.inputStream.use { input ->
                            file.outputStream().use { output ->
                                val buffer = ByteArray(8192);
                                var total = 0
                                while (true) {
                                    val size = input.read(buffer); if (size < 0) break; total += size; check(total <= 8 * 1024 * 1024); output.write(buffer, 0, size)
                                }
                            }
                        }
                        ZipFile(file).use { check(it.getEntry("manifest.json") != null) }
                    } finally {
                        connection.disconnect()
                    }
                }
                cachedFiles[url] = file
                check(device.deviceStatus.value == CHDeviceLoginStatus.logined)
                val target = LockSettings.awaitResult(device::updateFirmware)
                listener = object : DfuProgressListenerAdapter() {
                    private fun stage(value: Int) {
                        if (state != "updating" || currentSession != sessionId) return
                        progress = value
                        L.d("SesameFirmware", "session=$sessionId transport=ble stage=$value")
                    }

                    override fun onDeviceConnecting(address: String) {
                        stage(-1)
                    }

                    override fun onDeviceConnected(address: String) {
                        stage(-8)
                    }

                    override fun onDfuProcessStarting(address: String) {
                        stage(-2)
                    }

                    override fun onDfuProcessStarted(address: String) {
                        stage(-9)
                    }

                    override fun onEnablingDfuMode(address: String) {
                        stage(-3)
                    }

                    override fun onFirmwareValidating(address: String) {
                        stage(-4)
                    }

                    override fun onDeviceDisconnecting(address: String) {
                        stage(-5)
                    }

                    override fun onDeviceDisconnected(address: String) {
                        stage(-10)
                    }

                    override fun onProgressChanged(address: String, percent: Int, speed: Float, avgSpeed: Float, currentPart: Int, partsTotal: Int) {
                        if (state != "updating" || currentSession != sessionId) return
                        val total = ((currentPart - 1) * 100 + percent) / partsTotal.coerceAtLeast(1)
                        if (total / 10 != progress / 10) L.d("SesameFirmware", "session=$sessionId transport=ble progress=$total part=$currentPart/$partsTotal")
                        progress = total
                    }

                    override fun onDfuCompleted(address: String) {
                        finish("completed")
                    }

                    override fun onDfuAborted(address: String) {
                        finish("failed")
                    }

                    override fun onError(address: String, error: Int, errorType: Int, message: String?) {
                        L.e("BleFirmwareUpdate", "DFU failed: code=$error type=$errorType"); finish("failed")
                    }

                    private fun finish(result: String) {
                        if (state != "updating" || currentSession != sessionId) return
                        state = result
                        progress = if (result == "completed") -6 else -7
                        L.d("SesameFirmware", "session=$sessionId transport=ble state=$result")
                        DfuServiceListenerHelper.unregisterProgressListener(app, this)
                        reconnect()
                    }
                }
                DfuServiceListenerHelper.registerProgressListener(app, listener!!, target.address)
                state = "updating"
                DfuServiceInitiator(target.address).setZip(file.absolutePath)
                    .setPacketsReceiptNotificationsEnabled(true).setPrepareDataObjectDelay(400)
                    .setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true)
                    .setDisableNotification(false).setForeground(false)
                    .start(app, FirmwareDfuService::class.java)
            } catch (error: Exception) {
                state = "failed"
                if (cachedFiles[url] == null) file.delete()
                L.d("SesameFirmware", "session=$sessionId transport=ble state=failed")
                reconnect()
                listener?.let { DfuServiceListenerHelper.unregisterProgressListener(app, it) }
                L.e("BleFirmwareUpdate", "Firmware update failed", error)
            }
        }
        return snapshot()
    }
}
