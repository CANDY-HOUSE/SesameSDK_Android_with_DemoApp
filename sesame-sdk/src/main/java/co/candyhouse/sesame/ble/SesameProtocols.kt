package co.candyhouse.sesame.ble

import java.util.*

class SSM3PublishPayload(val data: ByteArray) {
    val cmdItCode = data[0].toUByte()
    val payload: ByteArray = data.drop(1).toByteArray()
}

internal class SesameNotifypayload(val data: ByteArray) {
    val notifyOpCode: SSM2OpCode = SSM2OpCode.getByValue(data[0])
    val payload: ByteArray = data.drop(1).toByteArray()
}

internal class SSM2ResponsePayload(val data: ByteArray) {
    val cmdItCode: UByte = data[0].toUByte()// login
    val cmdOPCode: UByte = data[1].toUByte()// sync
    val cmdResultCode: UByte = data[2].toUByte()// success
    val payload: ByteArray = data.drop(3).toByteArray()
}

internal class SSM3ResponsePayload(val data: ByteArray) {
    val cmdItCode: UByte = data[0].toUByte()
    val cmdResultCode: UByte = data[1].toUByte()
    val payload: ByteArray = data.drop(2).toByteArray()
}

internal enum class SesameResultCode(val value: UByte) {
    success(0U), invalidFormat(1U), notSupported(2U), StorageFail(3U), invalidSig(4U), notFound(5U), UNKNOWN(
        6U
    ),
    BUSY(7U), INVALID_PARAM(8U), ;
}

internal enum class SesameItemCode(val value: UByte) {

    none(0u), registration(1u), login(2u), user(3u), history(4u), versionTag(5u), disconnectRebootNow(
        6u
    ),
    enableDFU(7u), time(8u), bleConnectionParam(9u), bleAdvParam(10u), autolock(11u), serverAdvKick(
        12u
    ),
    ssmtoken(13u), initial(14u), IRER(15u), timePhone(16u),
    magnet(17u), BLE_ADV_PARAM_GET(18u), SENSOR_INVERVAL(19u), SENSOR_INVERVAL_GET(20u), mechSetting(
        80u
    ),
    mechStatus(81u), lock(82u), unlock(83u), moveTo(84u), driveDirection(85u), stop(86u), detectDir(
        87u
    ),
    toggle(88u), click(89u), ADD_SESAME(101u), PUB_KEY_SESAME(102u), REMOVE_SESAME(103u), Reset(104u), NOTIFY_LOCK_DOWN(
        106u
    ),

    SSM_OS3_CARD_CHANGE(107u), SSM_OS3_CARD_DELETE(108u), SSM_OS3_CARD_GET(109u), SSM_OS3_CARD_NOTIFY(
        110u
    ),
    SSM_OS3_CARD_LAST(111u), SSM_OS3_CARD_FIRST(112u), SSM_OS3_CARD_MODE_GET(113u), SSM_OS3_CARD_MODE_SET(
        114u
    ),

    SSM_OS3_FINGERPRINT_CHANGE(115u), SSM_OS3_FINGERPRINT_DELETE(116u), SSM_OS3_FINGERPRINT_GET(117u), SSM_OS3_FINGERPRINT_NOTIFY(
        118u
    ),
    SSM_OS3_FINGERPRINT_LAST(119u), SSM_OS3_FINGERPRINT_FIRST(120u),
    SSM_OS3_FINGERPRINT_MODE_GET(121u), SSM_OS3_FINGERPRINT_MODE_SET(122u),

    SSM_OS3_PASSCODE_CHANGE(123u), SSM_OS3_PASSCODE_DELETE(124u), SSM_OS3_PASSCODE_GET(125u), SSM_OS3_PASSCODE_NOTIFY(
        126u
    ),
    SSM_OS3_PASSCODE_LAST(127u), SSM_OS3_PASSCODE_FIRST(128u), SSM_OS3_PASSCODE_MODE_GET(129u), SSM_OS3_PASSCODE_MODE_SET(
        130u
    ), ;
}

internal enum class SSM2OpCode(val value: Byte) {

    create(0x01), read(0x02), update(0x03), delete(0x04), sync(0x05), async(0x06), response(0x07), publish(
        0x08
    ),
    undefine(0x10), ;

    companion object {
        private val values = values()
        fun getByValue(value: Byte) = values.first { it.value == value }
    }
}


internal enum class Sesame2HistoryTypeEnum(var value: Byte) {

    NONE(0), BLE_LOCK(1), BLE_UNLOCK(2), TIME_CHANGED(3), AUTOLOCK_UPDATED(4), MECH_SETTING_UPDATED(
        5
    ),
    AUTOLOCK(6), MANUAL_LOCKED(7), MANUAL_UNLOCKED(8), MANUAL_ELSE(9), DRIVE_LOCKED(10), DRIVE_UNLOCKED(
        11
    ),
    DRIVE_FAILED(12), BLE_ADV_PARAM_UPDATED(13), WM2_LOCK(14), WM2_UNLOCK(15), WEB_LOCK(16), WEB_UNLOCK(
        17
    ), ;

    companion object {
        private val values = values()
        fun getByValue(value: Byte) = values.firstOrNull { it.value == value }
    }

}

internal object Sesame2Chracs {
    val uuidService01: UUID = UUID.fromString("0000fd81-0000-1000-8000-00805f9b34fb")
    val uuidChr02: UUID = UUID.fromString("16860002-A5AE-9856-B6D3-DBB4C676993E")
    val uuidChr03: UUID = UUID.fromString("16860003-A5AE-9856-B6D3-DBB4C676993E")
}