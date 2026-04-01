package co.candyhouse.sesame.open.devices

import co.candyhouse.sesame.open.devices.base.CHSesameLock
import co.candyhouse.sesame.open.devices.base.CHSesameProtocolMechStatus
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult

interface CHSesameBike2 : CHSesameLock {
    var mechSetting: CHSesame5MechSettings?
    fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
    fun onHistoryReceived(historyData: ByteArray) {}
}

class CHSesameBike2MechStatus(override val data: ByteArray) : CHSesameProtocolMechStatus {
    private val flags = data[2].toInt()
    override var isInLockRange: Boolean = flags and 2 > 0
    override var isStop: Boolean? = flags and 4 > 0
}