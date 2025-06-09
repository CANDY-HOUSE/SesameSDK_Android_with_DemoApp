package co.candyhouse.sesame.ble.os3

import co.candyhouse.sesame.utils.hexStringToByteArray
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

class CHSesameTouchFace {
    val type: Byte
    val idLength: Byte
    val id: String
    private val nameIndex: Int
    val nameLength: Byte
    var nameUUID: String
    var name: String

    constructor(data: ByteArray) {
        this.type = data[0]
        this.idLength = data[1]
        this.id = data.sliceArray(2..idLength + 1).toHexString()
        this.nameIndex = idLength + 2
        this.nameLength = data[nameIndex]
        this.nameUUID = (data.sliceArray(nameIndex + 1..nameIndex + nameLength)).toHexString().noHashtoUUID().toString()
        this.name = ""
    }

    constructor(id: String, name: String, type: Byte, nameUUID: String) {
        this.type = type
        this.id = id
        this.idLength = id.hexStringToByteArray().size.toByte()
        this.name = name
        this.nameUUID = nameUUID
        this.nameLength = getNameLength(nameUUID)
        this.nameIndex = idLength + 2
    }

    private fun getNameLength(nameUUID: String): Byte {
        return try {
            // 尝试作为十六进制字符串处理
            nameUUID.replace("-", "").hexStringToByteArray().size.toByte()
        } catch (e: NumberFormatException) {
            // 如果不是有效的十六进制字符串，则使用UTF-8编码的字节长度
            nameUUID.toByteArray(Charsets.UTF_8).size.toByte()
        }
    }
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
