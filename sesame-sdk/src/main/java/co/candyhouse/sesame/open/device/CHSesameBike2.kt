package co.candyhouse.sesame.open.device

import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.utils.bytesToShort
import java.util.*

interface CHSesameBike2 : CHSesameLock {
    fun unlock(tag: ByteArray? = null, result: CHResult<CHEmpty>)
}

class CHSesameBike2MechStatus(override val data: ByteArray) : CHSesameProtocolMechStatus {
    private val battery = bytesToShort(data[0], data[1])
    private val flags = data[2].toInt()
    override var isInLockRange: Boolean = flags and 2 > 0
    override var isStop: Boolean? = flags and 4 > 0
    override fun getBatteryVoltage(): Float {
        return battery * 2f / 1000f
    }
}



