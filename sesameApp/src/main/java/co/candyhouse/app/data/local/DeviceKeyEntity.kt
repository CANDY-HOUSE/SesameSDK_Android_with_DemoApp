package co.candyhouse.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import co.candyhouse.sesame.ble.CHDevice

@Entity(tableName = "CHDevice")
internal data class DeviceKeyEntity(
    @PrimaryKey val deviceUUID: String,
    val deviceModel: String,
    val historyTag: ByteArray?,
    val keyIndex: String,
    val secretKey: String,
    val sesame2PublicKey: String
) {
    fun toKey() = CHDevice(deviceUUID, deviceModel, historyTag, keyIndex, secretKey, sesame2PublicKey)

    companion object {
        fun from(key: CHDevice) = DeviceKeyEntity(
            key.deviceUUID, key.deviceModel, key.historyTag,
            key.keyIndex, key.secretKey, key.sesame2PublicKey
        )
    }
}
