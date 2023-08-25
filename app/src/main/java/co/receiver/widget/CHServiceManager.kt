package co.receiver.widget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import co.candyhouse.app.R
import co.candyhouse.app.tabs.MainActivity
import co.candyhouse.app.tabs.devices.ssm2.*
import co.candyhouse.sesame.open.device.*

object CHServiceManager {
    fun createChannelID(context: Context, channelId: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(channelId, "sesame widget", NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.description = "sesame_widget"
            notificationChannel.enableVibration(true)
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
        return channelId
    }

    fun WigetLock(locker: CHDevices, context: Context): Notification {
        val replyActionPendingIntent: PendingIntent

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val intent = Intent(context, MessagingIntentService::class.java)
            intent.action = "toggle_ssm" + locker.deviceId.hashCode()
            replyActionPendingIntent = PendingIntent.getService(context, locker.deviceId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } else {
            val notifyIntent = Intent(context, MainActivity::class.java)
            notifyIntent.action = "toggle_ssm" + locker.deviceId.hashCode()
            replyActionPendingIntent = PendingIntent.getActivity(context, locker.deviceId.hashCode(), notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val notificationLayout = RemoteViews(context.packageName, R.layout.cell_weget_unlock)

        notificationLayout.setOnClickPendingIntent(R.id.toggle, replyActionPendingIntent)
        notificationLayout.setTextViewText(R.id.title, locker.getNickname())
        val nightModeFlags: Int = context.getResources().getConfiguration().uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (nightModeFlags) {
            Configuration.UI_MODE_NIGHT_YES -> {
                notificationLayout.setTextColor(R.id.title, ContextCompat.getColor(context, R.color.white))
            }
            else -> {
                notificationLayout.setTextColor(R.id.title, ContextCompat.getColor(context, R.color.black))
            }
        }
        notificationLayout.setImageViewResource(R.id.toggle, ssm5UIParser(locker))
        notificationLayout.setImageViewResource(R.id.bl_img, if (locker.deviceStatus.value == CHDeviceLoginStatus.Login) R.drawable.ic_bluetooth else R.drawable.ic_bluetooth_grey)
        notificationLayout.setImageViewResource(R.id.wifi_img, if (locker.deviceShadowStatus?.value == CHDeviceLoginStatus.Login ) R.drawable.ic_wifi_blue else R.drawable.ic_wifi_grey)
        val autounlockResouse = if (locker.getIsNOHandG()) R.drawable.ic_autounlock_active else R.drawable.ic_autounlock
        notificationLayout.setImageViewResource(R.id.hand_img, if (locker.getIsNOHand()) autounlockResouse else R.drawable.ic_autounlock_no)
        return NotificationCompat.Builder(context, createChannelID(context, "www")).setSmallIcon(R.drawable.small_icon).setStyle(NotificationCompat.DecoratedCustomViewStyle()).setColor(ContextCompat.getColor(context, R.color.colorPrimary)).setCustomContentView(notificationLayout).setOngoing(true).setNotificationSilent().build()
    }

    fun connectedNotification(requestID: Int, context: Context): Notification {
        val replyActionPendingIntent: PendingIntent
        val replyCloseAllIntent: PendingIntent

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val intent = Intent(context, MessagingIntentService::class.java)
            intent.action = "open_all" + requestID
            replyActionPendingIntent = PendingIntent.getService(context, requestID, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        } else {
            val notifyIntent = Intent(context, MainActivity::class.java)
            notifyIntent.action = "open_all" + requestID
            replyActionPendingIntent = PendingIntent.getActivity(context, requestID, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val intent = Intent(context, MessagingIntentService::class.java)
            intent.action = "close_all" + requestID
            replyCloseAllIntent = PendingIntent.getService(context, requestID, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        } else {
            val notifyIntent = Intent(context, MainActivity::class.java)
            notifyIntent.action = "close_all" + requestID
            replyCloseAllIntent = PendingIntent.getActivity(context, requestID, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        }

        val notificationLayout = RemoteViews(context.packageName, R.layout.cell_weget)
        notificationLayout.setOnClickPendingIntent(R.id.open_all, replyActionPendingIntent)
        notificationLayout.setOnClickPendingIntent(R.id.close_all, replyCloseAllIntent)
        return NotificationCompat.Builder(context, createChannelID(context, "www")).setSmallIcon(R.drawable.small_icon).setStyle(NotificationCompat.DecoratedCustomViewStyle()).setColor(ContextCompat.getColor(context, R.color.colorPrimary)).setCustomContentView(notificationLayout).setOngoing(true).setNotificationSilent().build()
    }
}