package co.candyhouse.sesame.db.model

import androidx.room.*
import co.candyhouse.sesame.utils.L

@Entity
data class CHDevice(
        @PrimaryKey var deviceUUID: String//16
        , val deviceModel: String//1
        , var historyTag: ByteArray?, val keyIndex: String//2
        , val secretKey: String//16
        , val sesame2PublicKey: String//64
)


internal fun CHDevice.createHistag(histag_: ByteArray?): ByteArray {
    var histag = histag_ ?: this.historyTag ?: byteArrayOf()
    if (histag.size > 21) {
        histag = histag.sliceArray(0..20)
    }
    val tagCount = histag.size
    val padding = 22 - tagCount - 1// -1 for tagCount need onebyte
    return byteArrayOf(tagCount.toByte()) + histag + ByteArray(padding)
}
internal fun CHDevice.createHistagV2(histag_: ByteArray?): ByteArray {
    var histag = histag_ ?: this.historyTag ?: byteArrayOf()
    if (histag.size > 29) {
        histag = histag.sliceArray(0..29)
    }/// 根據server解析協議。不能超過30 以區別來源
//    L.d("hcia", "[histag]:" + histag.size)
    return byteArrayOf(histag.size.toByte()) + histag
}

internal fun CHDevice.hisTagC(histag_: ByteArray?): ByteArray {
    return histag_ ?: this.historyTag ?: byteArrayOf()
}

