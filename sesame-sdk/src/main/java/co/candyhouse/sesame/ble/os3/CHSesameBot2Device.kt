package co.candyhouse.sesame.ble.os3

import android.annotation.SuppressLint
import co.candyhouse.sesame.ble.DeviceSegmentType
import co.candyhouse.sesame.ble.SSM3PublishPayload
import co.candyhouse.sesame.ble.SSM3ResponsePayload
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.ble.SesameResultCode
import co.candyhouse.sesame.ble.isBleAvailable
import co.candyhouse.sesame.ble.os3.base.SesameOS3BleCipher
import co.candyhouse.sesame.ble.os3.base.SesameOS3Payload
import co.candyhouse.sesame.db.CHDB
import co.candyhouse.sesame.db.model.CHDevice
import co.candyhouse.sesame.db.model.historyTagBLE
import co.candyhouse.sesame.db.model.historyTagIOT
import co.candyhouse.sesame.open.devices.CHSesame2MechStatus
import co.candyhouse.sesame.open.devices.CHSesame5MechStatus
import co.candyhouse.sesame.open.devices.CHSesameBot2
import co.candyhouse.sesame.open.devices.CHSesameBot2MechStatus
import co.candyhouse.sesame.open.devices.CHSesamebot2Event
import co.candyhouse.sesame.open.devices.CHSesamebot2Status
import co.candyhouse.sesame.open.devices.base.CHDeviceLoginStatus
import co.candyhouse.sesame.open.devices.base.CHDeviceStatus
import co.candyhouse.sesame.open.devices.base.CHSesameOS3LockBase
import co.candyhouse.sesame.open.devices.base.NSError
import co.candyhouse.sesame.server.CHAPIClientBiz
import co.candyhouse.sesame.server.CHIotManager
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.CHResultState
import co.candyhouse.sesame.utils.EccKey
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.aescmac.AesCmac
import co.candyhouse.sesame.utils.hexStringToByteArray
import co.candyhouse.sesame.utils.toHexString

@SuppressLint("MissingPermission")
internal class CHSesameBot2Device : CHSesameOS3LockBase(), CHSesameBot2 {

    override var scripts: CHSesamebot2Status =
        CHSesamebot2Status(curIdx = 0u, eventLength = 0u, events = emptyList())

    private val scriptNameListLock = Any()
    private var scriptNameListInFlight = false
    private val pendingScriptNameListResults = mutableListOf<CHResult<CHSesamebot2Status>>()

    override fun goIOT() {
        CHIotManager.subscribeSesame2Shadow(this) { result ->
            result.onSuccess { resource ->
                resource.data.state.reported.wm2s?.let { wm2s ->
                    isConnectedByWM2 = wm2s.map { it.value.hexStringToByteArray().first().toInt() }.contains(1)
                }

                if (isConnectedByWM2) {
                    resource.data.state.reported.mechst?.let { mechShadow ->
                        L.d("harry", "[bot2][iot]mechShadow[${mechShadow.hexStringToByteArray().size}]: $mechShadow")
                        val bytes = mechShadow.hexStringToByteArray()
                        mechStatus = when {
                            bytes.size >= 7 -> CHSesame5MechStatus(CHSesame2MechStatus(bytes).ss5Adapter())
                            bytes.size == 3 -> CHSesameBot2MechStatus(bytes.sliceArray(0..2))
                            else -> mechStatus
                        }
                    }
                    deviceShadowStatus =
                        if (mechStatus!!.isInLockRange) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked
                } else {
                    deviceShadowStatus = null
                }
            }
        }
    }

