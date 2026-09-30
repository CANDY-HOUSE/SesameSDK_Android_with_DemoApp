package co.candyhouse.app.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import co.candyhouse.app.R
import co.candyhouse.app.SesameActivity
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** Platform notification rendering; promotion content and destination are supplied by the server. */
object PushNotification {
    fun show(context: Context, title: String, body: String?, url: String?, imageUrl: String?, action: String?, announcement: Boolean) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = if (announcement) "announcement_channel" else "sesame_notifications"
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(
            NotificationChannel(channel, context.getString(R.string.Sesame), NotificationManager.IMPORTANCE_HIGH)
        )
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val id = (System.currentTimeMillis() and 0x7fffffff).toInt()
        val intent = Intent(context, SesameActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("pushUrl", url)
            putExtra("messageAction", action)
        }
        val pending = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification).setColor(0xff28aeb1.toInt())
            .setContentTitle(title).setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending).setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH).setDefaults(NotificationCompat.DEFAULT_ALL)
        if (announcement) builder.setGroup("group_marketing")
        // Deliver text immediately; an unavailable image must not suppress the promotion.
        manager.notify(id, builder.build())
        imageUrl?.let(::loadImage)?.let { bitmap ->
            builder.setLargeIcon(bitmap).setOnlyAlertOnce(true).setStyle(
                NotificationCompat.BigPictureStyle().bigPicture(bitmap).bigLargeIcon(null as Bitmap?).setSummaryText(body)
            )
            manager.notify(id, builder.build())
        }
    }

    /** Supports our data pushes, Firebase background notification extras, and old pending intents. */
    fun consumeUrl(intent: Intent): String? {
        val legacy = intent.data?.takeIf { it.scheme == "candyhouse" && it.host == "open" }
        val url = intent.getStringExtra("pushUrl") ?: intent.getStringExtra("url") ?: legacy?.getQueryParameter("url")
        val action = intent.getStringExtra("messageAction") ?: legacy?.getQueryParameter("action")
        intent.removeExtra("pushUrl")
        intent.removeExtra("url")
        intent.removeExtra("messageAction")
        if (legacy != null) intent.data = null
        return url?.takeIf { (action.isNullOrBlank() || action == "open_webview") && it.toUri().scheme == "https" }
    }

    private fun loadImage(url: String): Bitmap? = runCatching {
        require(url.toUri().scheme == "https")
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 2000
            connection.readTimeout = 2000
            // Bound both download size and decoded dimensions; no extra image-loading dependency.
            val deadline = SystemClock.elapsedRealtime() + 4000
            val bytes = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    require(SystemClock.elapsedRealtime() < deadline)
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= 4 * 1024 * 1024)
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            require(bytes.size <= 4 * 1024 * 1024)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val options = BitmapFactory.Options().apply {
                inSampleSize = 1
                while (bounds.outWidth / inSampleSize > 1024 || bounds.outHeight / inSampleSize > 1024) {
                    inSampleSize = inSampleSize * 2
                }
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}
