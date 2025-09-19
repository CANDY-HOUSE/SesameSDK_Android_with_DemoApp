package co.utils

import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import co.candyhouse.app.R
import co.candyhouse.sesame.ble.os3.CHSesameBiometricDevice
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHSesame2
import co.candyhouse.sesame.open.device.CHSesame5
import co.candyhouse.sesame.open.device.CHSesameBike
import co.candyhouse.sesame.open.device.CHSesameBike2
import co.candyhouse.sesame.open.device.CHSesameBot
import co.candyhouse.sesame.open.device.CHSesameBot2
import co.candyhouse.sesame.open.device.CHWifiModule2
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

fun CHDevices.isAutoConnect(): Boolean {
    return when (this) {
        is CHSesame2, is CHSesameBot, is CHSesameBike,
        is CHSesame5, is CHSesameBike2, is CHSesameBot2 -> true

        is CHWifiModule2, is CHSesameBiometricDevice -> false
        else -> false
    }
}