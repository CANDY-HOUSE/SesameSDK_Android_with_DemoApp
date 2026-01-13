package co.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import co.candyhouse.app.R
import co.candyhouse.app.tabs.MainActivity
import co.candyhouse.sesame.utils.L
import com.bumptech.glide.Glide

/**
 * 推送弹出通知工具类
 *
 * @author frey on 2025/5/8
 */
object NotificationUtils {
    private const val tag = "NotificationUtils"
    private const val GROUP_KEY_MARKETING = "group_marketing"
    private const val CHANNEL_ID = "announcement_channel"
    private const val ACTION_OPEN_PUSH = "com.candyhouse.action.OPEN_PUSH"

    fun sendNotification(
        context: Context,
        title: String?,
        body: String?,
        url: String?,
        imageUrl: String?,
        messageAction: String?
    ) {
        L.d(tag, "[推送] data content: $title $body $url $imageUrl $messageAction")
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_PUSH
            if (!url.isNullOrBlank()) {
                data = "candyhouse://open?url=${Uri.encode(url)}&action=$messageAction".toUri()
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context, (System.currentTimeMillis() and 0x7fffffff).toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.small_icon)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setGroup(GROUP_KEY_MARKETING)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // 加载并设置大图片
        imageUrl?.let {
            try {
                val bitmap = Glide.with(context)
                    .asBitmap()
                    .load(it)
                    .submit()
                    .get()

                notificationBuilder.setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon(null as Bitmap?)
                )
                notificationBuilder.setLargeIcon(bitmap)
            } catch (e: Exception) {
                L.e(tag, "Failed to load image: ${e.message}")
            }
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "公告通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "应用公告和活动通知"
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify((System.currentTimeMillis() and 0x7fffffff).toInt(), notificationBuilder.build())
    }
}