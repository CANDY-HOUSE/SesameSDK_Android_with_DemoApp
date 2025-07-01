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
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.device.CHSesameLock
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.toHexString
import co.utils.NotificationUtils
import co.utils.SharedPreferencesUtils
import co.utils.UserUtils
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONArray
import org.json.JSONObject

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
        L.d(tag, "onNewToken=$token")
        SharedPreferencesUtils.isUploadDeveceToken = false
        SharedPreferencesUtils.deviceToken = token
        baseContext.candyHouseApplication.subscriptionManager.onNewToken(token)
    }

    /**
     * Handle time allotted to BroadcastReceivers.
     */
    private fun handleNow(data: MutableMap<String, String>) {
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
                return
            }
        }

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
                                    val jsonHistoryTag = JSONObject(historyTagData).getJSONArray("data").toByteArray()
                                    val devicesHistoryTag = device.getHistoryTag()
                                    val isMe = checkMe(triggerUserSub, jsonHistoryTag, devicesHistoryTag)
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
                result.onFailure {
                    L.d(tag = "hcia", msg = "it:$it")
                }
            }
        } catch (e: Exception) {
            L.d("hcia", "e:$e")
        }
    }

    private fun checkMe(triggerUserSub: String?, jsonHistoryTag: ByteArray, devicesHistoryTag: ByteArray?): Boolean {
        return try {
            val triggerData = parseTriggerUserData(triggerUserSub)
            when {
                triggerData.isEmpty() -> checkIfSameHistoryTag(jsonHistoryTag, devicesHistoryTag)
                triggerData.size == 16 -> checkIfSameTriggerUser(triggerData)
                else -> false
            }
        } catch (e: Exception) {
            L.e("checkMe", "Error parsing triggerUserSub", e)
            false
        }
    }

    private fun parseTriggerUserData(triggerUserSub: String?): ByteArray {
        if (triggerUserSub == null || triggerUserSub.isEmpty()) {
            return ByteArray(0)
        }
        val jsonObject = JSONObject(triggerUserSub)
        val dataArray = jsonObject.getJSONArray("data")

        return ByteArray(dataArray.length()) { i ->
            dataArray.getInt(i).toByte()
        }
    }

    private fun checkIfSameTriggerUser(triggerData: ByteArray): Boolean {
        val triggerUserSubHex = triggerData.toHexString()
        val userId = UserUtils.getUserId()?.replace("-", "") ?: return false
        return triggerUserSubHex.equals(userId, ignoreCase = true)
    }

    private fun checkIfSameHistoryTag(
        jsonHistoryTag: ByteArray,
        devicesHistoryTag: ByteArray?
    ): Boolean {
        // 如果触发数据为空，检查历史标签是否匹配
        devicesHistoryTag?.let {
            if (jsonHistoryTag.isNotEmpty() && String(it).equals(String(jsonHistoryTag))) {
                return true
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