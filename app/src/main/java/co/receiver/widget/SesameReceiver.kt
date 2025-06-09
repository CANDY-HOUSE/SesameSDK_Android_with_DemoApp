package co.receiver.widget

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import co.candyhouse.app.ext.aws.AWSStatus
import co.candyhouse.app.R
import co.candyhouse.app.candyHouseApplication
import co.candyhouse.sesame.utils.L
import com.amazonaws.mobile.client.AWSMobileClient
import com.google.firebase.crashlytics.FirebaseCrashlytics


class SesameReceiver : BroadcastReceiver() {

    companion object {
        const val SERVER_ACTION = "co.candyhouse.sesame2.START_FOREGROUND_SERVICE"
        private const val CHANNEL_ID = "ForegroundServiceChannel"
        private const val NOTIFICATION_ID = 1
        var isStartForegroundServiceWithPendingIntent: Boolean = false
    }

    @SuppressLint("ScheduleExactAlarm")
    override fun onReceive(context: Context, p1: Intent?) {
        p1?.apply {
            L.d("sf", "onReceive = " + this.action)
            if (SERVER_ACTION == this.action) {
                //先判断应用是否在前台，如果在前台则直接启动服务
                val isInForeground =
                    context.candyHouseApplication.appLifecycleObserver.isAppForeground
                L.d("sf", "isInForeground = $isInForeground")

                if (isInForeground) {
                    //前台，直接启动服务
                    startForegroundServiceDirectly(context, true)
                } else {
                    //后台，根据操作系统版本执行启动服务形式
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        L.d(
                            "sf",
                            "Android 12及以上版本启动前台服务..." + SesameForegroundService.isLive
                        )
                        // Android 12及以上版本
                        if (!isStartForegroundServiceWithPendingIntent) {
                            isStartForegroundServiceWithPendingIntent = true
                            startForegroundServiceWithPendingIntent(context)
                        } else {
                            L.d("sf", "startForegroundServiceWithPendingIntent is start....")
                        }
                    } else {
                        // Android 12以下版本
                        startForegroundServiceDirectly(context, false)
                    }
                }
            }
        }
    }

    private fun startForegroundServiceWithPendingIntent(context: Context) {
        // 创建通知通道
        createNotificationChannel(context)

        // 创建通知
        val notification = createNotification(context)

        // 创建用于启动服务的PendingIntent
        val serviceIntent = Intent(context, SesameForegroundService::class.java)
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                serviceIntent.putExtra(
                    "foregroundServiceType",
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            }
            PendingIntent.getForegroundService(
                context,
                0,
                serviceIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getService(
                context,
                0,
                serviceIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        }

        // 创建通知以显示PendingIntent
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification.apply {
            contentIntent = pendingIntent
        })

        // 尝试启动服务
        try {
            pendingIntent.send()
        } catch (e: PendingIntent.CanceledException) {
            e.printStackTrace()
            isStartForegroundServiceWithPendingIntent = false
        }
    }

    private fun startForegroundServiceDirectly(context: Context, isInForeground: Boolean) {
        val serviceIntent = Intent(context, SesameForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                context.startForegroundService(serviceIntent)
            } catch (e: Exception) {
                L.d("sf", "SesameReceiver-startForegroundServiceDirectly:Exception=" + e.message)

                FirebaseCrashlytics.getInstance().apply {
                    log("SesameReceiver startForegroundServiceDirectly run 1...")
                    log("当前应用状态，是否在前台：$isInForeground")
                    log("前台服务是否存在：${SesameForegroundService.isLive}")
                    if (AWSStatus.getLoginStatus()) {
                        setCustomKey("mail", AWSMobileClient.getInstance().username + "")
                    }
                    recordException(e)
                }

                //传统方式启动服务
                try {
                    context.startService(serviceIntent)
                } catch (e: Exception) {
                    FirebaseCrashlytics.getInstance().apply {
                        log("SesameReceiver startForegroundServiceDirectly 第2次以普通服务启动...")

                        recordException(e)
                    }
                }
            }
        } else {
            context.startService(serviceIntent)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("CANDY HOUSE")
            .setContentText(context.getString(R.string.sesame_widget_notification))
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()
    }

}