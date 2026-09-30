package co.candyhouse.sesame.ble

import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.HttpResponseCallback

/** Configure once before starting BLE. The SDK has no AWS or database implementation. */
object CHBleSupport {
    lateinit var backend: CHBleHost
        private set
    lateinit var keys: CHKeyPersistence
        private set
    lateinit var gatewayTenantId: String
        private set

    fun initialize(backend: CHBleHost, keys: CHKeyPersistence, gatewayTenantId: String) {
        this.backend = backend
        this.keys = keys
        this.gatewayTenantId = gatewayTenantId
    }
}

/** Host-provided services needed by device registration, authentication and synchronization. */
interface CHBleHost {
    fun isNetworkAvailable(): Boolean
    fun shouldUploadHistory(deviceId: String): Boolean
    fun signGuestKey(deviceId: String, token: String, secretKey: String, onResponse: CHResult<String>)
    fun updateDeviceFirmwareVersion(deviceUUID: String, versionTag: String, onResponse: CHResult<Any>)
    fun postSS2History(deviceID: String, hisHex: String, onResponse: CHResult<Any>)
    fun postOS3History(deviceID: String, hisHex: String, onResponse: CHResult<Any>)
    fun postBatteryData(deviceID: String, payloadString: String, onResponse: CHResult<Any>)
    fun registerOs2(deviceId: String, appKey: String, nonce: String, registrationKey: String, productType: String, onResponse: CHResult<CHRegistrationResult>)
    fun registerOs3(deviceId: String, productType: String, publicKey: String, onResponse: CHResult<Any>)
}

/** Values required to finish the OS2 BLE registration handshake. */
data class CHRegistrationResult(val signature: String, val token: String, val publicKey: String)

interface CHKeyPersistence {
    fun insert(device: CHDevice, onResponse: HttpResponseCallback<String>)
    fun deleteByDeviceId(deviceId: String, onResponse: HttpResponseCallback<Int>)
}
