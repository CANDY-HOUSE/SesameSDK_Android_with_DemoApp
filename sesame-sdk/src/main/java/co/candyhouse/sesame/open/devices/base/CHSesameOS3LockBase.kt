package co.candyhouse.sesame.open.devices.base

import android.annotation.SuppressLint
import co.candyhouse.sesame.ble.CHDeviceUtil
import co.candyhouse.sesame.ble.CHadv
import co.candyhouse.sesame.ble.DeviceSegmentType
import co.candyhouse.sesame.ble.SSM3PublishPayload
import co.candyhouse.sesame.ble.SSM3ResponsePayload
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.ble.SesameResultCode
import co.candyhouse.sesame.ble.isBleAvailable
import co.candyhouse.sesame.ble.os3.base.CHSesameOS3
import co.candyhouse.sesame.ble.os3.base.SesameOS3BleCipher
import co.candyhouse.sesame.ble.os3.base.SesameOS3Payload
import co.candyhouse.sesame.db.CHDB
import co.candyhouse.sesame.db.model.CHDevice
import co.candyhouse.sesame.server.CHAPIClientBiz
import co.candyhouse.sesame.server.dto.CHOS3RegisterReq
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.CHResultState
import co.candyhouse.sesame.utils.EccKey
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.aescmac.AesCmac
import co.candyhouse.sesame.utils.hexStringToByteArray
import co.candyhouse.sesame.utils.isInternetAvailable
import co.candyhouse.sesame.utils.toBigLong
import co.candyhouse.sesame.utils.toUInt32ByteArray
import kotlin.math.abs

/**
 * 锁类公共基类
 *
 * @author frey on 2026/3/30
 */
@SuppressLint("MissingPermission")
internal abstract class CHSesameOS3LockBase : CHSesameOS3(), CHSesameLock, CHDeviceUtil {

    override var advertisement: CHadv? = null
        set(value) {
            field = value
            parceADV(value)
            value?.let {
                isHistory = it.adv_tag_b1
            }
        }

    protected var isConnectedByWM2: Boolean = false

    protected var isHistory: Boolean = false
        set(value) {
            if (deviceStatus.value == CHDeviceLoginStatus.logined) {
                field = value
                if (field) {
                    readHistoryCommand()
                }
            } else {
                field = value
            }
        }

    override fun setBleTxPower(txPower: Byte, result: CHResult<CHEmpty>) {
        if (!isBleAvailable(result)) return
        sendCommand(
            SesameOS3Payload(
                SesameItemCode.SSM3_ITEM_CODE_BLE_TX_POWER_SETTING.value,
                byteArrayOf(txPower)
            ),
            DeviceSegmentType.cipher
        ) {}
    }

    override fun register(result: CHResult<CHEmpty>) {
        if (deviceStatus != CHDeviceStatus.ReadyToRegister) {
            result.invoke(Result.failure(NSError("Busy", "CBCentralManager", 7)))
            return
        }

        deviceStatus = CHDeviceStatus.Registering
        val serverSecret = mSesameToken.toHexString()

        CHAPIClientBiz.myDevicesRegisterSesame5Post(
            deviceId.toString(),
            CHOS3RegisterReq(productModel.productType().toString(), serverSecret)
        ) { apiResult ->
            apiResult.exceptionOrNull()?.let { e ->
                L.Companion.d("os3lock", "[register][server] failed: ${e.message}")
            }

            sendCommand(
                SesameOS3Payload(
                    SesameItemCode.registration.value,
                    EccKey.getPubK().hexStringToByteArray() + System.currentTimeMillis().toUInt32ByteArray()
                ),
                DeviceSegmentType.plain
            ) { registerRes ->
                handleRegisterResponse(registerRes, serverSecret, result)
            }
        }
    }

    override fun login(token: String?) {
        deviceStatus = CHDeviceStatus.BleLogining

        val sessionAuth: ByteArray? = if (isNeedAuthFromServer == true) {
            token?.hexStringToByteArray()
        } else {
            AesCmac(sesame2KeyData!!.secretKey.hexStringToByteArray(), 16).computeMac(mSesameToken)
        }

        cipher = SesameOS3BleCipher(
            "customDeviceName",
            sessionAuth!!,
            ("00" + mSesameToken.toHexString()).hexStringToByteArray()
        )

        sendCommand(
            SesameOS3Payload(SesameItemCode.login.value, sessionAuth.sliceArray(0..3)),
            DeviceSegmentType.plain
        ) { loginPayload ->
            handleLoginResponse(loginPayload)
        }
    }

