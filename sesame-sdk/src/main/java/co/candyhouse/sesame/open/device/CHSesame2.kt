package co.candyhouse.sesame.open.device

import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.bytesToShort
import co.candyhouse.sesame.utils.toReverseBytes

interface CHSesame2 : CHSesameLock { // CHProductModel.SS2,CHProductModel.SS4
    var mechSetting: CHSesame2MechSettings?
    fun lock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
    fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
    fun toggle(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
    fun configureLockPosition(lockTarget: Short, unlockTarget: Short, result: CHResult<CHEmpty>)
    fun getAutolockSetting(result: CHResult<Int>)
    fun enableAutolock(delay: Int, historytag: ByteArray? = null, result: CHResult<Int>)
    fun disableAutolock(historytag: ByteArray? = null, result: CHResult<Int>)
    fun onHistoryReceived(historyData: ByteArray){}
}


class CHSesame2MechSettings(data: ByteArray) {
    val lockPosition: Short = (bytesToShort(data[0], data[1]).toInt() * 360 / 1024).toShort()
    val unlockPosition: Short = (bytesToShort(data[2], data[3]).toInt() * 360 / 1024).toShort()
    val isConfigured: Boolean = (lockPosition != unlockPosition)
}

class CHSesame2MechStatus(override val data: ByteArray) : CHSesameProtocolMechStatus {

    private val battery = bytesToShort(data[0], data[1])
    override val position: Short = (bytesToShort(data[4], data[5]).toInt() * 360 / 1024).toShort()
    override val target: Short? = if((bytesToShort(data[2], data[3]).toInt() == -32768) ) null else (bytesToShort(data[2], data[3]).toInt() * 360 / 1024).toShort()
    internal val retCode = data[6].toInt()

    private val flags = data[7].toInt()
    override var isInLockRange: Boolean = flags and 2 > 0
    override var isInUnlockRange: Boolean = flags and 4 > 0
    override var isBatteryCritical: Boolean = flags and 32 > 0
    override var isStop: Boolean? = null

    override fun getBatteryVoltage(): Float {
//        L.d("hcia", "[ss4][vol]" + battery * 7.2f / 1023)
        return battery * 7.2f / 1023
    }

    fun ss5Adapter(): ByteArray {
        val bat = (bytesToShort(data[0], data[1]).toInt() * 3600 / 1023).toShort()
        L.d("hub3_ss5", "bat: $bat")
        val position = (bytesToShort(data[4], data[5]).toInt() * 360 / 1024).toShort()
        L.d("hub3_ss5", "position: $position")

        return bat.toReverseBytes() + data.sliceArray(2..3) + position.toReverseBytes() + data.sliceArray(7..7)
    }
}