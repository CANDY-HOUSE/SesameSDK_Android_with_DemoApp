package co.candyhouse.sesame.open.device.sesameBiometric.capability.fingerPrint

import co.candyhouse.sesame.ble.SSM3PublishPayload
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.ble.os3.CHSesameTouchCard
import co.candyhouse.sesame.open.device.CHSesameConnector
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHEventHandler
import co.candyhouse.sesame.utils.L

class CHFingerPrintEventHandler(private val delegate: CHFingerPrintDelegate?) : CHEventHandler {
    @OptIn(ExperimentalStdlibApi::class)
    override fun handleEvent(device: CHSesameConnector, payload: SSM3PublishPayload): Boolean {
        if (delegate == null) return false

        when (payload.cmdItCode) {
            SesameItemCode.SSM_OS3_FINGERPRINT_CHANGE.value -> {
                val card = CHSesameTouchCard(payload.payload)
                delegate.onFingerPrintChanged(device, card.cardID, card.cardName, card.cardType)
                return true
            }
            SesameItemCode.SSM_OS3_FINGERPRINT_FIRST.value -> {
                delegate.onFingerPrintReceiveStart(device)
                return true
            }
            SesameItemCode.SSM_OS3_FINGERPRINT_LAST.value -> {
                delegate.onFingerPrintReceiveEnd(device)
                return true
            }
            SesameItemCode.SSM_OS3_FINGERPRINT_NOTIFY.value -> {
                val card = CHSesameTouchCard(payload.payload)
                delegate.onFingerPrintReceive(device, card.cardID, card.cardName, card.cardType)
                return true
            }
            SesameItemCode.SSM_OS3_FINGERPRINT_MODE_SET.value -> {
                L.d("hcia", "SSM_OS3_FINGERPRINT_MODE_SET : " + payload.payload.toHexString())
                delegate.onFingerModeChange(device, payload.payload[0])
                return true
            }
            SesameItemCode.SSM_OS3_FINGERPRINT_DELETE.value -> {
                L.d("hcia", "SSM_OS3_FINGERPRINT_DELETE : " + payload.payload.toHexString())
                delegate.onFingerDelete(device, payload.payload.toHexString())
                return true
            }
            else -> return false
        }
    }
}