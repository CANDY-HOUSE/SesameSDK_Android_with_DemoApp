package co.candyhouse.sesame.ble.os2.base

import co.candyhouse.sesame.ble.CHBaseDevice
import co.candyhouse.sesame.ble.SSM2OpCode
import co.candyhouse.sesame.ble.SSM2ResponsePayload
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.server.CHAPIClientBiz
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.generateRandomData
import kotlinx.coroutines.channels.Channel

internal typealias SesameOS2ResponseCallback = (result: SSM2ResponsePayload) -> Unit

internal open class CHSesameOS2 : CHBaseDevice() {
    var cipher: SesameOS2BleCipher? = null
    var semaphore: Channel<SesameOS2ResponseCallback> = Channel(capacity = 1)
    val mAppToken = generateRandomData(4)

    protected fun reportBatteryData(payloadString: String) {
        CHAPIClientBiz.postBatteryData(deviceId.toString().uppercase(), payloadString) {
            it.onSuccess { resp ->
                batteryPercentage = ((resp.data as? Map<*, *>)?.get("batteryPercentage") as? Number)?.toInt()
                L.d("os2lock", "[reportBatteryData] ${deviceId.toString().uppercase()} $batteryPercentage")
            }
        }
    }
}
internal data class SSM2Payload(val opCode: SSM2OpCode, val itemCode: SesameItemCode, val data: ByteArray) {
    fun toDataWithHeader(): ByteArray {
        return byteArrayOf(opCode.value, itemCode.value.toByte()) + data
    }
}