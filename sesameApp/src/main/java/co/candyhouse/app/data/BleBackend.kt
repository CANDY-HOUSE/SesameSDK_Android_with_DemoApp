package co.candyhouse.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import co.candyhouse.sesame.ble.CHBleHost
import co.candyhouse.sesame.ble.CHRegistrationResult
import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.CHResultState
import co.candyhouse.sesame.utils.L
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

/** BLE server handshakes are delegated to the trusted Biz page, which owns cloud transport. */
class BleBackend(private val context: Context) : CHBleHost {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    var gatewayDevices: Set<String> = emptySet()
    override fun isNetworkAvailable(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        return manager.getNetworkCapabilities(manager.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    override fun shouldUploadHistory(deviceId: String) = deviceId.uppercase() !in gatewayDevices
    private fun id(value: String) = value.also { UUID.fromString(it) }

    @Volatile
    var requestCloud: (suspend (String, JSONObject, String?) -> Any?)? = null

    private fun <T> request(operation: String, body: JSONObject, result: CHResult<T>, deviceId: String? = null, decode: (Any?) -> T) {
        val transport = requestCloud
        scope.launch {
            result(runCatching {
                checkNotNull(transport) { "Web page unavailable" }
                CHResultState.CHResultStateNetworks(decode(transport(operation, body, deviceId)))
            })
        }
    }

    override fun signGuestKey(deviceId: String, token: String, secretKey: String, onResponse: CHResult<String>) =
        request("signGuestKey", JSONObject().put("deviceId", deviceId).put("token", token).put("secretKey", secretKey), onResponse) {
            it as String
        }

    override fun updateDeviceFirmwareVersion(deviceUUID: String, versionTag: String, onResponse: CHResult<Any>) =
        request<Any>("firmware", JSONObject().put("versionTag", versionTag), { result ->
            L.d("SesameFirmware", "version upload ${if (result.isSuccess) "success" else "failed"} device=$deviceUUID")
            onResponse(result)
        }, id(deviceUUID)) { Unit }

    override fun postSS2History(deviceID: String, hisHex: String, onResponse: CHResult<Any>) =
        request("history", JSONObject().put("s", deviceID).put("v", hisHex), onResponse) { Unit }

    override fun postOS3History(deviceID: String, hisHex: String, onResponse: CHResult<Any>) =
        request("history", JSONObject().put("s", deviceID).put("v", hisHex).put("t", "5"), onResponse) { Unit }

    override fun postBatteryData(deviceID: String, payloadString: String, onResponse: CHResult<Any>) =
        request("battery", JSONObject().put("payload", payloadString), onResponse, id(deviceID)) { Unit }

    override fun registerOs2(deviceId: String, appKey: String, nonce: String, registrationKey: String, productType: String, onResponse: CHResult<CHRegistrationResult>) =
        request(
            "registerOs2", JSONObject().put(
                "s1", JSONObject()
                    .put("ak", appKey).put("n", nonce).put("e", registrationKey).put("t", productType)
            ), onResponse, id(deviceId)
        ) {
            val response = it as JSONObject
            CHRegistrationResult(response.getString("sig1"), response.getString("st"), response.getString("pubkey"))
        }

    override fun registerOs3(deviceId: String, productType: String, publicKey: String, onResponse: CHResult<Any>) =
        request("registerOs3", JSONObject().put("t", productType).put("pk", publicKey), onResponse, id(deviceId)) { Unit }
}
