package co.candyhouse.app.bridge

import android.content.Context
import android.os.Build
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.SesameApp
import co.candyhouse.app.ble.BleController
import co.candyhouse.app.ble.BleFirmwareUpdate
import co.candyhouse.app.ble.BleRegistrationController
import co.candyhouse.app.ble.BotSettings
import co.candyhouse.app.ble.DeviceLocation
import co.candyhouse.app.ble.LockSettings
import co.candyhouse.app.connecteddevice.AutoUnlockGeofenceManager
import co.candyhouse.app.connecteddevice.SesameConnectedDeviceService
import co.candyhouse.app.data.KeyHandoff
import co.candyhouse.app.data.auth.LegacySession
import co.candyhouse.app.util.toKeyJson
import co.candyhouse.sesame.utils.L
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

/** Routes H5 requests to native capabilities without owning an Activity or WebView. */
class NativeRequestHandler(
    private val context: Context,
    private val ble: BleController,
    private val registration: BleRegistrationController,
    private val keyHandoff: KeyHandoff,
    private val language: () -> String,
    private val requestBle: () -> Unit,
    private val startDiscovery: () -> Unit,
    private val stopDiscovery: () -> Unit,
    private val registeredKey: () -> JSONObject?,
    private val scanQr: () -> Unit,
    private val showRoute: (String) -> Unit,
    private val pushInfo: suspend () -> JSONObject,
    private val notificationSettings: () -> Unit,
    private val openWebPage: (String) -> Unit,
    private val openExternal: (String) -> Unit,
    private val autoUnlockMap: (JSONObject) -> JSONObject = { error("Map requires an Activity") },
    private val watchNfc: (String?) -> Unit = {},
    private val haptic: () -> Unit = {}
) {
    suspend fun handle(json: JSONObject, respond: (Boolean, Any?, String?) -> Unit) {
        fun reply(ok: Boolean, data: Any? = null, error: String? = null) = respond(ok, data, error)
        runCatching {
            when (json.optString("action")) {
                "home.ready" -> {
                    ble.snapshot(); reply(true)
                }

                "home.currentLocation" -> reply(true, DeviceLocation.current(context))

                "home.appVersion" -> reply(
                    true,
                    JSONObject().put("versionName", BuildConfig.VERSION_NAME).put("versionCode", BuildConfig.VERSION_CODE).put(
                        "display",
                        "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})-${BuildConfig.GIT_HASH}-${BuildConfig.BUILD_TYPE}-${Build.MODEL}:${Build.VERSION.SDK_INT}"
                    )
                )

                "home.session" -> {
                    val token = LegacySession.token()
                    keyHandoff.initialize(token != null || LegacySession.retired())
                    reply(
                        true, JSONObject().put("token", token ?: JSONObject.NULL)
                            .put("signedIn", token != null)
                            .put("language", language())
                            .put("guestIdentityId", LegacySession.guestIdentityId())
                    )
                }

                "home.retireSession" -> {
                    LegacySession.retire(); reply(true)
                }

                "home.devices" -> {
                    ble.accept(json.getJSONArray("devices"), json.optString("historyTag"))
                    if (json.getJSONArray("devices").length() > 0) requestBle()
                    reply(true)
                }

                "home.offlineDevices" -> {
                    ble.restoreSaved()
                    requestBle()
                    reply(true, JSONObject().put("devices", keyHandoff.offlineDevices()).put("language", language()))
                    ble.snapshot()
                }

                "home.localKeys" -> reply(true, JSONArray().apply {
                    keyHandoff.pending().forEach { put(it.toKeyJson().put("deviceName", keyHandoff.name(it)).put("keyLevel", keyHandoff.level(it))) }
                })

                "home.deviceKey" -> reply(true, keyHandoff.key(json.getString("deviceUUID")).toKeyJson())

                "home.keysUploaded" -> {
                    keyHandoff.acknowledge(json.getString("deviceUUID")); reply(true)
                }

                "home.command", "home.scripts" -> ble.handle(json.getString("action"), json) { ok, error -> reply(ok, error = error) }
                "home.firmwareTrace" -> {
                    val transport = json.optString("transport")
                    val phase = json.optString("phase")
                    if (transport in setOf("ble", "gateway") && phase in setOf("start", "progress", "completed", "failed", "version")) {
                        L.d("SesameFirmware", "transport=$transport phase=$phase progress=${json.optInt("progress", -1)}")
                    }
                    reply(true)
                }

                "home.firmwareStatus" -> reply(true, BleFirmwareUpdate.snapshot())
                "home.firmwareStart" -> reply(
                    true,
                    BleFirmwareUpdate.start(context, requireNotNull(ble.device(json.getString("deviceUUID"))), json.getString("url"), ble::reconnect)
                )

                "home.firmwareInfo" -> reply(
                    true,
                    JSONObject().put("productType", requireNotNull(ble.device(json.getString("deviceUUID"))).productModel.productType())
                        .put("firmwareDir", context.getSharedPreferences("firmware_dir_prefs", Context.MODE_PRIVATE).getString("firmware_dir", "prod"))
                )

                "home.devicePreferences" -> {
                    val app = context.applicationContext as SesameApp
                    val id = json.getString("deviceUUID")
                    require(ble.device(id) != null)
                    when (json.optString("operation")) {
                        "widget" -> app.devicePreferences.setEnabled("wid", id, json.getBoolean("value"))
                        "clearNfc" -> app.devicePreferences.setNfc(id, null)
                        "watchNfc" -> watchNfc(id)
                        "stopNfc" -> watchNfc(null)
                    }
                    SesameConnectedDeviceService.sync(context)
                    reply(true, app.devicePreferences.snapshot(id))
                }

                "home.autoUnlockMap" -> reply(true, autoUnlockMap(json))
                "home.haptic" -> {
                    haptic(); reply(true)
                }

                "home.dropDevice" -> {
                    val app = context.applicationContext as SesameApp
                    val id = json.getString("deviceUUID")
                    ble.drop(id); keyHandoff.acknowledge(id); app.devicePreferences.clear(id)
                    AutoUnlockGeofenceManager.sync(context)
                    SesameConnectedDeviceService.sync(context)
                    reply(true)
                }

                "home.peripheralSettings" -> reply(true, ble.peripheralSettings.handle(json))
                "home.hubSettings" -> reply(true, ble.hubSettings(json))
                "home.botSettings" -> reply(true, BotSettings.handle(ble, json))
                "home.lockSettings" -> reply(true, LockSettings.handle(ble, json))
                "home.scanStart" -> {
                    startDiscovery(); reply(true)
                }

                "home.scanStop" -> {
                    stopDiscovery(); reply(true)
                }

                "home.register" -> registration.register(json.getString("deviceUUID")) { ok, error ->
                    if (ok) ble.peripheralSettings.registered(json.getString("deviceUUID"))
                    reply(ok, registeredKey(), error)
                }

                "home.scanQr" -> {
                    scanQr(); reply(true)
                }

                "home.route" -> {
                    showRoute(json.optString("path")); reply(true)
                }

                "home.pushInfo" -> reply(true, pushInfo())
                "home.notificationSettings" -> {
                    notificationSettings(); reply(true)
                }

                "home.openWebPage" -> {
                    openWebPage(json.getString("url")); reply(true)
                }

                "home.external" -> {
                    openExternal(json.getString("url")); reply(true)
                }

                else -> reply(false, error = "This native capability is not available in this version")
            }
        }.onFailure {
            if (it is CancellationException) throw it
            reply(false, null, "Native request failed; retry when connected")
        }
    }
}
