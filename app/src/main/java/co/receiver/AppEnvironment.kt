package co.receiver

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import co.candyhouse.app.BuildConfig
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * 采集尽可能完整的 App 环境信息，拍平为一层 `key=value` 有序数组上报。
 *
 * 对齐 iOS AppEnvironment：每项是单键对象 `[{"key": value}, ...]`，数组顺序即字段顺序。
 */
object AppEnvironment {

    /**
     * 采集全部环境信息。
     * @return 有序数组，每项是单键对象 `[{"key": value}, ...]`。
     */
    fun collect(context: Context): List<Map<String, Any>> {
        val pairs = mutableListOf<Pair<String, Any>>()
        pairs += appInfo()
        pairs += deviceInfo(context)
        pairs += permissionInfo(context)
        val ms = System.currentTimeMillis()
        pairs += "collectedAt" to formatTime(ms)
        return pairs.map { mapOf(it.first to it.second) }
    }

    // MARK: - App

    private fun appInfo(): List<Pair<String, Any>> {
        val locale = Locale.getDefault()
        return listOf(
            "displayName" to "Sesame",
            "version" to BuildConfig.VERSION_NAME,
            "build" to BuildConfig.VERSION_CODE,
            "language" to locale.toLanguageTag(),
            "region" to (locale.country ?: ""),
            "bundleId" to BuildConfig.APPLICATION_ID
        )
    }

    // MARK: - Device / System

    private fun deviceInfo(context: Context): List<Pair<String, Any>> {
        val metrics = context.resources.displayMetrics
        val isTablet =
            context.resources.configuration.smallestScreenWidthDp >= 600
        return listOf(
            "systemName" to "Android",
            "systemVersion" to Build.VERSION.RELEASE,
            "model" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "deviceType" to if (isTablet) "pad" else "phone",
            "screenWidth" to metrics.widthPixels,
            "screenHeight" to metrics.heightPixels,
            "screenScale" to metrics.density,
            "timeZone" to TimeZone.getDefault().id,
            "batteryLevel" to batteryLevelPercent(context)
        )
    }

    /** 电量百分比 0–100；读取失败返回 -1。 */
    private fun batteryLevelPercent(context: Context): Int {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level < 0 || scale <= 0) -1 else ((level * 100f) / scale).toInt()
        } catch (_: Exception) {
            -1
        }
    }

    // MARK: - Permissions / Capabilities

    private fun permissionInfo(context: Context): List<Pair<String, Any>> {
        return listOf(
            "notification" to notificationString(context),
            "bluetooth" to bluetoothString(context),
            "location" to locationString(context),
            "network" to networkTypeString(context)
        )
    }

    private fun notificationString(context: Context): String {
        return if (NotificationManagerCompat.from(context).areNotificationsEnabled()) "authorized" else "denied"
    }

    /** 蓝牙真实状态（电源开关 + 授权）。 */
    private fun bluetoothString(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return "unauthorized"
        }
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter? = manager?.adapter
        return when {
            adapter == null -> "unsupported"
            adapter.isEnabled -> "poweredOn"
            else -> "poweredOff"
        }
    }

    /** 对齐 iOS 授权范围（不关注精度 fine/coarse）：always / whenInUse / denied。 */
    private fun locationString(context: Context): String {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return "denied"
        // Android 10(Q) 起后台定位为独立权限；Q 以下前台授予即等价 always
        val background = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        return if (background) "always" else "whenInUse"
    }

    private fun networkTypeString(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return "unknown"
        val network = cm.activeNetwork ?: return "none"
        val caps = cm.getNetworkCapabilities(network) ?: return "none"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            else -> "none"
        }
    }

    private fun formatTime(ms: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(ms)
    }
}
