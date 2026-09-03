package co.candyhouse.app.ext.connecteddevice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.PowerManager
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import co.candyhouse.app.R
import co.candyhouse.app.tabs.devices.ssm2.getIsNOHand
import co.candyhouse.app.tabs.devices.ssm2.getIsNOHandG
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.app.tabs.devices.ssm2.ssm5UIParser
import co.candyhouse.sesame.open.devices.base.CHDeviceLoginStatus
import co.candyhouse.sesame.open.devices.base.CHDevices

object SesameWidgetNotificationFactory {

    private fun createChannelID(context: Context, channelId: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel =
                NotificationChannel(channelId, "sesame widget", NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.description = "sesame_widget"
            notificationChannel.enableVibration(true)
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
        return channelId
    }

    private fun isScreenOn(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isInteractive
    }

    fun widgetLock(locker: CHDevices, context: Context): Notification {
        val replyActionPendingIntent = SesameConnectedDeviceService.widgetActionPendingIntent(
            context,
            locker.deviceId.hashCode(),
            SesameConnectedDeviceService.toggleAction(locker.deviceId.hashCode())
        )
        val notificationLayout = RemoteViews(context.packageName, R.layout.cell_weget_unlock)
        notificationLayout.setOnClickPendingIntent(R.id.toggle, replyActionPendingIntent)
        notificationLayout.setTextViewText(R.id.title, locker.getNickname())
        val nightModeFlags: Int =
            context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (nightModeFlags) {
            Configuration.UI_MODE_NIGHT_YES -> {
                notificationLayout.setTextColor(
                    R.id.title,
                    ContextCompat.getColor(context, R.color.white)
                )
            }

            else -> {
                notificationLayout.setTextColor(
                    R.id.title,
                    ContextCompat.getColor(context, R.color.black)
                )
            }
        }
        notificationLayout.setImageViewResource(R.id.toggle, ssm5UIParser(locker))
        notificationLayout.setImageViewResource(
            R.id.bl_img,
            if (locker.deviceStatus.value == CHDeviceLoginStatus.logined) R.drawable.ic_bluetooth else R.drawable.ic_bluetooth_grey
        )
        notificationLayout.setImageViewResource(
            R.id.wifi_img,
            if (locker.deviceShadowStatus?.value == CHDeviceLoginStatus.logined) R.drawable.ic_wifi_blue else R.drawable.ic_wifi_grey
        )
        val autounlockResouse =
            if (locker.getIsNOHandG()) R.drawable.ic_autounlock_active else R.drawable.ic_autounlock
        notificationLayout.setImageViewResource(
            R.id.hand_img,
            if (locker.getIsNOHand()) autounlockResouse else R.drawable.ic_autounlock_no
        )
        if (!isScreenOn(context)) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                context.packageName
            )
            wakeLock.acquire(3000)
            wakeLock.release()
        }
        return NotificationCompat.Builder(context, createChannelID(context, "www"))
            .setSmallIcon(R.drawable.small_icon)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setCustomContentView(notificationLayout).setOngoing(true).setNotificationSilent()
            .build()
    }

    fun connectedNotification(requestID: Int, context: Context): Notification {
        val replyActionPendingIntent = SesameConnectedDeviceService.widgetActionPendingIntent(
            context,
            requestID,
            SesameConnectedDeviceService.openAllAction(requestID)
        )
        val replyCloseAllIntent = SesameConnectedDeviceService.widgetActionPendingIntent(
            context,
            requestID,
            SesameConnectedDeviceService.closeAllAction(requestID)
        )

        val notificationLayout = RemoteViews(context.packageName, R.layout.cell_weget)
        notificationLayout.setOnClickPendingIntent(R.id.open_all, replyActionPendingIntent)
        notificationLayout.setOnClickPendingIntent(R.id.close_all, replyCloseAllIntent)
        return NotificationCompat.Builder(context, createChannelID(context, "www"))
            .setSmallIcon(R.drawable.small_icon)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setCustomContentView(notificationLayout).setOngoing(true).setNotificationSilent()
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }
}
