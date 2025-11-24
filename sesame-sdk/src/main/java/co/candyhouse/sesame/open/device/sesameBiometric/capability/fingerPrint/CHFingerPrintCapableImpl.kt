package co.candyhouse.sesame.open.device.sesameBiometric.capability.fingerPrint

import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.ble.os3.base.SesameOS3Payload
import co.candyhouse.sesame.open.CHAccountManager
import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.open.CHResultState
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHAutoInitCapabilityImpl
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHDataSynchronizeCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHDataSynchronizeCapableImpl
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBase
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.server.dto.CHFingerPrintNameRequest
import co.candyhouse.sesame.utils.hexStringToByteArray

internal class CHFingerPrintCapableImpl() :
    CHAutoInitCapabilityImpl(),
    CHFingerPrintCapable {

    private val eventHandlers = mutableMapOf<CHFingerPrintDelegate, CHFingerPrintEventHandler>()

    override fun fingerPrintModeGet(result: CHResult<Byte>) {
        sendCommandSafely(
            SesameOS3Payload(
                SesameItemCode.SSM_OS3_FINGERPRINT_MODE_GET.value,
                byteArrayOf()
            ), result
        ) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(res.payload[0])))
        }
    }

    override fun fingerPrintModeSet(mode: Byte, result: CHResult<CHEmpty>) {
        sendCommandSafely(
            SesameOS3Payload(
                SesameItemCode.SSM_OS3_FINGERPRINT_MODE_SET.value,
                byteArrayOf(mode)
            ), result
        ) {
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun fingerPrintDelete(fingerPrintID: String, deviceId: String, result: CHResult<CHEmpty>) {
        sendCommandSafely(
            SesameOS3Payload(
                SesameItemCode.SSM_OS3_FINGERPRINT_DELETE.value,
                fingerPrintID.hexStringToByteArray()
            ), result
        ) {
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun fingerPrintNameGet(fingerPrintID: String, fingerPrintNameUUID: String, subUUID: String, deviceUUID: String, result: CHResult<String>) {
        CHAccountManager.getFingerPrintName(fingerPrintID, fingerPrintNameUUID, subUUID, deviceUUID) { it ->
            it.onSuccess {
                val fingerPrintName = it.data
                result.invoke(Result.success(CHResultState.CHResultStateNetworks(fingerPrintName)))
            }
        }
    }

    override fun fingerPrintNameSet(fingerPrintNameRequest: CHFingerPrintNameRequest, result: CHResult<String>) {
        CHAccountManager.setFingerPrintName(fingerPrintNameRequest) { it ->
            it.onSuccess {
                val res = it.data
                result.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
            }
            it.onFailure {
                result.invoke(Result.failure(it))
            }
        }
    }

    override fun fingerPrints(result: CHResult<CHEmpty>) {
        sendCommandSafely(
            SesameOS3Payload(
                SesameItemCode.SSM_OS3_FINGERPRINT_GET.value,
                byteArrayOf()
            ), result
        ) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun fingerPrintsChange(ID: String, hexName: String, result: CHResult<CHEmpty>) {
        sendCommandSafely(
            SesameOS3Payload(
                SesameItemCode.SSM_OS3_FINGERPRINT_CHANGE.value,
                byteArrayOf(ID.hexStringToByteArray().size.toByte()) + ID.hexStringToByteArray() + hexName.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            ), result
        ) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    private val dataSyncCapable = CHDataSynchronizeCapableImpl()
    override fun getFingerPrintDataSyncCapable(): CHDataSynchronizeCapable {
        return dataSyncCapable
    }

    override fun registerEventDelegate(device: CHSesameBiometricBase, delegate: CHFingerPrintDelegate) {
        setupSupport(device)
        val handler = CHFingerPrintEventHandler(delegate)
        eventHandlers[delegate] = handler
        device.addEventHandler(handler)
    }

    override fun unregisterEventDelegate(device: CHSesameBiometricBase, delegate: CHFingerPrintDelegate) {
        if (!isInitialized()) return
        eventHandlers[delegate]?.let { handler ->
            device.removeEventHandler(handler)
            eventHandlers.remove(delegate)
        }
    }
}