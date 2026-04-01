package co.candyhouse.sesame.open.devices

import co.candyhouse.sesame.open.devices.base.CHSesameLock
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult

interface CHSesameBike : CHSesameLock {
    fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
}

