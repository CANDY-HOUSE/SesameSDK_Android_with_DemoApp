package co.candyhouse.sesame.open.devices.sesameBiometric.capability.palm

import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.ble.os3.base.SesameOS3Payload
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.baseCapbale.CHAutoInitCapabilityImpl
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.baseCapbale.CHCapabilityHost
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.baseCapbale.CHDataSynchronizeCapable
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.baseCapbale.CHDataSynchronizeCapableImpl
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.CHResultState

internal class CHPalmCapableImpl() :
    CHAutoInitCapabilityImpl(),
    CHPalmCapable {

    private val eventHandlers = mutableMapOf<CHPalmDelegate, CHPalmEventHandler>()

    override fun palmModeSet(mode: Byte, result: CHResult<CHEmpty>) {
        sendCommandSafely(SesameOS3Payload(SesameItemCode.SSM_OS3_PALM_MODE_SET.value, byteArrayOf(mode)), result) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun palmModeGet(result: CHResult<Byte>) {
        sendCommandSafely(SesameOS3Payload(SesameItemCode.SSM_OS3_PALM_MODE_GET.value, byteArrayOf()), result) { res ->
            if (res.payload.isNotEmpty()) {
                result.invoke(Result.success(CHResultState.CHResultStateBLE(res.payload[0])))
                return@sendCommandSafely
            }
            result.invoke(Result.failure(Exception("Data Error: ${res.payload.toHexString()}")))
        }
    }

    override fun palmListGet(result: CHResult<CHEmpty>) {
        sendCommandSafely(SesameOS3Payload(SesameItemCode.SSM_OS3_PALM_GET.value, byteArrayOf()), result) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun palmDelete(palmID: String, deviceId: String, result: CHResult<CHEmpty>) {
        sendCommandSafely(SesameOS3Payload(SesameItemCode.SSM_OS3_PALM_DELETE.value, byteArrayOf(palmID.toInt(16).toByte())), result) {
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    private val dataSyncCapable = CHDataSynchronizeCapableImpl()
    override fun getPalmDataSyncCapable(): CHDataSynchronizeCapable {
        return dataSyncCapable
    }

    override fun registerEventDelegate(device: CHCapabilityHost, delegate: CHPalmDelegate) {
        setupSupport(device)
        val handler = CHPalmEventHandler(delegate)
        eventHandlers[delegate] = handler
        device.addEventHandler(handler)
    }

    override fun unregisterEventDelegate(device: CHCapabilityHost, delegate: CHPalmDelegate) {
        if (!isInitialized()) return
        eventHandlers[delegate]?.let { handler ->
            device.removeEventHandler(handler)
            eventHandlers.remove(delegate)
        }
    }
}