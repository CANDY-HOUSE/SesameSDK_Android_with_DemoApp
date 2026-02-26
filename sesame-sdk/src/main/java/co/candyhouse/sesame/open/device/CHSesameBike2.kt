package co.candyhouse.sesame.open.device

import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.bytesToShort

interface CHSesameBike2 : CHSesameLock {
    var mechSetting: CHSesame5MechSettings?
    fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
    fun onHistoryReceived(historyData: ByteArray){}
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



