package co.candyhouse.app.tabs.devices.ssm2

import co.candyhouse.app.R
import co.candyhouse.sesame.ble.Sesame2.CHSesame2
import co.candyhouse.sesame.ble.Sesame2.CHSesame2Status

fun ssmUIParcer(device: CHSesame2): Int {
    return when (device.deviceStatus) {
        CHSesame2Status.dfumode -> R.drawable.icon_nosignal
        CHSesame2Status.noBleSignal -> R.drawable.icon_nosignal
        CHSesame2Status.receivedBle -> R.drawable.icon_receiveblee
        CHSesame2Status.bleConnecting -> R.drawable.icon_receiveblee
        CHSesame2Status.waitingGatt -> R.drawable.icon_waitgatt
        CHSesame2Status.bleLogining -> R.drawable.icon_logining
        CHSesame2Status.readyToRegister -> R.drawable.icon_nosignal
        CHSesame2Status.locked -> R.drawable.icon_lock
        CHSesame2Status.unlocked -> R.drawable.icon_unlock
        CHSesame2Status.noSettings -> R.drawable.icon_nosetting
        CHSesame2Status.moved -> R.drawable.icon_unlock
        CHSesame2Status.reset -> R.drawable.icon_nosignal
        CHSesame2Status.registering -> R.drawable.icon_logining
        else -> R.drawable.ic_icons_outlined_setting

    }
}