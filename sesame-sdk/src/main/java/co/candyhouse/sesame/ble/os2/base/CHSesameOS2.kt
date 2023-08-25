package co.candyhouse.sesame.ble.os2.base

import co.candyhouse.sesame.ble.*
import co.candyhouse.sesame.ble.CHBaseDevice
import co.candyhouse.sesame.utils.*
import kotlinx.coroutines.channels.Channel

internal typealias SesameOS2ResponseCallback = (result: SSM2ResponsePayload) -> Unit

internal open class CHSesameOS2 : CHBaseDevice() {
    var cipher: SesameOS2BleCipher? = null
    var semaphore: Channel<SesameOS2ResponseCallback> = Channel(capacity = 1)
    val mAppToken = generateRandomData(4)
}
internal data class SSM2Payload(val opCode: SSM2OpCode, val itemCode: SesameItemCode, val data: ByteArray) {
    fun toDataWithHeader(): ByteArray {
        return byteArrayOf(opCode.value, itemCode.value.toByte()) + data
    }
}