package co.candyhouse.sesame.open.device.sesameBiometric.capability.face

import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.ble.os3.base.SesameOS3Payload
import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.CHResultState
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHAutoInitCapabilityImpl
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHDataSynchronizeCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHDataSynchronizeCapableImpl
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBase
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.hexStringToByteArray


internal class CHFaceCapableImpl() :
    CHAutoInitCapabilityImpl(),
    CHDataSynchronizeCapable by CHDataSynchronizeCapableImpl(),
    CHFaceCapable {

    private val eventHandlers = mutableMapOf<CHFaceDelegate, CHFaceEventHandler>()

    override fun faceModeSet(mode: Byte, result: CHResult<CHEmpty>) {
        sendCommandSafely(SesameOS3Payload(SesameItemCode.SSM_OS3_FACE_MODE_SET.value, byteArrayOf(mode)),result) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun faceModeGet(result: CHResult<Byte>) {
        sendCommandSafely(SesameOS3Payload(SesameItemCode.SSM_OS3_FACE_MODE_GET.value, byteArrayOf()),result) { res ->
            if (res.payload.isNotEmpty()) {
                result.invoke(Result.success(CHResultState.CHResultStateBLE(res.payload[0])))
                return@sendCommandSafely
            }
            result.invoke(Result.failure(Exception("Data Error: ${res.payload.toHexString()}")))
        }
    }

    override fun faceListGet(result: CHResult<CHEmpty>) {
        sendCommandSafely(SesameOS3Payload(SesameItemCode.SSM_OS3_FACE_GET.value, byteArrayOf()),result) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    fun faceChange(ID: String, name: String, result: CHResult<CHEmpty>) {
        sendCommandSafely(SesameOS3Payload(SesameItemCode.SSM_OS3_FACE_CHANGE.value, byteArrayOf(ID.hexStringToByteArray().size.toByte()) + ID.hexStringToByteArray() + name.chunked(2).map { it.toInt(16).toByte() }.toByteArray()),result) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun faceDelete(faceID: String, deviceID: String, result: CHResult<CHEmpty>) {
        sendCommandSafely(SesameOS3Payload(SesameItemCode.SSM_OS3_FACE_DELETE.value, byteArrayOf(faceID.toInt(16).toByte())),result) {
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    private val dataSyncCapable = CHDataSynchronizeCapableImpl()
    override fun getFaceDataSyncCapable(): CHDataSynchronizeCapable {
        return dataSyncCapable
    }

    override fun registerEventDelegate(device: CHSesameBiometricBase, delegate: CHFaceDelegate) {
        setupSupport(device)
        val handler = CHFaceEventHandler(delegate)
        eventHandlers[delegate] = handler
        device.addEventHandler(handler)
    }

    override fun unregisterEventDelegate(device: CHSesameBiometricBase, delegate: CHFaceDelegate) {
        if (!isInitialized() || support !is CHSesameBiometricBase) return

        val device = support as CHSesameBiometricBase
        eventHandlers[delegate]?.let { handler ->
            device.removeEventHandler(handler)
            eventHandlers.remove(delegate)
        }
    }

}