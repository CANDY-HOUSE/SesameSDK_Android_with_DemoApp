package co.candyhouse.sesame.ble

import co.candyhouse.sesame.ble.os3.CHHub3Device
import co.candyhouse.sesame.ble.os3.CHSesame5Device
import co.candyhouse.sesame.ble.os3.CHSesameBike2Device
import co.candyhouse.sesame.ble.os3.CHSesameBot2Device
import co.candyhouse.sesame.open.devices.CHSesame5MechStatus
import co.candyhouse.sesame.open.devices.CHWifiModule2
import co.candyhouse.sesame.open.devices.CHSesameBike2MechStatus
import co.candyhouse.sesame.open.devices.CHWifiModule2NetWorkStatus
import co.candyhouse.sesame.open.devices.base.CHDeviceStatus
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.open.devices.base.CHSesameOS3LockBase
import co.candyhouse.sesame.server.dto.StateInfo
import co.candyhouse.sesame.utils.toReverseBytes

/**
 * 用服务端 list 的 stateInfo 直接写入设备状态，替代冷启动的 IoT 快照拉取（getThingShadow）。
 * stateInfo 不含 isStop/target 等精确位，只能近似，等 BLE/MQTT 补精。（对齐 iOS CHDevice.applyServerState）
 */
internal fun CHDevices.applyServerState(state: StateInfo) {
    val wm2Connected = state.wm2State ?: false

    // WM2 / Hub3（均为 CHWifiModule2）：网络连接（+ Hub3 继电器）
    if (this is CHWifiModule2) {
        mechStatus = CHWifiModule2NetWorkStatus(
            isAPWork = wm2Connected,
            isNetWork = wm2Connected,
            isIOTWork = wm2Connected,
            isAPConnecting = false,
            isConnectingNet = false,
            isConnectingIOT = false,
            isAPCheck = wm2Connected
        )
        if (this is CHHub3Device) {
            state.relayStatus?.let { isRelayOn = it == 1 }
        }
        return
    }

    // 锁类：在线 + 锁态（+ 角度）
    applyServerWM2Connected(wm2Connected)
    if (!wm2Connected) {
        deviceShadowStatus = null
        return
    }
    val isLocked = when (state.CHSesame2Status) {
        "locked" -> true
        "unlocked" -> false
        else -> null
    } ?: return

    state.batteryPercentage?.let { batteryPercentage = it }

    val flags = if (isLocked) 2 else 4 // bit1 isInLockRange / bit2 isInUnlockRange
    when (this) {
        is CHSesame5Device -> {
            val pos = ((state.position ?: 0) * 360 / 1024).toShort()
            // CHSesame5MechStatus: [0,1]battery [2,3]target [4,5]position [6]flags
            val bytes = 0.toShort().toReverseBytes() + pos.toReverseBytes() + pos.toReverseBytes() + byteArrayOf((flags or 16).toByte()) // SS5: bit4(16)=isStop
            mechStatus = CHSesame5MechStatus(bytes)
        }
        is CHSesameBike2Device -> {
            // CHSesameBike2MechStatus: [0,1]battery [2]flags
            val bytes = 0.toShort().toReverseBytes() + byteArrayOf((flags or 4).toByte()) // Bike2: bit2(4)=isStop
            mechStatus = CHSesameBike2MechStatus(bytes)
        }
        // Bot2 等无角度环，仅用 deviceShadowStatus
    }
    deviceShadowStatus = if (isLocked) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked
}

/** 仅 OS3 锁类持有 isConnectedByWM2（对齐 iOS applyServerWM2Connected）。 */
private fun CHDevices.applyServerWM2Connected(connected: Boolean) {
    (this as? CHSesameOS3LockBase)?.setConnectedByWM2(connected)
}
