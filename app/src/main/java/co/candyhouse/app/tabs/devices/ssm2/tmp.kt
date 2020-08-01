package co.candyhouse.app.tabs.devices.ssm2

import co.candyhouse.app.R
import co.candyhouse.sesame.ble.CHSesame2Status
import co.candyhouse.sesame.ble.Sesame2.CHSesame2

fun ssmUIParcer(device: CHSesame2): Int {
    return when (device.deviceStatus) {
        CHSesame2Status.dfumode -> R.drawable.icon_nosignal
        CHSesame2Status.noSignal -> R.drawable.icon_nosignal
        CHSesame2Status.receiveBle -> R.drawable.icon_receiveblee
        CHSesame2Status.connecting -> R.drawable.icon_receiveblee
        CHSesame2Status.waitgatt -> R.drawable.icon_waitgatt
        CHSesame2Status.logining -> R.drawable.icon_logining
        CHSesame2Status.readytoRegister -> R.drawable.icon_nosignal
        CHSesame2Status.locked -> R.drawable.icon_lock
        CHSesame2Status.unlocked -> R.drawable.icon_unlock
        CHSesame2Status.nosetting -> R.drawable.icon_nosetting
        CHSesame2Status.moved -> R.drawable.icon_unlock
        CHSesame2Status.reset -> R.drawable.icon_nosignal
        CHSesame2Status.registing -> R.drawable.icon_logining
    }
}