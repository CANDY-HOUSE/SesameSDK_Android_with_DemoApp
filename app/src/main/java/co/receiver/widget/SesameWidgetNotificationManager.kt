package co.receiver.widget

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import co.candyhouse.app.tabs.devices.ssm2.getIsNOHand
import co.candyhouse.app.tabs.devices.ssm2.getIsNOHandG
import co.candyhouse.app.tabs.devices.ssm2.getIsWidget
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.open.devices.base.CHSesameLock

object SesameWidgetNotificationManager {
    fun update(context: Context, devices: List<CHDevices>, deviceId: String? = null) {
        val locks = devices.filterIsInstance<CHSesameLock>()
        val hasWidgets = locks.any { it.getIsWidget() }
        val hasAutoUnlock = devices.any { it.getIsNOHand() && it.getIsNOHandG() }
        val canPostNotifications =
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED

        SesameConnectedDeviceService.sync(
            context,
            hasWidgets = hasWidgets && canPostNotifications,
            hasAutoUnlock = hasAutoUnlock,
            deviceIds = devices.mapTo(mutableSetOf()) { it.deviceId.toString() }
        )

        if (!canPostNotifications) return
        refresh(context, devices, deviceId)
    }

    fun refresh(context: Context, devices: List<CHDevices>, deviceId: String? = null) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager = NotificationManagerCompat.from(context)
        val locks = devices.filterIsInstance<CHSesameLock>()
        if (deviceId == null) {
            locks.forEach { device ->
                if (device.getIsWidget()) {
                    manager.notify(
                        device.deviceId.hashCode(),
                        CHServiceManager.widgetLock(device, context)
                    )
                } else {
                    manager.cancel(device.deviceId.hashCode())
                }
            }
        } else {
            locks.firstOrNull { it.deviceId.toString() == deviceId }?.let { device ->
                if (device.getIsWidget()) {
                    manager.notify(
                        device.deviceId.hashCode(),
                        CHServiceManager.widgetLock(device, context)
                    )
                } else {
                    manager.cancel(device.deviceId.hashCode())
                }
            }
        }

        val widgetCount = locks.count { it.getIsWidget() }
        if (widgetCount > 1) {
            manager.notify(
                "all".hashCode(),
                CHServiceManager.connectedNotification("all".hashCode(), context)
            )
        } else {
            manager.cancel("all".hashCode())
        }
    }

    fun cancelAll(context: Context, devices: List<CHDevices>) {
        val hasAutoUnlock = devices.any { it.getIsNOHand() && it.getIsNOHandG() }
        SesameConnectedDeviceService.sync(
            context,
            hasWidgets = false,
            hasAutoUnlock = hasAutoUnlock,
            deviceIds = devices.mapTo(mutableSetOf()) { it.deviceId.toString() }
        )

        val manager = NotificationManagerCompat.from(context)
        devices.filterIsInstance<CHSesameLock>().forEach {
            manager.cancel(it.deviceId.hashCode())
        }
        manager.cancel("all".hashCode())
    }
}
