package co.candyhouse.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.core.net.toUri

fun Context.dp(value: Int) = (value * resources.displayMetrics.density).toInt()

fun Context.openExternalUrl(uri: Uri) {
    require(uri.scheme in setOf("https", "http", "mailto", "tel"))
    startActivity(Intent(Intent.ACTION_VIEW, uri))
}

fun Context.openNotificationSettings() {
    val settings = if (Build.VERSION.SDK_INT >= 26) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    } else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())
    startActivity(settings)
}

/** Vibrates for [duration] ms, or plays a light tick on [view] when no duration is given. */
fun Context.haptic(view: View, duration: Long) {
    if (duration > 0) vibrate(duration) else view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}

/** Vibrates for [duration] ms; usable without a View (e.g. from a Service). */
fun Context.vibrate(duration: Long) {
    val vibrator = if (Build.VERSION.SDK_INT >= 31) getSystemService(VibratorManager::class.java).defaultVibrator
    else getSystemService(Vibrator::class.java)
    if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
    else vibrator.vibrate(duration)
}
