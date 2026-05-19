package co.utils

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import co.candyhouse.app.R
import co.candyhouse.app.ext.userKey
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.open.devices.base.CHSesameLock
import co.candyhouse.sesame.utils.L

private fun NavController.hasAction(actionId: Int): Boolean {
    val action = this.currentDestination?.getAction(actionId)
    return action != null || this.graph.getAction(actionId) != null
}

fun Fragment.safeNavigate(actionId: Int) {
    this.safeNavigate(actionId, null)
}

fun Fragment.safeNavigateBack() {
    if (!isAdded) return
    findNavController().navigateUp()
}

fun Fragment.safeNavigate(actionId: Int, bundle: Bundle?) {
    try {
        if (!isAdded) return
        val navController = findNavController()
        val hasAction = navController.hasAction(actionId)
        L.d("sf", "hasAction=$hasAction")
        if (hasAction) {
            navController.navigate(actionId, bundle)
        } else {
            L.d("sf", "navigation cannot be found actionId=$actionId")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun applyInsetsPadding(
    view: View,
    top: Boolean = false,
    bottom: Boolean = false,
    left: Boolean = false,
    right: Boolean = false
) {
    ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(
            if (left) systemBars.left else 0,
            if (top) systemBars.top else 0,
            if (right) systemBars.right else 0,
            if (bottom) systemBars.bottom else 0
        )
        insets
    }
}

fun Fragment.clearContainerTopPadding() {
    activity?.findViewById<View>(R.id.main_container)?.let { container ->
        container.setPadding(0, 0, 0, 0)
        applyInsetsPadding(container)
    }
}

fun Fragment.restoreContainerTopPadding() {
    activity?.findViewById<View>(R.id.main_container)?.let { container ->
        applyInsetsPadding(container, top = true)
    }
}

fun Fragment.applyBottomInsets() {
    applyInsetsPadding(requireView(), bottom = true)
}

val CHDevices.hasFirmwareUpdate: Boolean
    get() {
        val current = userKey?.stateInfo?.currentFwVer
        val latest = userKey?.stateInfo?.latestFwVer
        return current != null && latest != null && current != latest
    }

fun CHDevices.isLockDevice(): Boolean = this is CHSesameLock

@Suppress("DEPRECATION")
fun Context.vibrateDevice(milliseconds: Long) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (!vibrator.hasVibrator()) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        vibrator.vibrate(milliseconds)
    }
}