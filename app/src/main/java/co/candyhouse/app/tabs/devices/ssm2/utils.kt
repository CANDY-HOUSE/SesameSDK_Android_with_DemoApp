package co.candyhouse.app.tabs.devices.ssm2

import androidx.fragment.app.Fragment
import co.candyhouse.app.R
import co.candyhouse.app.tabs.MainActivity
import co.candyhouse.sesame.open.device.*
import co.utils.SharedPreferencesUtils
import kotlin.math.pow

fun ssm5UIParser(device: CHSesameBot): Int {

//    L.d("hcia", "[ssm5UIParser bot!]deviceShadowStatus:" + device.deviceShadowStatus + " deviceStatus:" + device.deviceStatus)
    if (device.deviceShadowStatus != null) {
        if (device.deviceShadowStatus == CHDeviceStatus.Locked) {
            return R.drawable.swtich_locked
        }
        if (device.deviceShadowStatus == CHDeviceStatus.Unlocked) {
            return R.drawable.swtich_unlocked
        }
        if (device.deviceShadowStatus == CHDeviceStatus.Moved) {
            return R.drawable.swtich_unlocked
        }
    }


    return when (device.deviceStatus) {
        CHDeviceStatus.DfuMode -> R.drawable.swtich_no_ble
        CHDeviceStatus.NoBleSignal -> R.drawable.swtich_no_ble
        CHDeviceStatus.ReceivedAdV -> R.drawable.swtich_receive_ble
        CHDeviceStatus.BleConnecting -> R.drawable.swtich_receive_ble
        CHDeviceStatus.DiscoverServices -> R.drawable.swtich_waitgatt
        CHDeviceStatus.BleLogining -> R.drawable.swtich_logining
        CHDeviceStatus.ReadyToRegister -> R.drawable.swtich_no_ble
        CHDeviceStatus.Locked -> R.drawable.swtich_locked
        CHDeviceStatus.Unlocked -> R.drawable.swtich_unlocked
        CHDeviceStatus.Reset -> R.drawable.swtich_no_ble
        CHDeviceStatus.Registering -> R.drawable.swtich_logining
        CHDeviceStatus.WaitingForAuth -> R.drawable.icon_waitgatt
        else -> R.drawable.ic_icons_outlined_setting
    }

}

fun ssm5UIParser(device: CHDevices): Int {

    if (device is CHSesameBot) {
        return ssm5UIParser(device)
    }
//    L.d("hcia", "ssm5UIParser deviceShadowStatus:" + device.deviceShadowStatus + " deviceStatus:" + device.deviceStatus)
    if (device is CHSesameLock) {
        if (device.deviceShadowStatus != null) {
            if (device.deviceShadowStatus == CHDeviceStatus.Locked) {
                return R.drawable.icon_lock
            }
            if (device.deviceShadowStatus == CHDeviceStatus.Unlocked) {
                return R.drawable.icon_unlock
            }
            if (device.deviceShadowStatus == CHDeviceStatus.Moved) {
                return R.drawable.icon_unlock
            }
        }
    }

    return when (device.deviceStatus) {
        CHDeviceStatus.DfuMode -> R.drawable.icon_nosignal
        CHDeviceStatus.NoBleSignal -> R.drawable.icon_nosignal
        CHDeviceStatus.ReceivedAdV -> R.drawable.icon_receiveblee
        CHDeviceStatus.BleConnecting -> R.drawable.icon_receiveblee
        CHDeviceStatus.DiscoverServices -> R.drawable.icon_waitgatt
        CHDeviceStatus.BleLogining -> R.drawable.icon_logining
        CHDeviceStatus.ReadyToRegister -> R.drawable.icon_nosignal
        CHDeviceStatus.Locked -> R.drawable.icon_lock
        CHDeviceStatus.Unlocked -> R.drawable.icon_unlock
        CHDeviceStatus.NoSettings -> R.drawable.icon_nosetting
        CHDeviceStatus.Moved -> R.drawable.icon_unlock
        CHDeviceStatus.Reset -> R.drawable.icon_nosignal
        CHDeviceStatus.Registering -> R.drawable.icon_logining
        CHDeviceStatus.WaitingForAuth -> R.drawable.icon_waitgatt
        else -> R.drawable.ic_icons_outlined_setting
    }
}


fun CHDevices.setIsJustRegister(level: Boolean) {
    SharedPreferencesUtils.preferences.edit().putBoolean("r" + this.deviceId.toString(), level).apply()
}

fun CHDevices.getIsJustRegister(): Boolean {
    return SharedPreferencesUtils.preferences.getBoolean("r" + this.deviceId.toString(), false)
}