    protected open fun handleLoginResponse(loginPayload: SSM3ResponsePayload) {
        val systemTime = loginPayload.payload.sliceArray(0..3).toBigLong()
        val currentTimestamp = System.currentTimeMillis() / 1000
        val timeMinus = currentTimestamp.minus(systemTime)
        if (abs(timeMinus) > 3) {
            sendCommand(
                SesameOS3Payload(
                    SesameItemCode.time.value,
                    System.currentTimeMillis().toUInt32ByteArray()
                ),
                DeviceSegmentType.cipher
            ) {}
        }
    }

    protected abstract fun handleRegisterResponse(
        registerRes: SSM3ResponsePayload,
        serverSecret: String,
        result: CHResult<CHEmpty>
    )

    protected fun saveDeviceAndCipher(
        deviceSecretHex: String,
        serverSecret: String,
        result: CHResult<CHEmpty>
    ) {
        val candyDevice = CHDevice(
            deviceId.toString(),
            advertisement!!.productModel!!.deviceModel(),
            null,
            "0000",
            deviceSecretHex,
            serverSecret
        )
        sesame2KeyData = candyDevice

        val sessionAuth = AesCmac(deviceSecretHex.hexStringToByteArray(), 16).computeMac(mSesameToken)
        cipher = SesameOS3BleCipher(
            "customDeviceName",
            sessionAuth!!,
            ("00" + mSesameToken.toHexString()).hexStringToByteArray()
        )

        CHDB.CHSS2Model.insert(candyDevice) {
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    protected fun reportBatteryData(payloadString: String) {
        L.Companion.d("os3lock", "[reportBatteryData] ${isInternetAvailable()}, ${!isConnectedByWM2}, payload: $payloadString")
        CHAPIClientBiz.postBatteryData(deviceId.toString().uppercase(), payloadString) {
            it.onSuccess { resp ->
                batteryPercentage = ((resp.data as? Map<*, *>)?.get("batteryPercentage") as? Number)?.toInt()
            }
        }
    }

    protected open fun onHistoryReceivedInternal(historyData: ByteArray) {}

    protected fun readHistoryCommand() {
        val isConnectNET = isInternetAvailable()
        sendCommand(
            SesameOS3Payload(SesameItemCode.history.value, byteArrayOf(0x01)),
            DeviceSegmentType.cipher
        ) { res ->
            val historyPayload = res.payload
            onHistoryReceivedInternal(historyPayload)

            if (res.cmdResultCode == SesameResultCode.success.value) {
                if (isConnectNET && !isConnectedByWM2) {
                    CHAPIClientBiz.postOS3History(
                        deviceId.toString().uppercase(),
                        historyPayload.toHexString()
                    ) { postResult ->
                        val recordId = historyPayload.sliceArray(0..3)
                        postResult.onSuccess {
                            sendCommand(
                                SesameOS3Payload(
                                    SesameItemCode.SSM2_ITEM_CODE_HISTORY_DELETE.value,
                                    recordId
                                ),
                                DeviceSegmentType.cipher
                            ) { deleteRes ->
                                L.Companion.d("os3lock", "[history][delete]: ${deleteRes.cmdResultCode}")
                            }
                        }
                        postResult.onFailure { exception ->
                            L.Companion.d("os3lock", "[history] post failed: $exception")
                        }
                    }
                }
            }
        }
    }

    override fun onGattSesamePublish(receivePayload: SSM3PublishPayload) {
        super.onGattSesamePublish(receivePayload)

        when (receivePayload.cmdItCode) {
            SesameItemCode.SSM3_ITEM_CODE_BATTERY_VOLTAGE.value -> {
                reportBatteryData(receivePayload.payload.toHexString())
            }

            SesameItemCode.SSM3_ITEM_CODE_BLE_TX_POWER_SETTING.value -> {
                bleTxPower = receivePayload.payload[0]
            }
        }

        handleDevicePublish(receivePayload)
    }

    protected abstract fun handleDevicePublish(receivePayload: SSM3PublishPayload)
}