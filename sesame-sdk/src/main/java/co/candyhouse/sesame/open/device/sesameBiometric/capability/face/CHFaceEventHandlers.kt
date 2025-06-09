package co.candyhouse.sesame.open.device.sesameBiometric.capability.face

import co.candyhouse.sesame.ble.SSM3PublishPayload
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.ble.os3.CHSesameTouchFace
import co.candyhouse.sesame.open.device.CHSesameConnector
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHEventHandler
import co.candyhouse.sesame.utils.L

class CHFaceEventHandler(private val delegate: CHFaceDelegate?) : CHEventHandler {
    @OptIn(ExperimentalStdlibApi::class)
    override fun handleEvent(device: CHSesameConnector, payload: SSM3PublishPayload): Boolean {
        if (delegate == null) return false

        when (payload.cmdItCode) {
            SesameItemCode.SSM_OS3_FACE_CHANGE.value -> {
                L.d("harry", "SSM_OS3_FACE_CHANGE data: " + payload.payload.toHexString())
                delegate.onFaceReceive(device, CHSesameTouchFace(payload.payload))
                return true
            }
            SesameItemCode.SSM_OS3_FACE_NOTIFY.value -> {
                L.d("harry", "SSM_OS3_FACE_NOTIFY data: " + payload.payload.toHexString())
                delegate.onFaceReceive(device, CHSesameTouchFace(payload.payload))
                return true
            }
            SesameItemCode.SSM_OS3_FACE_FIRST.value -> {
                delegate.onFaceReceiveStart(device)
                return true
            }
            SesameItemCode.SSM_OS3_FACE_LAST.value -> {
                delegate.onFaceReceiveEnd(device)
                return true
            }
            SesameItemCode.SSM_OS3_FACE_MODE_SET.value -> {
                L.d("hcia", "SSM_OS3_FACE_MODE_SET : " + payload.payload.toHexString())
                delegate.onFaceModeChanged(device, payload.payload[0])
                return true
            }
            SesameItemCode.SSM_OS3_FACE_DELETE.value -> {
                return true
            }
            SesameItemCode.SSM_OS3_FACE_MODE_DELETE_NOTIFY.value -> {
                if (payload.payload.size >= 2) {
                    val faceID = payload.payload[0].toByte()
                    val isSuccess = payload.payload[1] == 0.toByte()
                    delegate.onFaceDeleted(device, faceID, isSuccess)
                }
                return true
            }
            else -> return false
        }
    }
}