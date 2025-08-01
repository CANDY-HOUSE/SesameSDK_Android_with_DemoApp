package co.candyhouse.sesame.ble

import co.candyhouse.sesame.utils.bytesToUShort
import java.util.UUID

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

class SSM3ResponsePayload(val data: ByteArray) {
    val cmdItCode: UByte = data[0].toUByte()
    val cmdResultCode: UByte = data[1].toUByte()
    val payload: ByteArray = data.drop(2).toByteArray()
}

internal enum class SesameResultCode(val value: UByte) {
    success(0U), invalidFormat(1U), notSupported(2U), StorageFail(3U), invalidSig(4U), notFound(5U), UNKNOWN(6U), BUSY(7U), INVALID_PARAM(8U), ;
}

internal enum class SesameItemCode(val value: UByte) {

    none(0u), registration(1u), login(2u), user(3u), history(4u), versionTag(5u), disconnectRebootNow(6u), enableDFU(7u), time(8u), bleConnectionParam(9u), bleAdvParam(10u), autolock(11u), serverAdvKick(12u), ssmtoken(13u), initial(14u), IRER(15u), timePhone(16u), magnet(17u), SSM2_ITEM_CODE_HISTORY_DELETE(18u), SENSOR_INVERVAL(19u), SENSOR_INVERVAL_GET(20u),

    mechSetting(80u), mechStatus(81u), lock(82u), unlock(83u), moveTo(84u), driveDirection(85u), stop(86u), detectDir(87u), toggle(88u), click(89u), DOOR_OPEN(90u), DOOR_CLOSE(91u), OPS_CONTROL(92u), SCRIPT_SETTING(93u), SCRIPT_SELECT(94u), SCRIPT_CURRENT(95u), SCRIPT_NAME_LIST(96u),ADD_SESAME(101u), PUB_KEY_SESAME(102u), REMOVE_SESAME(103u), Reset(104u), NOTIFY_LOCK_DOWN(106u),
    SSM_OS3_CARD_CHANGE(107u), SSM_OS3_CARD_DELETE(108u), SSM_OS3_CARD_GET(109u), SSM_OS3_CARD_NOTIFY(110u), SSM_OS3_CARD_LAST(111u), SSM_OS3_CARD_FIRST(112u), SSM_OS3_CARD_MODE_GET(113u), SSM_OS3_CARD_MODE_SET(114u),
    SSM_OS3_FINGERPRINT_CHANGE(115u), SSM_OS3_FINGERPRINT_DELETE(116u), SSM_OS3_FINGERPRINT_GET(117u), SSM_OS3_FINGERPRINT_NOTIFY(118u), SSM_OS3_FINGERPRINT_LAST(119u), SSM_OS3_FINGERPRINT_FIRST(120u), SSM_OS3_FINGERPRINT_MODE_GET(121u), SSM_OS3_FINGERPRINT_MODE_SET(122u),
    SSM_OS3_PASSCODE_CHANGE(123u), SSM_OS3_PASSCODE_DELETE(124u), SSM_OS3_PASSCODE_GET(125u), SSM_OS3_PASSCODE_NOTIFY(126u), SSM_OS3_PASSCODE_LAST(127u), SSM_OS3_PASSCODE_FIRST(128u), SSM_OS3_PASSCODE_MODE_GET(129u), SSM_OS3_PASSCODE_MODE_SET(130u),
    HUB3_ITEM_CODE_WIFI_SSID(131u), HUB3_ITEM_CODE_SSID_FIRST(132u), HUB3_ITEM_CODE_SSID_NOTIFY(133u), HUB3_ITEM_CODE_SSID_LAST(134u), HUB3_ITEM_CODE_WIFI_PASSWORD(135u), HUB3_UPDATE_WIFI_SSID(136u), HUB3_MATTER_PAIRING_CODE(137u), SSM_OS3_PASSCODE_ADD(138u), SSM_OS3_CARD_CHANGE_VALUE(139u), SSM_OS3_CARD_ADD(140u),SSM_OS3_CARD_MOVE(141u), SSM_OS3_PASSCODE_MOVE(142u),
    SSM_OS3_IR_MODE_SET(143u), SSM_OS3_IR_CODE_CHANGE(144u), SSM_OS3_IR_CODE_EMIT(145u), SSM_OS3_IR_CODE_GET(146u),  SSM_OS3_IR_CODE_LAST(147u), SSM_OS3_IR_CODE_FIRST(148u), SSM_OS3_IR_CODE_DELETE(149u),  SSM_OS3_IR_MODE_GET(150u), SSM_OS3_IR_CODE_NOTIFY(151u),
    HUB3_MATTER_PAIRING_WINDOW(153u),
    SSM_OS3_FACE_CHANGE(154u), SSM_OS3_FACE_DELETE(155u), SSM_OS3_FACE_GET(156u), SSM_OS3_FACE_NOTIFY(157u),
    SSM_OS3_FACE_LAST(158u), SSM_OS3_FACE_FIRST(159u), SSM_OS3_FACE_MODE_GET(160u), SSM_OS3_FACE_MODE_SET(161u),
    SSM_OS3_PALM_CHANGE (162u), SSM_OS3_PALM_DELETE (163u), SSM_OS3_PALM_GET (164u), SSM_OS3_PALM_NOTIFY (165u),
    SSM_OS3_PALM_LAST (166u), SSM_OS3_PALM_FIRST (167u), SSM_OS3_PALM_MODE_GET (168u), SSM_OS3_PALM_MODE_SET (169u),
    BOT2_ITEM_CODE_RUN_SCRIPT_0(170u), BOT2_ITEM_CODE_RUN_SCRIPT_1(171u), BOT2_ITEM_CODE_RUN_SCRIPT_2(172u), BOT2_ITEM_CODE_RUN_SCRIPT_3(173u), BOT2_ITEM_CODE_RUN_SCRIPT_4(174u), BOT2_ITEM_CODE_RUN_SCRIPT_5(175u), BOT2_ITEM_CODE_RUN_SCRIPT_6(176u), BOT2_ITEM_CODE_RUN_SCRIPT_7(177u), BOT2_ITEM_CODE_RUN_SCRIPT_8(178u), BOT2_ITEM_CODE_RUN_SCRIPT_9(179u),
    ADD_HUB3(180u), BOT2_ITEM_CODE_EDIT_SCRIPT(181u),
    STP_ITEM_CODE_CARDS_ADD(182u), STP_ITEM_CODE_DEVICE_STATUS(183u), REMOTE_NANO_ITEM_CODE_SET_TRIGGER_DELAYTIME(190u), REMOTE_NANO_ITEM_CODE_PUB_TRIGGER_DELAYTIME(191u),
    SSM_OS3_FACE_MODE_DELETE_NOTIFY(192u),SSM_OS3_PALM_MODE_DELETE_NOTIFY(193u),SSM_OS3_RADAR_PARAM_SET(200u),SSM_OS3_RADAR_PARAM_PUBLISH(201u),
    SSM3_ITEM_CODE_BATTERY_VOLTAGE(202u),
}
internal enum class Hub3ItemCode(val value: UByte) {
    HUB3_ITEM_CODE_LED_DUTY(92u),
}
internal enum class SSM2OpCode(val value: Byte) {