    override fun click(index: UByte?, historytag: ByteArray?, result: CHResult<CHEmpty>) {
        L.d("hcia", "[bot2]click[index:$index]")
        var itemCode = SesameItemCode.click
        if (index != null) {
            itemCode = SesameItemCode.values().find {
                it.value == (SesameItemCode.BOT2_ITEM_CODE_RUN_SCRIPT_0.value + index).toUByte()
            } ?: SesameItemCode.click
        }

        if (deviceStatus.value == CHDeviceLoginStatus.unlogined && deviceShadowStatus != null) {
            CHAPIClientBiz.cmdSesame(itemCode, this, sesame2KeyData!!.historyTagIOT(historytag), result)
            return
        }
        if (!isBleAvailable(result)) {
            CHAPIClientBiz.cmdSesame(itemCode, this, sesame2KeyData!!.historyTagIOT(historytag), result)
            return
        }

        sendCommand(
            SesameOS3Payload(itemCode.value, sesame2KeyData!!.historyTagBLE(historytag)),
            DeviceSegmentType.cipher
        ) {
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun sendClickScript(index: UByte, script: ByteArray, result: CHResult<CHEmpty>) {
        L.d("hcia", "[bot2]combinedData:$index ${script.toList()}")
        if (!isBleAvailable(result)) return

        val sendData = byteArrayOf(index.toByte()) + script
        sendCommand(
            SesameOS3Payload(SesameItemCode.BOT2_ITEM_CODE_EDIT_SCRIPT.value, sendData),
            DeviceSegmentType.cipher
        ) {
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun selectScript(index: UByte, result: CHResult<CHEmpty>) {
        if (!isBleAvailable(result)) return
        L.d("selectScript", "[bot2]select[index:$index]")
        sendCommand(
            SesameOS3Payload(SesameItemCode.SCRIPT_SELECT.value, byteArrayOf(index.toByte())),
            DeviceSegmentType.cipher
        ) {
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun getCurrentScript(index: UByte?, result: CHResult<CHSesamebot2Event>) {
        L.d("hcia", "[send]getNowScript")
        if (!isBleAvailable(result)) return

        val idx = index?.let { byteArrayOf(it.toByte()) } ?: byteArrayOf()
        sendCommand(
            SesameOS3Payload(SesameItemCode.SCRIPT_CURRENT.value, idx),
            DeviceSegmentType.cipher
        ) { res ->
            L.d("hcia", "result${res.payload.toList()}")
            if (res.cmdResultCode == SesameResultCode.success.value) {
                val resp = CHSesamebot2Event.fromByteArray(res.payload)
                if (resp != null) {
                    result.invoke(Result.success(CHResultState.CHResultStateBLE(resp)))
                } else {
                    result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
                }
            } else {
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
            }
        }
    }

    override fun getScriptNameList(result: CHResult<CHSesamebot2Status>) {
        L.d("getScriptNameList", "[send]getScriptNameList")
        if (!isBleAvailable(result)) {
            result.invoke(Result.failure(NSError("ble_unavailable", "SCRIPT_NAME_LIST", -2)))
            return
        }

        synchronized(scriptNameListLock) {
            pendingScriptNameListResults.add(result)
            if (scriptNameListInFlight) {
                L.d("getScriptNameList", "getScriptNameList merged (inFlight)")
                return
            }
            scriptNameListInFlight = true
        }

        sendCommand(
            SesameOS3Payload(SesameItemCode.SCRIPT_NAME_LIST.value, byteArrayOf()),
            DeviceSegmentType.cipher
        ) { res ->
            val callbacks: List<CHResult<CHSesamebot2Status>> = synchronized(scriptNameListLock) {
                scriptNameListInFlight = false
                val copy = pendingScriptNameListResults.toList()
                pendingScriptNameListResults.clear()
                copy
            }

            if (res.cmdResultCode == SesameResultCode.success.value) {
                val payload = res.payload
                L.d("getScriptNameList", "SCRIPT_NAME_LIST payload=${payload.toHexString()}")
                val status = CHSesamebot2Status.fromByteArray(payload)
                if (status != null) {
                    scripts = status
                    callbacks.forEach { cb ->
                        cb.invoke(Result.success(CHResultState.CHResultStateBLE(status)))
                    }
                } else {
                    callbacks.forEach { cb ->
                        cb.invoke(Result.failure(NSError("parse_failed", "SCRIPT_NAME_LIST", -1)))
                    }
                }
            } else {
                callbacks.forEach { cb ->
                    cb.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
                }
            }
        }
    }

    override fun onHistoryReceived(historyData: ByteArray) {}

    override fun onHistoryReceivedInternal(historyData: ByteArray) {
        onHistoryReceived(historyData)
    }

    override fun handleRegisterResponse(
        registerRes: SSM3ResponsePayload,
        serverSecret: String,
        result: CHResult<CHEmpty>
    ) {
        try {
            val eccPublicKeyFromSS5 = registerRes.payload.sliceArray(13..76)
            val ecdhSecret = EccKey.ecdh(eccPublicKeyFromSS5)

            mechStatus = CHSesame5MechStatus(registerRes.payload.sliceArray(0..6))

            val deviceSecret = ecdhSecret.sliceArray(0..15).toHexString()
            saveDeviceAndCipher(deviceSecret, serverSecret, result)
            deviceStatus = if (mechStatus?.isInLockRange == true) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked
        } catch (e: Exception) {
            mechStatus = CHSesameBot2MechStatus(registerRes.payload.sliceArray(0..2))
            deviceStatus = if (mechStatus?.isInLockRange == true) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked

            val ecdhSecretPre16 = EccKey.ecdh(registerRes.payload.sliceArray(3..66)).sliceArray(0..15)
            sesame2KeyData = CHDevice(
                deviceId.toString(),
                productModel.deviceModel(),
                null,
                "0000",
                ecdhSecretPre16.toHexString(),
                serverSecret
            )
            cipher = SesameOS3BleCipher(
                "customDeviceName",
                AesCmac(ecdhSecretPre16, 16).computeMac(mSesameToken)!!,
                ("00" + mSesameToken.toHexString()).hexStringToByteArray()
            )
            CHDB.CHSS2Model.insert(sesame2KeyData!!) {
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            }
        }
    }

    override fun handleDevicePublish(receivePayload: SSM3PublishPayload) {
        L.d("harry", "onGattSesamePublish: ${receivePayload.cmdItCode} payload: ${receivePayload.payload.toHexString()}")

        when (receivePayload.cmdItCode) {
            SesameItemCode.mechStatus.value -> {
                L.d("harry", "[bot2]mechStatus【size: ${receivePayload.payload.size}】: ${receivePayload.payload.toHexString()}")
                mechStatus = when (receivePayload.payload.size) {
                    7 -> CHSesame5MechStatus(receivePayload.payload)
                    3 -> CHSesameBot2MechStatus(receivePayload.payload)
                    else -> mechStatus
                }
                if (mechStatus != null) {
                    deviceStatus = if (mechStatus!!.isInLockRange) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked
                    reportBatteryData(receivePayload.payload.sliceArray(0..1).toHexString())
                }
            }
        }
    }
}