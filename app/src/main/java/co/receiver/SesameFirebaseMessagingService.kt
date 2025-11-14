package co.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import co.candyhouse.app.R
import co.candyhouse.app.candyHouseApplication
import co.candyhouse.app.tabs.MainActivity
import co.candyhouse.sesame.utils.L
import co.utils.NotificationUtils
import co.utils.SharedPreferencesUtils
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONArray

class SesameFirebaseMessagingService : FirebaseMessagingService() {

    private val tag = "SesameFirebaseMessagingService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.data.isNotEmpty()) {
            L.d(tag, "Message data payload: ${remoteMessage.data}")
            handleNow(remoteMessage.data)
        }
        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            L.d(tag, "Message Notification Body: ${it.body}")
        }
    }

    override fun onNewToken(token: String) {
        baseContext.candyHouseApplication.subscriptionManager.onNewToken(token)
    }

    /**
     * Handle time allotted to BroadcastReceivers.
     */
    private fun handleNow(data: MutableMap<String, String>) {
        if (handleAdvertisementNotification(data)) {
            return
        }
        if (handleFriedNotification(data)) {
            return
        }
        handleAlertTitleNotification(data)
    }

    private fun handleAlertTitleNotification(data: MutableMap<String, String>) {
        data["alertTitle"]?.let {
            sendNotification(it)
        }
    }

    private fun handleAdvertisementNotification(data: MutableMap<String, String>): Boolean {
        data["messageType"]?.let {
            val messageType = data["messageType"]
            //广告活动
            if (messageType == "announcement") {
                val title = data["title"]
                val body = data["body"]
                val imageUrl = data["imageUrl"]
                val url = data["url"]
                val messageAction = data["messageAction"]
                NotificationUtils.sendNotification(this, title, body, url, imageUrl, messageAction)
                return true
            }
        }
        return false
    }

    private fun handleFriedNotification(data: MutableMap<String, String>): Boolean {
        data["event"]?.let {
            if (it == "friend") {
                SharedPreferencesUtils.isNeedFreshFriend = true
                return true
            }
            if (it == "device") {
                SharedPreferencesUtils.isNeedFreshDevice = true
                return true
            }
        }
        return  false
    }

    private fun sendNotification(title: String) {
        L.d("hcia", "送出推送 title:$title")
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0 /* Request code */,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val channelId = getString(R.string.app_name)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder =
            NotificationCompat.Builder(this, channelId).setSmallIcon(R.drawable.small_icon)
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary)).setContentTitle(title)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.PRIORITY_HIGH)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(defaultSoundUri).setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.Sesame),
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(
            System.currentTimeMillis().toInt(),
            notificationBuilder.build()
        )
    }

    private fun JSONArray.toByteArray(): ByteArray {
        val byteArr = ByteArray(length())
        for (i in 0 until length()) {
            byteArr[i] = (get(i) as Int and 0xFF).toByte()
        }
        return byteArr
    }

    private fun String?.tohisEventi18(ctx: Context): String? {
        return when (this) {
            "LOCK" -> ctx.getString(R.string.historyLOCK)
            "UNLOCK" -> ctx.getString(R.string.historyUNLOCK)
            "AUTOLOCK" -> ctx.getString(R.string.historyAUTOLOCK)
            "AUTOLOCK_UPDATED" -> ctx.getString(R.string.historyAUTOLOCK_UPDATED)
            "MANUAL_LOCKED" -> ctx.getString(R.string.historyMANUAL_LOCKED)
            "MANUAL_UNLOCKED" -> ctx.getString(R.string.historyMANUAL_UNLOCKED)
            else -> this
        }
    }

}