fun CHDevices.setIsWidget(level: Boolean) {
    SharedPreferencesUtils.preferences.edit().putBoolean("wid" + this.deviceId.toString(), level).apply()
}

fun CHDevices.getIsWidget(): Boolean {
    return SharedPreferencesUtils.preferences.getBoolean("wid" + this.deviceId.toString(), false)
}

fun CHDevices.setIsNOHand(level: Boolean) {
    SharedPreferencesUtils.preferences.edit().putBoolean("nohand" + this.deviceId.toString(), level).apply()
}

fun CHDevices.getIsNOHand(): Boolean {
    return SharedPreferencesUtils.preferences.getBoolean("nohand" + this.deviceId.toString(), false)
}

fun CHDevices.setIsNOHandG(level: Boolean) {
    SharedPreferencesUtils.preferences.edit().putBoolean("nohandg" + this.deviceId.toString(), level).apply()
}

fun CHDevices.getIsNOHandG(): Boolean {
    return SharedPreferencesUtils.preferences.getBoolean("nohandg" + this.deviceId.toString(), false)
}

fun CHDevices.setNOHandRadius(level: Float) {
    SharedPreferencesUtils.preferences.edit().putFloat("getNOHandRadius" + this.deviceId.toString(), level).apply()
}

fun CHDevices.getNOHandRadius(): Float {
    return SharedPreferencesUtils.preferences.getFloat("getNOHandRadius" + this.deviceId.toString(), 100f)
}

fun CHDevices.setNOHandLeft(level: Float) {
    SharedPreferencesUtils.preferences.edit().putFloat("getNOHandLeft" + this.deviceId.toString(), level).apply()
}

fun CHDevices.getNOHandLeft(): Float {
    return SharedPreferencesUtils.preferences.getFloat("getNOHandLeft" + this.deviceId.toString(), 0f)
}

fun CHDevices.setNOHandRight(level: Float) {
    SharedPreferencesUtils.preferences.edit().putFloat("getNOHandRight" + this.deviceId.toString(), level).apply()
}

fun CHDevices.getNOHandRight(): Float {
    return SharedPreferencesUtils.preferences.getFloat("getNOHandRight" + this.deviceId.toString(), 0f)
}


fun CHDevices.setLevel(level: Int) {
    SharedPreferencesUtils.preferences.edit().putInt("l" + this.deviceId.toString(), level).apply()
}

fun CHDevices.getLevel(): Int {
    return SharedPreferencesUtils.preferences.getInt("l" + this.deviceId.toString(), -1)
}

fun CHDevices.setNFC(level: String) {
    SharedPreferencesUtils.preferences.edit().putString("nfc" + this.deviceId.toString(), level).apply()
}

fun CHDevices.clearNFC() {
    SharedPreferencesUtils.preferences.edit().remove("nfc" + this.deviceId.toString()).apply()
}

fun CHDevices.getNFC(): String? {
    return SharedPreferencesUtils.preferences.getString("nfc" + this.deviceId.toString(), null)
}

fun CHDevices.setRank(level: Int) {
    SharedPreferencesUtils.preferences.edit().putInt("ra" + this.deviceId.toString(), -level).apply()
}

fun CHDevices.getRank(): Int {
//    L.d("hcia", "rank "+getNickname()+":" + SharedPreferencesUtils.preferences.getInt("ra" + this.deviceId.toString(), 0))
    return SharedPreferencesUtils.preferences.getInt("ra" + this.deviceId.toString(), 0)
}


fun CHDevices.uiPriority(): Int { // 利用 * -1 排序。數字越大排越上方
    return -1 * when (productModel) {
        CHProductModel.BLEConnector -> 11
        CHProductModel.BiKeLock2 -> 10
        CHProductModel.SSMTouch -> 9
        CHProductModel.SSMTouchPro -> 8
        CHProductModel.SS5PRO -> 7
        CHProductModel.SS5 -> 6
        CHProductModel.SS4 -> 5
        CHProductModel.SS2 -> 4
        CHProductModel.SesameBot1 -> 3
        CHProductModel.BiKeLock -> 2
        CHProductModel.WM2 -> 1
        CHProductModel.SSMOpenSensor -> 0
    }
}


fun Fragment.level2Tag(level: Int?): String {
    if (level == 0) {
        return getString(R.string.owner)
    }
    if (level == 1) {
        return getString(R.string.manager)
    }
    if (level == 2) {
        return getString(R.string.guest)
    }
    return "unknown"
}

