package co.candyhouse.sesame.ble.os2.bike

import co.candyhouse.sesame.ble.CHSesameLock
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult

interface CHSesameBike : CHSesameLock {
    fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
}
