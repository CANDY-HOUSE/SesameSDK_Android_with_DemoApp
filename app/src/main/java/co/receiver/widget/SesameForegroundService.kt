package co.receiver.widget

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import co.candyhouse.app.ext.aws.AWSStatus
import co.candyhouse.app.CandyHouseApp
import co.candyhouse.app.R
import co.candyhouse.app.candyHouseApplication
import co.candyhouse.app.tabs.MainActivity
import co.candyhouse.app.tabs.devices.ssm2.getIsNOHand
import co.candyhouse.app.tabs.devices.ssm2.getIsNOHandG
import co.candyhouse.app.tabs.devices.ssm2.getIsWidget
import co.candyhouse.app.tabs.devices.ssm2.getNOHandLeft
import co.candyhouse.app.tabs.devices.ssm2.getNOHandRadius
import co.candyhouse.app.tabs.devices.ssm2.getNOHandRight
import co.candyhouse.app.tabs.devices.ssm2.setIsNOHandG
import co.candyhouse.app.tabs.devices.ssm2.uiPriority
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.device.CHDeviceLoginStatus
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHSesame2
import co.candyhouse.sesame.open.device.CHSesame5
import co.candyhouse.sesame.open.device.CHSesameLock
import co.candyhouse.sesame.utils.L
import co.utils.PermissionUtils
import co.utils.UserUtils
import co.utils.getLastKnownLocation
import com.amazonaws.mobile.client.AWSMobileClient
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SesameForegroundService : Service() {

    private var isForeground: Boolean = false

    private var isReceiverRegistered = false

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val deviceID = intent?.getStringExtra(acitonKey)
            updateWidgetNotifications(deviceID)
        }
    }

    companion object {
        var isLive: Boolean = false
        var isShowAllNotify: Boolean = false
        const val CHANNEL_ID = "SesameForegroundServiceChannel"
        const val NOTIFICATION_ID = 1 // Define a notification ID
        var aciton: String = "sesame.action.service"
        var acitonKey: String = "acitonKey"
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        isLive = true
        L.d("sf", "SesameForegroundService-onCreate...")

        // 先创建通知并启动前台服务
        // 当API版本大于29时，统一使用FOREGROUND_SERVICE_TYPE_LOCATION 发送前台通知。
        createNotificationChannel()
        val notification = createNotification()

        //启动前台服务，否则后面的小组件通知栏会报错
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            L.d("sf", "SesameForegroundService is startForeground now...")
            try {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } catch (e: Exception) {
                L.d("sf", "SesameForegroundService-onCreate:Exception=" + e.message)

                FirebaseCrashlytics.getInstance().apply {
                    log("SesameForegroundService onCreate run...")
                    log("当前应用状态，是否在前台：${baseContext.candyHouseApplication.appLifecycleObserver.isAppForeground}")
                    if (AWSStatus.getLoginStatus()) {
                        setCustomKey("mail", AWSMobileClient.getInstance().username + "")
                    }
                    recordException(e)
                }
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // 权限检查放在后面，不影响前台服务启动，避免ForegroundServiceDidNotStartInTimeException
        // 添加permission的保护，防止用户手动关闭权限导致崩溃
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }
        if (!PermissionUtils.checkAndRequestPermissions(
                MainActivity.activity,
                permissions.toTypedArray(),
                PermissionUtils.REQUEST_CODE_FOREGROUND_NOTIFICATION_PERMISSION
            )
        ) {
            L.d("sf", "onCreate: Permissions denied")
            //权限校验没有通过，关闭服务
            stopSelf()
            return
        }

        isForeground = true

        val filter = IntentFilter(aciton) // 使用你定义的 action
        if (!isReceiverRegistered) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(receiver, filter, RECEIVER_EXPORTED)
            } else {
                registerReceiver(receiver, filter)
            }
            isReceiverRegistered = true
        }

        //设置开关锁小组件通知栏
        startServiceLoop()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Sesame Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateWidgetNotifications()
        return START_STICKY
    }

    @SuppressLint("WrongConstant")
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CANDY HOUSE")
            .setContentText("Widget Running - Keep Active!")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun startServiceLoop() {
        CoroutineScope(Dispatchers.IO).launch {
            while (isLive) {
                delay(1000)

                val devicesCopy: List<CHDevices>
                synchronized(CHDeviceManager.listDevices) {
                    devicesCopy = CHDeviceManager.listDevices.toList()
                }
                val sortedDevices = devicesCopy
                    .sortedWith(compareBy({ it.uiPriority() }, { it.deviceId }))
                    .reversed()

                sortedDevices.forEach { device ->
                    if (device.getIsNOHand()) {
                        handleNoHandDevice(device)
                    }
                }
            }
        }
    }

    private fun handleNoHandDevice(device: CHDevices) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (!device.getIsNOHandG() && device.deviceStatus.value == CHDeviceLoginStatus.UnLogin) {
            getLastKnownLocation(baseContext.applicationContext) { locationState ->
                locationState.getOrNull()?.data?.let { location ->
                    val dist = FloatArray(1)
                    Location.distanceBetween(
                        device.getNOHandLeft().toDouble(),
                        device.getNOHandRight().toDouble(),
                        location.latitude,
                        location.longitude,
                        dist
                    )
                    dist.firstOrNull()?.let { distance ->
                        if (distance > device.getNOHandRadius()) {
                            device.setIsNOHandG(true)
                            NotificationManagerCompat.from(baseContext.applicationContext).notify(
                                device.deviceId.hashCode(),
                                CHServiceManager.widgetLock(device, baseContext.applicationContext)
                            )
                        }
                    }
                }
            }
        }
        if (device.deviceStatus.value == CHDeviceLoginStatus.Login) {
            device.rssi?.let {
                if (device.getIsNOHandG()) {
                    if (device.deviceStatus == CHDeviceStatus.Unlocked) {
                        device.setIsNOHandG(false)
                    } else if (device.deviceStatus != CHDeviceStatus.Unlocked) {
                        device.setIsNOHandG(false)
                        (device as? CHSesame5)?.unlock(historytag = UserUtils.getUserIdWithByte()) { }
                        (device as? CHSesame2)?.unlock() { }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        L.d("sf", "SesameForegroundService-onDestroy...")

        //增加判断广播是否已注册
        if (isReceiverRegistered) {
            unregisterReceiver(receiver)
            isReceiverRegistered = false
        }

        isShowAllNotify = false
        isLive = false
        SesameReceiver.isStartForegroundServiceWithPendingIntent = false
        CHDeviceManager.listDevices.forEach { device ->
            if (device is CHSesameLock) {
                NotificationManagerCompat.from(baseContext.applicationContext)
                    .cancel(device.deviceId.hashCode())
            }
        }
        NotificationManagerCompat.from(baseContext.applicationContext).cancel("all".hashCode())
        if (isForeground) {
            stopForeground(true)
            isForeground = false // Reset the flag
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateWidgetNotifications(id: String? = null) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notificationManager = NotificationManagerCompat.from(baseContext.applicationContext)
        var widgetCount = 0

        val sortedDevices = synchronized(CHDeviceManager.listDevices) {
            CHDeviceManager.listDevices
                .sortedWith(compareBy({ it.uiPriority() }, { it.deviceId }))
                .reversed()
        }
        L.d("updateWidgets", "更新设备列表" + "--大小:" + sortedDevices.size + "---id:" + id)
        sortedDevices.forEach { device ->
            if (device is CHSesameLock && device.getIsWidget()) {
                widgetCount++
            }
        }

        if (id == null) {
            sortedDevices.forEach { device ->
                if (device is CHSesameLock) {
                    if (device.getIsWidget()) {
                        val notification =
                            CHServiceManager.widgetLock(device, baseContext.applicationContext)
                        notificationManager.notify(device.deviceId.hashCode(), notification)
                    } else {
                        notificationManager.cancel(device.deviceId.hashCode())
                    }
                }
            }
        } else {
            val findDevice = sortedDevices.find { it.getIsWidget() && it.deviceId.toString() == id }
            findDevice?.apply {
                val notification = CHServiceManager.widgetLock(this, baseContext.applicationContext)
                notificationManager.notify(findDevice.deviceId.hashCode(), notification)
            }
        }

        if (widgetCount > 1) {
            if (!isShowAllNotify) {
                notificationManager.notify(
                    "all".hashCode(),
                    CHServiceManager.connectedNotification(
                        "all".hashCode(),
                        baseContext.applicationContext
                    )
                )
                isShowAllNotify = true
            }
        } else {
            if (isForeground) {
                notificationManager.cancel("all".hashCode())
                isShowAllNotify = false
            }
        }
    }

}
