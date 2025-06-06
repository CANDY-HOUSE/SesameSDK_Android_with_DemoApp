package co.candyhouse.sesame.open.device

import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.utils.bytesToShort

/**
 * 动作类型：正传、反转、停止（无惯性）、睡眠（有惯性）
 */
enum class BotActionType(val value: UByte) {
    FORWARD(0u),
    REVERSE(1u),
    STOP(2u),
    SLEEP(3u);
    companion object {
        fun fromValue(value: UByte): BotActionType? = values().find { it.value == value }
    }
}

/**
 * 脚本动作，最多可以有20个
 */
data class Bot2Action(
        val action: BotActionType,
        val time: UByte
)

/**
 * 结构体转 ByteArray
 */
fun Bot2Action.toByteArray(): ByteArray {
    return byteArrayOf(action.value.toByte(), time.toByte())
}

/**
 * 脚本结构
 */
data class CHSesamebot2Event(
        var nameLength: UByte,
        val name: ByteArray,
        var actionLength: UByte? = null,
        var actions: List<Bot2Action>? = null
) {
    companion object {
        fun fromByteArray(buf: ByteArray): CHSesamebot2Event? {
            var cursor = 0
            val nameLength = buf[cursor].toUByte()
            if (nameLength < 1u) return null
            cursor++
            val name = buf.copyOfRange(cursor, cursor + nameLength.toInt())
            cursor += 20
            val event = CHSesamebot2Event(nameLength, name)
            val actionLength = buf[cursor].toUByte()
            if (actionLength == 0u.toUByte()) return event
            val actions = mutableListOf<Bot2Action>()
            while (cursor < buf.size - 1) {
                cursor++
                val action = BotActionType.fromValue(buf[cursor].toUByte())
                cursor++
                val time = buf[cursor].toUByte()
                actions.add(Bot2Action(action!!, time))
            }
            event.actionLength = actionLength
            event.actions = actions
            return event
        }
    }

    fun toByteArray(): ByteArray {
        val result = mutableListOf<Byte>()
        result.add(nameLength.toByte())
        val nameData = name + ByteArray(20 - name.size)
        result.addAll(nameData.toList())
        result.add(actionLength!!.toByte())
        actions?.forEach { action ->
            result.addAll(action.toByteArray().toList())
        }
        return result.toByteArray()
    }
}

/**
 * 脚本列表结构（action 只包含 name、nameLength）
 */
data class CHSesamebot2Status(
    var curIdx: UByte,
    val eventLength: UByte,
    val events: List<CHSesamebot2Event>
) {
    companion object {
        fun fromByteArray(buf: ByteArray): CHSesamebot2Status? {
            var cursor = 0
            val curIdx = buf[cursor].toUByte()
            cursor++
            val eventLength = buf[cursor].toUByte()
            if (curIdx >= eventLength) return null
            cursor++
            val events = mutableListOf<CHSesamebot2Event>()
            repeat(eventLength.toInt()) {
                val nameLength = maxOf(buf[cursor].toUByte(), 1u.toUByte())
                cursor++
                val name = buf.copyOfRange(cursor, cursor + nameLength.toInt())
                events.add(CHSesamebot2Event(nameLength, name))
                cursor += 20
            }
            return CHSesamebot2Status(curIdx, eventLength, events)
        }
    }
}

interface CHSesameBot2 : CHSesameLock {

    var scripts: CHSesamebot2Status
    fun click(index: UByte? = null, result: CHResult<CHEmpty>)
    fun sendClickScript(index: UByte, script: ByteArray, result: CHResult<CHEmpty>)
    fun selectScript(index: UByte, result: CHResult<CHEmpty>)
    fun getCurrentScript(index: UByte?, result: CHResult<CHSesamebot2Event>)
    fun getScriptNameList(result: CHResult<CHSesamebot2Status>)
}

class CHSesameBot2MechStatus(override val data: ByteArray) : CHSesameProtocolMechStatus {
    private val battery = bytesToShort(data[0], data[1])
    private val flags = data[2].toInt()
    override var isInLockRange: Boolean = flags and 2 > 0
    override var isStop: Boolean? = flags and 4 > 0
    override fun getBatteryVoltage(): Float {
        return battery * 2f / 1000f
    }
}




