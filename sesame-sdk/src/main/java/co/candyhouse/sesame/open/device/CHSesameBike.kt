package co.candyhouse.sesame.open.device

import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.server.dto.CHEmpty

interface CHSesameBike : CHSesameLock {
    fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
}

