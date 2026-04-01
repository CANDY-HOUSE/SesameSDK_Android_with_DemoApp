package co.candyhouse.sesame.open.devices.sesameBiometric.capability.palm

import co.candyhouse.sesame.ble.SSM3PublishPayload
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.open.devices.base.CHSesameConnector
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.baseCapbale.CHEventHandler
import co.candyhouse.sesame.open.devices.sesameBiometric.parseData.CHSesameTouchFace

class CHPalmEventHandler(private val delegate: CHPalmDelegate?)  : CHEventHandler {
    @OptIn(ExperimentalStdlibApi::class)
    override fun handleEvent(device: CHDevices, payload: SSM3PublishPayload): Boolean {
        if (delegate == null) return false

        when (payload.cmdItCode) {
            SesameItemCode.SSM_OS3_PALM_CHANGE.value -> {
                delegate.onPalmChanged(device, CHSesameTouchFace(payload.payload))
                return true
            }
            SesameItemCode.SSM_OS3_PALM_FIRST.value -> {
                delegate.onPalmReceiveStart(device)
                return true
            }
            SesameItemCode.SSM_OS3_PALM_NOTIFY.value -> {
                delegate.onPalmReceive(device, CHSesameTouchFace(payload.payload))
                return true
            }
            SesameItemCode.SSM_OS3_PALM_LAST.value -> {
                delegate.onPalmReceiveEnd(device)
                return true
            }
            SesameItemCode.SSM_OS3_PALM_MODE_SET.value -> {
                delegate.onPalmModeChanged(device, payload.payload[0])
                return true
            }
            SesameItemCode.SSM_OS3_PALM_MODE_DELETE_NOTIFY.value -> {
                if (payload.payload.size >= 2) {
                    val faceID = payload.payload[0].toByte()
                    val isSuccess = payload.payload[1] == 0.toByte()
                    delegate.onPalmDeleted(device, faceID, isSuccess)
                }
                return true
            }
            else -> return false
        }
    }
}