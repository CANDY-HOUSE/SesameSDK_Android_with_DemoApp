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
import androidx.lifecycle.lifecycleScope
import co.candyhouse.app.R
import co.candyhouse.app.tabs.MainActivity
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.sesame.ble.UUID4HistoryTag
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.device.CHSesame5History
import co.candyhouse.sesame.open.device.CHSesameLock
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.toHexString
import co.utils.SharedPreferencesUtils
import co.utils.UserUtils
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.text.toByteArray

class SesameFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.data.isNotEmpty()) {
            L.d("hcia", "Message data payload: ${remoteMessage.data}")
            handleNow(remoteMessage.data)
        }
        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            L.d("hcia", "Message Notification Body: ${it.body}")
        }
    }

    override fun onNewToken(token: String) {
        SharedPreferencesUtils.isUploadDeveceToken = false
        SharedPreferencesUtils.deviceToken = token
    }

    /**
     * Handle time allotted to BroadcastReceivers.
     */
    private fun handleNow(data: MutableMap<String, String>) {
        L.d("hcia", "[推送] data:$data")

        data["device"]?.let {
            L.d("hcia", "it1:$it")
        }
        data["event"]?.let {
            L.d("hcia", "通知")

            L.d("hcia", "it2:$it")
            if (it == "friend") {
                SharedPreferencesUtils.isNeedFreshFriend = true
                return
            }
            if (it == "device") {
                SharedPreferencesUtils.isNeedFreshDevice = true
                return
            }
        }
        try {
            val deviceID = data["deviceId"]
            val event = data["event"].tohisEventi18(this)
            val deviceName = SharedPreferencesUtils.preferences.getString(
                deviceID?.lowercase(),
                getString(R.string.Sesame)
            )
            CHDeviceManager.getCandyDevices { result ->
                result.onSuccess {
                    it.data.forEach { device ->
                        if (device.deviceId.toString()
                                .uppercase() == deviceID && device is CHSesameLock
                        ) {
                            val historyTagData = data["historyTag"]
                            val triggerUserSub = data["triggerUserSub"]
                            when {
                                historyTagData == null -> {
                                    sendNotification(title = "$deviceName $event", isme = false)
                                }

                                else -> {
                                    val jsonHistoryTag =
                                        JSONObject(historyTagData).getJSONArray("data")
                                            .toByteArray()
                                    val deviceHistoryTag = device.getHistoryTag()
                                    if (deviceHistoryTag != null) {
                                        val isMe = checkMe(triggerUserSub)
                                        L.d(tag = "hcia", msg = "isMe:$isMe")
                                        sendNotification(
                                            title = "$deviceName $event," + String(jsonHistoryTag),
                                            isme = isMe
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                result.onFailure {
                    L.d(tag = "hcia", msg = "it:$it")
                }
            }
        } catch (e: Exception) {
            L.d("hcia", "e:$e")
            sendNotification("error!: $data", false)
        }
    }

    private fun checkMe(triggerUserSub: String?): Boolean {
        if (triggerUserSub != null) {
            try {
                val jsonObject = JSONObject(triggerUserSub)
                val dataArray = jsonObject.getJSONArray("data")
                val byteArray = ByteArray(dataArray.length())
                for (i in 0 until dataArray.length()) {
                    byteArray[i] = dataArray.getInt(i).toByte()
                }
                if (byteArray.size == 16) {
                    val triggerUserSubHex = byteArray.toHexString()
                    val userId = UserUtils.getUserId()?.replace("-", "")
                    if (triggerUserSubHex == userId) {
                        return true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return false
    }

    private fun sendNotification(title: String, isme: Boolean) {
        L.d("hcia", "送出推送 title:$title")
        if (isme) {
            return
        }
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