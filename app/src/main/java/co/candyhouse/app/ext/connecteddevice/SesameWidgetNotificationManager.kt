package co.candyhouse.app.ext.connecteddevice

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import co.candyhouse.app.tabs.devices.ssm2.getIsNOHand
import co.candyhouse.app.tabs.devices.ssm2.getIsNOHandG
import co.candyhouse.app.tabs.devices.ssm2.getIsWidget
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.open.devices.base.CHSesameLock

object SesameWidgetNotificationManager {
    // Android 会丢弃超过每秒 5 次的通知更新，按低于上限的速率串行发送。
    private const val NOTIFICATION_UPDATE_INTERVAL_MILLISECONDS = 300L
    private val notificationHandler = Handler(Looper.getMainLooper())
    private val pendingNotifications = linkedMapOf<Int, () -> Unit>()
    private var notificationDispatchScheduled = false
    private val notificationDispatchRunnable = object : Runnable {
        override fun run() {
            val notification = synchronized(pendingNotifications) {
                val entry = pendingNotifications.entries.firstOrNull()
                if (entry == null) {
                    notificationDispatchScheduled = false
                    null
                } else {
                    pendingNotifications.remove(entry.key)
                    entry.value
                }
            } ?: return

            notification.invoke()
            notificationHandler.postDelayed(this, NOTIFICATION_UPDATE_INTERVAL_MILLISECONDS)
        }
    }

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

        val appContext = context.applicationContext
        val manager = NotificationManagerCompat.from(appContext)
        val locks = devices.filterIsInstance<CHSesameLock>()
        if (deviceId == null) {
            locks.forEach { device ->
                if (device.getIsWidget()) {
                    val notificationId = device.deviceId.hashCode()
                    enqueueNotification(notificationId) {
                        manager.notify(
                            notificationId,
                            SesameWidgetNotificationFactory.widgetLock(device, appContext)
                        )
                    }
                } else {
                    cancelNotification(manager, device.deviceId.hashCode())
                }
            }
        } else {
            locks.firstOrNull { it.deviceId.toString() == deviceId }?.let { device ->
                if (device.getIsWidget()) {
                    val notificationId = device.deviceId.hashCode()
                    enqueueNotification(notificationId) {
                        manager.notify(
                            notificationId,
                            SesameWidgetNotificationFactory.widgetLock(device, appContext)
                        )
                    }
                } else {
                    cancelNotification(manager, device.deviceId.hashCode())
                }
            }
        }

        refreshAggregateNotification(
            manager,
            appContext,
            locks,
            forceUpdate = deviceId == null
        )
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
            cancelNotification(manager, it.deviceId.hashCode())
        }
        cancelNotification(manager, "all".hashCode())
    }

    private fun enqueueNotification(notificationId: Int, notification: () -> Unit) {
        synchronized(pendingNotifications) {
            pendingNotifications[notificationId] = notification
            if (!notificationDispatchScheduled) {
                notificationDispatchScheduled = true
                notificationHandler.post(notificationDispatchRunnable)
            }
        }
    }

    private fun refreshAggregateNotification(
        manager: NotificationManagerCompat,
        context: Context,
        locks: List<CHSesameLock>,
        forceUpdate: Boolean
    ) {
        val notificationId = "all".hashCode()
        if (locks.count { it.getIsWidget() } <= 1) {
            cancelNotification(manager, notificationId)
            return
        }

        val isPending = synchronized(pendingNotifications) {
            pendingNotifications.containsKey(notificationId)
        }
        val isActive = context.getSystemService(NotificationManager::class.java)
            ?.activeNotifications
            ?.any { it.id == notificationId } == true
        if (forceUpdate || (!isPending && !isActive)) {
            enqueueNotification(notificationId) {
                manager.notify(
                    notificationId,
                    SesameWidgetNotificationFactory.connectedNotification(notificationId, context)
                )
            }
        }
    }

    private fun cancelNotification(manager: NotificationManagerCompat, notificationId: Int) {
        synchronized(pendingNotifications) {
            pendingNotifications.remove(notificationId)
        }
        manager.cancel(notificationId)
    }
}