    create(0x01), read(0x02), update(0x03), delete(0x04), sync(0x05), async(0x06), response(0x07), publish(0x08), undefine(0x10), ;

    companion object {
        private val values = values()
        fun getByValue(value: Byte) = values.first { it.value == value }
    }
}

internal enum class StpItemCode(val value: UByte) {
    STP_ITEM_CODE_CARDS_ADD(182u), STP_ITEM_CODE_CARDS_DELETE(183u), STP_ITEM_CODE_PASSCODES_ADD(184u), STP_ITEM_CODE_PASSCODES_DELETE(185u),
}

internal enum class Sesame2HistoryTypeEnum(var value: Byte) {

    NONE(0), BLE_LOCK(1), BLE_UNLOCK(2), TIME_CHANGED(3), AUTOLOCK_UPDATED(4), MECH_SETTING_UPDATED(5), AUTOLOCK(6), MANUAL_LOCKED(7), MANUAL_UNLOCKED(8), MANUAL_ELSE(9), DRIVE_LOCKED(10), DRIVE_UNLOCKED(11), DRIVE_FAILED(12), BLE_ADV_PARAM_UPDATED(13), WM2_LOCK(14), WM2_UNLOCK(15), WEB_LOCK(16), WEB_UNLOCK(17),
    DOOR_OPEN(90), DOOR_CLOSE(91), ;

    companion object {
        private val values = values()
        fun getByValue(value: Byte) = values.firstOrNull { it.value == value }
    }

}

enum class UUID4HistoryTagTypeEnum(val value: UShort) {
    NAME_UUID_TYPE_ANDROID_USER_BLE_UUID(14U),
    NAME_UUID_TYPE_ANDROID_USER_WIFI_UUID(16U),
    ;

    companion object {
        private val values = UUID4HistoryTagTypeEnum.entries.toTypedArray()
        fun getByValue(value: UShort) = values.firstOrNull { it.value == value }
    }
}

@OptIn(ExperimentalStdlibApi::class)
class UUID4HistoryTag(val data: ByteArray) {

    private var uuid4HistoryTagType: UUID4HistoryTagTypeEnum? = null

    private var uuid4HistoryTagValue: ByteArray = byteArrayOf()
    init {

        uuid4HistoryTagType = UUID4HistoryTagTypeEnum.getByValue(bytesToUShort(data[1], data[0]))
        uuid4HistoryTagValue = if (data.size == 18) data.sliceArray(2..17) else byteArrayOf()
    }




    fun getTagType(): UUID4HistoryTagTypeEnum? {
        return uuid4HistoryTagType
    }

    fun getTagValue(): ByteArray {
        return uuid4HistoryTagValue
    }

    fun isUUID4HistoryTag(): Boolean {

        if (uuid4HistoryTagValue.size != 16) {
            return false
        }
        if (uuid4HistoryTagValue[6].toInt() and 0xF0 != 0x40) {
            return false
        }
        if (uuid4HistoryTagValue[8].toInt() and 0xC0 != 0x80) {
            return false
        }

        return true
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun isDefineHistoryTagType(): Boolean {
        if (uuid4HistoryTagType != null) {
            return true
        }
        return false
    }
}

internal object Sesame2Chracs {
    val uuidService01: UUID = UUID.fromString("0000fd81-0000-1000-8000-00805f9b34fb")
    val uuidChr02: UUID = UUID.fromString("16860002-A5AE-9856-B6D3-DBB4C676993E")
    val uuidChr03: UUID = UUID.fromString("16860003-A5AE-9856-B6D3-DBB4C676993E")
}