package co.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import com.google.android.gms.location.LocationServices
import java.nio.charset.StandardCharsets
import java.util.*
import co.candyhouse.server.CHResult
import co.candyhouse.server.CHResultState
import android.location.Location
import androidx.core.app.ActivityCompat
import android.Manifest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task

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
        println("the element at $index is $value")
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