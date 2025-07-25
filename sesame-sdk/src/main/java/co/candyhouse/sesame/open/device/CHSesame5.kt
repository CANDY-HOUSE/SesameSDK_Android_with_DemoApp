package co.candyhouse.sesame.open.device

import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.bytesToShort
import co.candyhouse.sesame.utils.bytesToUShort
import co.candyhouse.sesame.utils.toHexString
import java.util.*

interface CHSesame5 : CHSesameLock {
    var mechSetting: CHSesame5MechSettings?
    var opsSetting: CHSesame5OpsSettings?
    fun lock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
    fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
    fun toggle(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
    fun magnet(result: CHResult<CHEmpty>)
    fun configureLockPosition(lockTarget: Short, unlockTarget: Short, result: CHResult<CHEmpty>)
    fun autolock(delay: Int, result: CHResult<Int>)
    fun history(cursor: Long?, uuid: UUID, subUUID: String?, result: CHResult<Pair<List<CHSesame5History>, Long?>>)
    fun opSensorControl(isEnable: Int, result: CHResult<Int>)
}

class CHSesame5MechStatus(override val data: ByteArray) : CHSesameProtocolMechStatus {
    private val battery = bytesToShort(data[0], data[1])
    override val position: Short = bytesToShort(data[4], data[5])
    override val target: Short? = if ((bytesToShort(data[2], data[3]).toInt() == -32768)) null else bytesToShort(data[2], data[3])
    private val flags = data[6].toInt()
    override var isInLockRange: Boolean = flags and 2 > 0
    override var isCritical: Boolean? = flags and 8 > 0
    override var isStop: Boolean? = flags and 16 > 0
    override var isBatteryCritical: Boolean = flags and 32 > 0
    override fun getBatteryVoltage(): Float {
        return battery * 2f / 1000f
    }
}

class CHSesame5MechSettings(data: ByteArray) {
    var lockPosition: Short = bytesToShort(data[0], data[1])
    var unlockPosition: Short = bytesToShort(data[2], data[3])
    var autoLockSecond: Short = bytesToShort(data[4], data[5])
}

class CHSesame5OpsSettings(data: ByteArray) {
    var opsLockSecond: UShort = bytesToUShort(data[0], data[1])
}

sealed class CHSesame5History( timestamp: Long, val recordID: Int, val mechStatus: CHSesame5MechStatus?, var historyTag: ByteArray?) {
    var date: Date = Date(timestamp)

    open class ManualLocked(timestamp: Long, recordID: Int, mechStatus: CHSesame5MechStatus?, histag: ByteArray?) : CHSesame5History(timestamp, recordID, mechStatus, histag)
    open class ManualUnlocked(timestamp: Long, recordID: Int, mechStatus: CHSesame5MechStatus?, histag: ByteArray?) : CHSesame5History(timestamp, recordID, mechStatus, histag)
    open class BLELock(timestamp: Long, recordID: Int, mechStatus: CHSesame5MechStatus?, histag: ByteArray?) : CHSesame5History(timestamp, recordID, mechStatus, histag)
    open class BLEUnlock(timestamp: Long, recordID: Int, mechStatus: CHSesame5MechStatus?, histag: ByteArray?) : CHSesame5History(timestamp, recordID, mechStatus, histag)
    open class AutoLock(timestamp: Long, recordID: Int, mechStatus: CHSesame5MechStatus?, histag: ByteArray?) : CHSesame5History(timestamp, recordID, mechStatus, histag)
    open class WM2Lock(timestamp: Long, recordID: Int, mechStatus: CHSesame5MechStatus?, histag: ByteArray?) : CHSesame5History(timestamp, recordID, mechStatus, histag)
    open class WM2Unlock(timestamp: Long, recordID: Int, mechStatus: CHSesame5MechStatus?, histag: ByteArray?) : CHSesame5History(timestamp, recordID, mechStatus, histag)
    open class WEBLock(timestamp: Long, recordID: Int, mechStatus: CHSesame5MechStatus?, histag: ByteArray?) : CHSesame5History(timestamp, recordID, mechStatus, histag)
    open class WEBUnlock(timestamp: Long, recordID: Int, mechStatus: CHSesame5MechStatus?, histag: ByteArray?) : CHSesame5History(timestamp, recordID, mechStatus, histag)
    open class DoorOpen(timestamp: Long, recordID: Int, mechStatus: CHSesame5MechStatus?, histag: ByteArray?) : CHSesame5History(timestamp, recordID, mechStatus, histag)
    open class DoorClose(timestamp: Long, recordID: Int, mechStatus: CHSesame5MechStatus?, histag: ByteArray?) : CHSesame5History(timestamp, recordID, mechStatus, histag)

}

