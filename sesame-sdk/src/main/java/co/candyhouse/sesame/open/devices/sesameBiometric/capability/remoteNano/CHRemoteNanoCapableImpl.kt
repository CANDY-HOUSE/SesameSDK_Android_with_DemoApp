package co.candyhouse.sesame.open.devices.sesameBiometric.capability.remoteNano

import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.ble.os3.base.SesameOS3Payload
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.baseCapbale.CHAutoInitCapabilityImpl
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.baseCapbale.CHCapabilityHost
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.baseCapbale.CHEventHandlerHost
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.CHResultState

internal class CHRemoteNanoCapableImpl() :
    CHAutoInitCapabilityImpl(),
    CHRemoteNanoCapable {

    private val eventHandlers = mutableMapOf<CHRemoteNanoDelegate, CHRemoteNanoEventHandler>()


    override fun setTriggerDelayTime(time: UByte, result: CHResult<CHEmpty>) {
        sendCommandSafely(
            SesameOS3Payload(
                SesameItemCode.REMOTE_NANO_ITEM_CODE_SET_TRIGGER_DELAYTIME.value,
                byteArrayOf(time.toByte())
            ), result
        ) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun registerEventDelegate(
        device: CHCapabilityHost,
        delegate: CHRemoteNanoDelegate
    ) {
        setupSupport(device)
        val handler = CHRemoteNanoEventHandler(delegate)
        eventHandlers[delegate] = handler
        device.addEventHandler(handler)
    }

    override fun unregisterEventDelegate(delegate: CHRemoteNanoDelegate) {
        if (!isInitialized()) return
        val device = support as? CHEventHandlerHost ?: return
        eventHandlers[delegate]?.let { handler ->
            device.removeEventHandler(handler)
            eventHandlers.remove(delegate)
        }
    }

}