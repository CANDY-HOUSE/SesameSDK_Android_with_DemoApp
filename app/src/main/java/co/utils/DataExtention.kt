package co.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import com.google.android.gms.location.LocationServices
import java.nio.charset.StandardCharsets
import java.util.*
import android.location.Location
import androidx.core.app.ActivityCompat
import android.Manifest
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.CHResultState
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import java.io.Serializable
import kotlin.experimental.and

internal fun String.base64decodeHex(): String {
    val data = Base64.decode(this, Base64.DEFAULT)
    return data.toHexString()
}

internal fun String.noHashtoUUID(): UUID? {
    if (this.length != 32) {
        throw IllegalArgumentException("Input string must be 32 characters long")
    }
    val input = this
    val tmp = input.length - 1
    val testid = input.substring(0..7) + "-" + input.substring(8..11) + "-" + input.substring(12..15) + "-" + input.substring(16..19) + "-" + input.substring(20..tmp)
    return UUID.fromString(testid)
}

internal fun String.isUUIDv4(): Boolean {
    // 如果输入为 null，直接返回 false
    if (this.isNullOrEmpty()) return false

    // 移除所有连字符（如果是标准UUID格式）
    val cleanHex = this.replace("-", "")

    // 检查是否为有效的十六进制字符串
    if (!cleanHex.matches(Regex("[0-9a-fA-F]+"))) return false

    // 十六进制字符串转换为字节数组
    val byteArray = cleanHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    // UUIDv4 的固定长度为 16 字节
    if (byteArray.size != 16) return false

    // 定义 UUIDv4 的版本号和变体的字节值
    // 版本号：第7字节的高4位为0100 (即 0x40)
    // 变体：第9字节的高2位为10 (即 0x80)
    val uuidVersionByte = 0x40.toByte() // UUIDv4 版本号
    val uuidVariantByte = 0x80.toByte() // UUIDv4 变体

    // 检查版本号和变体是否符合 UUIDv4 标准
    return (byteArray[6].and(0xF0.toByte()) == uuidVersionByte) && (byteArray[8].and(0xC0.toByte()) == uuidVariantByte)
}

internal fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

internal fun ByteArray.base64Encode(): String {
    val ss = Base64.encode(this.clone(), Base64.DEFAULT)

    return String(ss, StandardCharsets.UTF_8).replace("\n", "")
}


internal fun String.hexStringToByteArray() =
    ByteArray(this.length / 2) { this.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

internal fun String.hexStringToIntStr(): String {
    var tmp = ""
    for ((index, value) in this.withIndex()) {
        if (index % 2 == 1) {
            tmp += value
        }
    }
    return tmp
}

fun getLastKnownLocation(contex: Context, onResponse: CHResult<Location?>) {
    val islocationOK = (ActivityCompat.checkSelfPermission(contex, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(contex, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    if (!islocationOK) {
        return
    }
    LocationServices.getFusedLocationProviderClient(contex).getCurrentLocation(PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).addOnCompleteListener { task: Task<Location> ->
        if (task.isSuccessful && task.result != null) {
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(task.result)))
        }
    }
}

fun convertStringToColor(text: String): String {
    return String.format("#FF%06X", 0xFFFFFF and text.hashCode())
}

inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelable(key)
    }
}

inline fun <reified T : Parcelable> Bundle.getParcelableArrayListCompat(key: String): ArrayList<T>? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayList(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableArrayList(key)
    }
}

inline fun <reified T : Serializable> Bundle.getSerializableCompat(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializable(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getSerializable(key) as? T
    }
}