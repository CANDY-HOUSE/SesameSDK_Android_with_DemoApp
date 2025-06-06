package co.candyhouse.sesame.open.device.sesameBiometric.capability.passcode

import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.ble.os3.base.SesameOS3Payload
import co.candyhouse.sesame.open.CHAccountManager
import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.open.CHResultState
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHAutoInitCapabilityImpl
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHDataSynchronizeCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHDataSynchronizeCapableImpl
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBase
import co.candyhouse.sesame.server.dto.AuthenticationDataWrapper
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.server.dto.CHKeyBoardPassCodeNameRequest
import co.candyhouse.sesame.server.dto.AuthenticationData
import co.candyhouse.sesame.server.dto.CredentialListResponse
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.hexStringToByteArray
import com.google.gson.Gson
import kotlin.collections.set

internal open class CHPassCodeCapableImpl() :
    CHAutoInitCapabilityImpl(),
    CHPassCodeCapable {

    private val eventHandlers = mutableMapOf<CHPassCodeDelegate, CHPassCodeEventHandler>()

    override fun keyBoardPassCodeModeGet(result: CHResult<Byte>) {
        sendCommandSafely(SesameOS3Payload(SesameItemCode.SSM_OS3_PASSCODE_MODE_GET.value, byteArrayOf()), result) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(res.payload[0])))
        }
    }

    override fun keyBoardPassCodeModeSet(mode: Byte, result: CHResult<CHEmpty>) {
        sendCommandSafely(SesameOS3Payload(SesameItemCode.SSM_OS3_PASSCODE_MODE_SET.value, byteArrayOf(mode)), result) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun keyBoardPassCodeAdd(id: String, name: String, result: CHResult<CHEmpty>) {
        sendCommandSafely(
            SesameOS3Payload(
                SesameItemCode.SSM_OS3_PASSCODE_ADD.value,
                byteArrayOf(id.toByteArray().size.toByte()) + id.toByteArray() + byteArrayOf(name.toByteArray().size.toByte()) + name.toByteArray()
            ), result
        ) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun keyBoardPassCodeDelete(keyBoardPassCodeID: String, deviceId: String, result: CHResult<CHEmpty>) {
        sendCommandSafely(SesameOS3Payload(SesameItemCode.SSM_OS3_PASSCODE_DELETE.value, keyBoardPassCodeID.hexStringToByteArray()), result) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun keyBoardPassCodeMove(cardId: String, touchProUUID: String, result: CHResult<CHEmpty>) {
        sendCommandSafely(
            SesameOS3Payload(
                SesameItemCode.SSM_OS3_PASSCODE_MOVE.value,
                byteArrayOf(cardId.hexStringToByteArray().size.toByte()) + cardId.hexStringToByteArray() + touchProUUID.toByteArray()
            ), result
        ) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun keyBoardPassCodeChange(ID: String, name: String, result: CHResult<CHEmpty>) {
        sendCommandSafely(
            SesameOS3Payload(SesameItemCode.SSM_OS3_PASSCODE_CHANGE.value, byteArrayOf(ID.hexStringToByteArray().size.toByte()) + ID.hexStringToByteArray() + name.chunked(2).map { it.toInt(16).toByte() }.toByteArray()),
            result
        ) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun sendKeyBoardPassCodeDataGetCmd(result: CHResult<CHEmpty>) {
        sendCommandSafely(SesameOS3Payload(SesameItemCode.SSM_OS3_PASSCODE_GET.value, byteArrayOf()), result) {}
    }

    override fun keyBoardPassCodeNameGet(keyBoardPassCode: String, keyBoardPassCodeNameUUID: String, subUUID: String, deviceUUID: String, result: CHResult<String>) {
        CHAccountManager.getKeyBoardPassCodeName(keyBoardPassCode, keyBoardPassCodeNameUUID, subUUID, deviceUUID) { it ->
            it.onSuccess {
                val keyBoardPassCodeName = it.data
                L.d("harry", "keyBoardPassCodeName: $keyBoardPassCodeName； keyBoardPassCode: $keyBoardPassCode")
                result.invoke(Result.success(CHResultState.CHResultStateNetworks(keyBoardPassCodeName)))
            }
        }
    }

    override fun keyBoardPassCodeNameSet(keyBoardPassCodeNameRequest: CHKeyBoardPassCodeNameRequest, result: CHResult<String>) {
        L.d(
            "harry",
            "【keyBoardPassCodeNameSet】keyBoardPassCodeNameUUID: ${keyBoardPassCodeNameRequest.keyBoardPassCodeNameUUID}; keyBoardPassCode: ${keyBoardPassCodeNameRequest.keyBoardPassCode}; subUUID: ${keyBoardPassCodeNameRequest.subUUID}; deviceUUID: ${keyBoardPassCodeNameRequest.stpDeviceUUID}"
        )
        CHAccountManager.setKeyBoardPassCodeName(keyBoardPassCodeNameRequest) { it ->
            it.onSuccess {
                val res = it.data
                L.d("harry", "keyBoardPassCodeNameSet res: $res")
                result.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
            }
            it.onFailure { result.invoke(Result.failure(it)) }
        }
    }
    private val dataSyncCapable = CHDataSynchronizeCapableImpl()
    override fun getBoardPassCodeDataSyncCapable(): CHDataSynchronizeCapable {
        return dataSyncCapable
    }

    override fun registerEventDelegate(device: CHSesameBiometricBase, delegate: CHPassCodeDelegate) {
        setupSupport(device)
        val handler = CHPassCodeEventHandler(delegate)
        eventHandlers[delegate] = handler
        device.addEventHandler(handler)
    }

    override fun unregisterEventDelegate(device: CHSesameBiometricBase, delegate: CHPassCodeDelegate) {
        if (!isInitialized()) return
        eventHandlers[delegate]?.let { handler ->
            device.removeEventHandler(handler)
            eventHandlers.remove(delegate)
        }
    }
}