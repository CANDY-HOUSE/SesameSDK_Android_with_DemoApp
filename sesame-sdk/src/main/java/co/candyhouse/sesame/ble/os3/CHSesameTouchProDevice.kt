package co.candyhouse.sesame.ble.os3

import co.candyhouse.sesame.utils.noHashtoUUID
import co.candyhouse.sesame.utils.toHexString
import java.nio.ByteBuffer
import java.nio.ByteOrder

class CHSesameTouchCard(data: ByteArray) {
    val cardType = data[0]
    val idLength = data[1]
    val cardID = data.sliceArray(2..idLength + 1).toHexString()
    private val nameIndex = idLength + 2
    val nameLength = data[nameIndex]
    val cardName = (data.sliceArray(nameIndex + 1..nameIndex + nameLength)).toHexString()
}

class CHSesameTouchFace(data: ByteArray) {
    val type = data[0]
    val idLength = data[1]
    val id: String = data.sliceArray(2..idLength + 1).toHexString()
    private val nameIndex = idLength + 2
    val nameLength = data[nameIndex]
    var nameUUID: String = (data.sliceArray(nameIndex + 1..nameIndex + nameLength)).toHexString().noHashtoUUID().toString()
    var name: String = ""
}

public data class CHRemoteNanoTriggerSettings(
    var triggerDelaySecond: UByte
) {
    companion object {
        fun fromData(buf: ByteArray): CHRemoteNanoTriggerSettings? {
            val content = buf.copyOf()
            return ByteBuffer.wrap(content)
                .order(ByteOrder.LITTLE_ENDIAN)
                .let {
                    CHRemoteNanoTriggerSettings(
                        triggerDelaySecond = it.get().toUByte()
                    )
                }
        }
    }
}
