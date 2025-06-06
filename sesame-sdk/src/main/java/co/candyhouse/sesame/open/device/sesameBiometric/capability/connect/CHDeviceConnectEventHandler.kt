package co.candyhouse.sesame.open.device.sesameBiometric.capability.connect

import co.candyhouse.sesame.ble.SSM3PublishPayload
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.open.device.CHSesameConnector
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHEventHandler
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.base64decodeHex
import co.candyhouse.sesame.utils.divideArray
import co.candyhouse.sesame.utils.noHashtoUUID

class CHDeviceConnectEventHandler(private val delegate: CHDeviceConnectDelegate?) : CHEventHandler {
    @OptIn(ExperimentalStdlibApi::class)
    override fun handleEvent(device: CHSesameConnector, payload: SSM3PublishPayload): Boolean {
        if (delegate == null) return false

        if (payload.cmdItCode == SesameItemCode.PUB_KEY_SESAME.value) {
            L.d("hcia", "[ds][PUB][KEY]===>:" + payload.payload.toHexString())
            device.ssm2KeysMap.clear()
            val keyDatas = payload.payload.divideArray(23)
            keyDatas.forEach {
                val lock_status = it[22].toInt()
                if (lock_status != 0) {
                    if (it[21].toInt() == 0x00) {
                        val ss5_id = it.sliceArray(IntRange(0, 15))
                        val ssmID = ss5_id.toHexString().noHashtoUUID().toString()
                        device.ssm2KeysMap.put(ssmID, byteArrayOf(0x05, it[22]))
                    } else {
                        val ss2_ir_22 = it.sliceArray(IntRange(0, 21))
                        try {
                            val ssmID = (String(ss2_ir_22) + "==").base64decodeHex().noHashtoUUID().toString()
                            device.ssm2KeysMap.put(ssmID, byteArrayOf(0x04, it[22]))
                        } catch (e: Exception) {
                            L.d("hcia", "🩰  e:" + e)
                        }
                    }
                }
            }
            L.d("hcia", "[TPO][ssm2KeysMap]" + device.ssm2KeysMap)
            delegate.onSSM2KeysChanged(device, device.ssm2KeysMap)
            return true
        } else {
            return false
        }
    }
}