fun CHDevices.setNickname(name: String) {
    SharedPreferencesUtils.preferences.edit().putString(this.deviceId.toString(), name).apply()
}

fun CHDevices.getNickname(): String {
    return SharedPreferencesUtils.preferences.getString(this.deviceId.toString(), productModel.modelName()) ?: productModel.modelName()
}
fun CHDevices.setTestLoginCount(name: Int) {
    SharedPreferencesUtils.preferences.edit().putInt("testloginct" +this.deviceId.toString(), name).apply()
}

fun CHDevices.getTestLoginCount(): Int {
    return SharedPreferencesUtils.preferences.getInt("testloginct" +this.deviceId.toString(),0)
}

fun CHDevices.getDistance(): Int {
    return (10.0.pow(((0 - (rssi?.toDouble() ?: 0.0) - 62.0) / 20.0)) * 100).toInt()
}

fun CHDevices.getFirZip(): Int {
    //        L.d("hcia", "productModel:" + productModel)
    return when (productModel) {
        CHProductModel.SS2 -> R.raw.sesame_221_0_8c080c
        CHProductModel.SS4 -> R.raw.sesame_421_4_50ce5b
        CHProductModel.SS5 -> R.raw.sesame5_30_5_aaac8b
        CHProductModel.SS5PRO -> R.raw.sesame5pro_30_7_aaac8b
        CHProductModel.WM2 -> 0
        CHProductModel.SesameBot1 -> R.raw.sesamebot1_21_2_369eb9
        CHProductModel.BiKeLock -> R.raw.sesamebike1_21_3_d7162a
        CHProductModel.BiKeLock2 -> R.raw.sesamebike2_30_6_4835a6
        CHProductModel.SSMOpenSensor -> R.raw.opensensor1_30_8_dcd308
        CHProductModel.SSMTouchPro -> R.raw.sesametouch1pro_30_9_77a483
        CHProductModel.SSMTouch -> R.raw.sesametouch1_30_10_77a483
        CHProductModel.BLEConnector -> R.raw.bleconnector_30_11_3a61be
    }

//    fun findDFU(zipStart: String): Int {
////        L.d("hcia", "zipStart:" + zipStart)
//        val fields: Array<Field> = R.raw::class.java.fields
//        fields.forEach { field ->
////            L.d("hcia", "field.name:" + field.name)
//            if (field.name.startsWith(zipStart)) {
//                return field.getInt(field)
//            }
//        }
//        return -1
//    }
//
//    return when (productModel) {
//        CHProductModel.SS2 -> findDFU("sesame_221")
//        CHProductModel.SS4 -> findDFU("sesame_421")
//        CHProductModel.SS5 -> findDFU("sesame5_")
//        CHProductModel.SS5PRO -> findDFU("sesame5pro_")
//        CHProductModel.SesameBot1 -> findDFU("sesamebot1_")
//        CHProductModel.BiKeLock -> findDFU("sesamebike1_")
//        CHProductModel.BiKeLock2 -> findDFU("sesamebike2_")
//        CHProductModel.SSMOpenSensor -> findDFU("opensensor1_")
//        CHProductModel.SSMTouchPro -> findDFU("sesametouch1pro_")
//        CHProductModel.WM2 -> return -1 //wm2 聯網更新。不需要 zip
//    }
}

fun CHProductModel.modelName(): String {
    return when (this) {
        CHProductModel.WM2 -> MainActivity.activity!!.getString(R.string.WM2)
        CHProductModel.SS2 -> MainActivity.activity!!.getString(R.string.Sesame)
        CHProductModel.SS4 -> MainActivity.activity!!.getString(R.string.Sesame)
        CHProductModel.SS5 -> MainActivity.activity!!.getString(R.string.Sesame5)
        CHProductModel.SS5PRO -> MainActivity.activity!!.getString(R.string.Sesame5pro)
        CHProductModel.SesameBot1 -> MainActivity.activity!!.getString(R.string.SesameBot)
        CHProductModel.BiKeLock -> MainActivity.activity!!.getString(R.string.SesameBike)
        CHProductModel.BiKeLock2 -> MainActivity.activity!!.getString(R.string.SesameBike2)
        CHProductModel.SSMOpenSensor -> MainActivity.activity!!.getString(R.string.SesameOpenSensor)
        CHProductModel.SSMTouchPro -> MainActivity.activity!!.getString(R.string.SSMTouchPro)
        CHProductModel.SSMTouch -> MainActivity.activity!!.getString(R.string.SSMTouch)
        CHProductModel.BLEConnector -> MainActivity.activity!!.getString(R.string.BLEConnector)
    }
}
