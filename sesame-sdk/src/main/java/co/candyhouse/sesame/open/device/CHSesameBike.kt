package co.candyhouse.sesame.open.device

import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.CHEmpty

interface CHSesameBike : CHSesameLock {
    fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
}

