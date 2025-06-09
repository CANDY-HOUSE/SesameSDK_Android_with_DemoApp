package co.candyhouse.sesame.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import co.candyhouse.sesame.ble.UUID4HistoryTagTypeEnum
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.toHexString

@Entity
data class CHDevice(
        @PrimaryKey var deviceUUID: String, // 16
        val deviceModel: String, // 1
        var historyTag: ByteArray? = null, // 默认值设为空
        val keyIndex: String, // 2
        val secretKey: String, // 16
        val sesame2PublicKey: String // 64
)

internal fun CHDevice.createHistag(histag_: ByteArray? = null): ByteArray {
    val histag = histag_ ?: this.historyTag ?: byteArrayOf()
    val limitedHistag = histag.take(21)
    val padding = 22 - limitedHistag.size - 1
    return byteArrayOf(limitedHistag.size.toByte()) + limitedHistag + ByteArray(padding)
}

internal fun CHDevice.createHistagV2(histag_: ByteArray? = null): ByteArray {
    val histag = histag_ ?: this.historyTag ?: byteArrayOf()
    val limitedHistag = histag.take(20) // 统一长度， 客户名称长度超出后， IoT与BLE的Tag，截取长度保持一致。
    return byteArrayOf(limitedHistag.size.toByte()) + limitedHistag
}

internal fun CHDevice.hisTagC(histag_: ByteArray? = null): ByteArray {
    var histag = histag_ ?: this.historyTag ?: byteArrayOf()
    val limitedHistag = histag.take(20)
    return limitedHistag.toByteArray()
}

internal fun CHDevice.historyTagBLE(histag_: ByteArray? = null): ByteArray {
    var histag = histag_ ?: byteArrayOf()
    histag = enumTo16BitBEByteArray(UUID4HistoryTagTypeEnum.NAME_UUID_TYPE_ANDROID_USER_BLE_UUID) + histag
    val limitedHistag = histag.take(20)
    return limitedHistag.toByteArray()
}

internal fun CHDevice.historyTagIOT(histag_: ByteArray? = null): ByteArray {
    var histag = histag_ ?: byteArrayOf()
    histag = enumTo16BitBEByteArray(UUID4HistoryTagTypeEnum.NAME_UUID_TYPE_ANDROID_USER_WIFI_UUID) + histag
    val limitedHistag = histag.take(20)
    return limitedHistag.toByteArray()
}

private fun enumTo16BitBEByteArray(value: UUID4HistoryTagTypeEnum): ByteArray {
    val intValue = value.value.toInt()
    require(intValue in 0..0xFFFF) {
        "Enum value must be within 16-bit unsigned range (0-65535), got $intValue"
    }
    return byteArrayOf((intValue shr 8).toByte(), intValue.toByte())
}