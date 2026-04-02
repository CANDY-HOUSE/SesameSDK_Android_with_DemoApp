package co.candyhouse.sesame.open.devices.sesameBiometric.parseData

import co.candyhouse.sesame.open.devices.base.CHSesameProtocolMechStatus
import co.candyhouse.sesame.utils.hexStringToByteArray
import co.candyhouse.sesame.utils.noHashtoUUID
import com.google.gson.Gson
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

data class CHRemoteNanoTriggerSettings(
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

class CHSesameTouchProMechStatus(override val data: ByteArray) : CHSesameProtocolMechStatus {}

/** Open sensor */
class CHSesameOpenSensorMechStatus(openSensorData: OpenSensorData) : CHSesameProtocolMechStatus {
    override val data: ByteArray = openSensorData.toByteArray()
    private val battery: Short? = openSensorData.Battery
    private val lightLoadBatteryVoltage: Short? = openSensorData.lightLoadBatteryVoltage_mV
    private val heavyLoadBatteryVoltage: Short? = openSensorData.heavyLoadBatteryVoltage_mV
}

data class OpenSensorData(
    val Status: String,
    val TimeStamp: Long,
    val Battery: Short?,
    val lightLoadBatteryVoltage_mV: Short?,
    val heavyLoadBatteryVoltage_mV: Short?,
) {
    fun toByteArray(): ByteArray {
        val gson = Gson()
        return gson.toJson(this).toByteArray(Charsets.UTF_8)
    }

    companion object {
        fun fromByteArray(byteArray: ByteArray): OpenSensorData {
            val gson = Gson()
            val jsonString = String(byteArray, Charsets.UTF_8)
            return gson.fromJson(jsonString, OpenSensorData::class.java)
        }
    }
}