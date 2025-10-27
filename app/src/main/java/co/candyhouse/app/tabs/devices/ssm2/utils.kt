package co.candyhouse.app.tabs.devices.ssm2

import android.content.Context
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.style.AlignmentSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import co.candyhouse.app.R
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHProductModel
import co.candyhouse.sesame.open.device.CHSesameBot
import co.candyhouse.sesame.open.device.CHSesameBot2
import co.candyhouse.sesame.open.device.CHSesameLock
import co.candyhouse.sesame.utils.L
import co.utils.SharedPreferencesUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

fun ssm5UIParser(device: CHSesameBot2): Int {


//    L.d("hcia", "[ssm5UIParser bot2!]deviceShadowStatus:" + device.deviceShadowStatus + " deviceStatus:" + device.deviceStatus)
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

fun createOpensensorStateText(status: String, timeStamp: Long): SpannableStringBuilder? {
    return runCatching {
        val statusLength = status.length

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val (dateString, timeString) = dateFormat.format(Date(timeStamp)).split(" ")

        val text = buildString {
            if (status.isNotEmpty()) {
                append(status)
                append("\n")
            }
            append(dateString)
            append("\n")
            append(timeString)
        }

        SpannableStringBuilder(text).apply {
            setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, length, SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(ForegroundColorSpan("#E4E3E3".toColorInt()), 0, length, SPAN_EXCLUSIVE_EXCLUSIVE)

            if (statusLength > 0) {
                setSpan(StyleSpan(Typeface.BOLD), 0, statusLength, SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(RelativeSizeSpan(20f / 17f), 0, statusLength, SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }.getOrNull()
}

fun ssm5UIParser(device: CHDevices): Int {
//    L.d("ssm5UIParser",device.getNickname()+"---"+device.deviceId+"---"+device.deviceShadowStatus+"-----"+device.deviceStatus)

    if (device is CHSesameBot) {
        return ssm5UIParser(device)
    }
    if (device is CHSesameBot2) {
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
    if (device.productModel == CHProductModel.SSMOpenSensor || device.productModel == CHProductModel.SSMOpenSensor2) {
        return R.drawable.icon_opensensor
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
    SharedPreferencesUtils.preferences.edit().putInt("ra" + this.deviceId.toString(), level).apply()
}

fun CHDevices.getRank(): Int {
    return SharedPreferencesUtils.preferences.getInt("ra" + this.deviceId.toString(), 0)
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
    try {
        val deviceIdString = this.deviceId?.toString() ?: return productModel.modelName()
        return SharedPreferencesUtils.preferences.getString(deviceIdString, productModel.modelName()) ?: productModel.modelName()
    }catch (e:NullPointerException){
        e.printStackTrace()
    }
   return ""

}
fun CHDevices.setTestLoginCount(name: Int) {
    SharedPreferencesUtils.preferences.edit().putInt("testloginct" +this.deviceId.toString(), name).apply()
}

fun CHDevices.getTestLoginCount(): Int {
    return SharedPreferencesUtils.preferences.getInt("testloginct" +this.deviceId.toString(),0)
}


fun CHDevices.getDistance(): Int {
    val rssiValue = rssi?.toDouble() ?: return Int.MAX_VALUE
    return (10.0.pow(((0 - rssiValue - 62.0) / 20.0)) * 100).toInt()
}

fun CHDevices.getFirmwareName(): String? {
    L.d("firmware", "productModel:$productModel")
    return when (productModel) {
        CHProductModel.SS2 -> "sesame_221_0_8c080c"
        CHProductModel.SS4 -> "sesame_421_4_50ce5b"
        CHProductModel.SS5 -> "sesame5_30_5_1e6f97"
        CHProductModel.SS5PRO -> "sesame5pro_30_7_1e6f97"
        CHProductModel.WM2 -> null
        CHProductModel.SesameBot1 -> "sesamebot1_21_2_369eb9"
        CHProductModel.BiKeLock -> "sesamebike1_21_3_d7162a"
        CHProductModel.BiKeLock2 -> "sesamebike2_30_6_990e7f"
        CHProductModel.SSMTouchPro -> "sesametouch1pro_30_9_4d0bf8"
        CHProductModel.SSMTouch -> "sesametouch1_30_10_4d0bf8"
        CHProductModel.SSMOpenSensor -> "opensensor1_30_8_7970f1"
        CHProductModel.BLEConnector -> "bleconnector_30_11_01be3d"
        CHProductModel.Remote -> "remote_30_14_03d244"
        CHProductModel.RemoteNano -> "remoten_30_15_03d244"
        CHProductModel.SesameBot2 -> "sesamebot2_30_17_990e7f"
        CHProductModel.SS5US -> "sesame5us_30_16_1e6f97"
        CHProductModel.SSMFacePro -> "sesameface1pro_30_18_0ace8d"
        CHProductModel.SSMFaceAI -> "sesameface1ai_30_23_0ace8d"
        CHProductModel.SSMFaceProAI -> "sesameface1proai_30_22_0ace8d"
        CHProductModel.SSMFace -> "sesameface1_30_19_0ace8d"
        CHProductModel.SS6Pro -> "sesame6pro_30_21_a659e3"
        CHProductModel.SSMOpenSensor2 -> "opensensor2_30_24_7970f1"
        else -> null
    }
}

fun CHDevices.getFirmwarePath(context: Context): String? {
    val fileName = getFirmwareName() ?: return null
    val cacheDir = File(context.cacheDir, "firmware")
    if (!cacheDir.exists()) {
        cacheDir.mkdirs()
    }

    val cacheFile = File(cacheDir, "$fileName.zip")

    // 如果缓存文件不存在，从assets复制
    if (!cacheFile.exists()) {
        try {
            context.assets.open("firmware/$fileName.zip").use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            L.e("firmware", "Failed to copy firmware: $fileName", e)
            return null
        }
    }

    return cacheFile.absolutePath
}

fun CHProductModel.modelName(): String {
    return when (this) {
        CHProductModel.WM2 -> CHDeviceManager.app.getString(R.string.WM2)
        CHProductModel.SS2 -> CHDeviceManager.app.getString(R.string.Sesame)
        CHProductModel.SS4 -> CHDeviceManager.app.getString(R.string.Sesame)
        CHProductModel.SS5 -> CHDeviceManager.app.getString(R.string.Sesame5)
        CHProductModel.SS5PRO -> CHDeviceManager.app.getString(R.string.Sesame5pro)
        CHProductModel.SesameBot1 -> CHDeviceManager.app.getString(R.string.SesameBot)
        CHProductModel.BiKeLock -> CHDeviceManager.app.getString(R.string.SesameBike)
        CHProductModel.BiKeLock2 -> CHDeviceManager.app.getString(R.string.SesameBike2)
        CHProductModel.SSMOpenSensor -> CHDeviceManager.app.getString(R.string.SesameOpenSensor)
        CHProductModel.SSMTouchPro -> CHDeviceManager.app.getString(R.string.SSMTouchPro)
        CHProductModel.SSMTouch -> CHDeviceManager.app.getString(R.string.SSMTouch)
        CHProductModel.BLEConnector -> CHDeviceManager.app.getString(R.string.BLEConnector)
        CHProductModel.Hub3 -> CHDeviceManager.app.getString(R.string.Hub3)
        CHProductModel.Remote -> CHDeviceManager.app.getString(R.string.CHRemote)
        CHProductModel.RemoteNano -> CHDeviceManager.app.getString(R.string.CHRemoteNano)
        CHProductModel.SS5US -> CHDeviceManager.app.getString(R.string.Sesame5us)
        CHProductModel.SesameBot2 -> CHDeviceManager.app.getString(R.string.SesameBot2)
        CHProductModel.SSMFace -> CHDeviceManager.app.getString(R.string.SSMFace)
        CHProductModel.SSMFacePro -> CHDeviceManager.app.getString(R.string.SSMFacePro)
        CHProductModel.SSMFaceProAI -> CHDeviceManager.app.getString(R.string.SSMFaceProAI)
        CHProductModel.SSMFaceAI -> CHDeviceManager.app.getString(R.string.SSMFaceAI)
        CHProductModel.SS6Pro -> CHDeviceManager.app.getString(R.string.Sesame6Pro)
        CHProductModel.SSMOpenSensor2 -> CHDeviceManager.app.getString(R.string.SesameOpenSensor2)
    }
}
