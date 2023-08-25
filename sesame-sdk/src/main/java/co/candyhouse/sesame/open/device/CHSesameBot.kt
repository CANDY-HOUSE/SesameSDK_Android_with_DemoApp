package co.candyhouse.sesame.open.device

import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.utils.bytesToShort

interface CHSesameBot : CHSesameLock {
    var mechSetting: CHSesameBotMechSettings?
    fun updateSetting(setting: CHSesameBotMechSettings, historyTag: ByteArray? = null, result: CHResult<CHEmpty>)
    fun toggle(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)
    fun lock(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)
    fun unlock(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)
    fun click(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)
}

data class CHSesameBotMechSettings(var userPrefDir: Byte, var lockSec: Byte, var unlockSec: Byte, var clickLockSec: Byte, var clickHoldSec: Byte, var clickUnlockSec: Byte, var buttonMode: Byte) {
    internal fun data(): ByteArray =
        byteArrayOf(userPrefDir, lockSec, unlockSec, clickLockSec, clickHoldSec, clickUnlockSec, buttonMode) + byteArrayOf(0, 0, 0, 0, 0)
}

class CHSesameBotMechStatus(override val data: ByteArray) : CHSesameProtocolMechStatus {
    private val battery = bytesToShort(data[0], data[1])
    internal val motorStatus = data[4]
    private val flags = data[7].toInt()
    override var isInLockRange: Boolean = flags and 2 > 0
    override var isInUnlockRange: Boolean = flags and 4 > 0
    override var isBatteryCritical: Boolean = flags and 32 > 0
    override var isStop: Boolean? = (flags and 1 == 0)

    override fun getBatteryVoltage(): Float {
        return battery * 7.2f / 1023
    }

}
