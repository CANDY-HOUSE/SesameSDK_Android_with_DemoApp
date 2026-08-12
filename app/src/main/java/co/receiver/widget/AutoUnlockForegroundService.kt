package co.receiver.widget

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import co.candyhouse.app.R
import co.candyhouse.app.candyHouseApplication
import co.candyhouse.app.tabs.MainActivity
import co.candyhouse.app.tabs.devices.ssm2.getIsNOHand
import co.candyhouse.app.tabs.devices.ssm2.getIsNOHandG
import co.candyhouse.app.tabs.devices.ssm2.setIsNOHandG
import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.devices.CHSesame2
import co.candyhouse.sesame.open.devices.CHSesame5
import co.candyhouse.sesame.open.devices.base.CHDeviceLoginStatus
import co.candyhouse.sesame.open.devices.base.CHDeviceStatus
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.utils.L
import co.utils.UserUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AutoUnlockForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null
    private var devices: List<CHDevices> = emptyList()
    private var isLoadingDevices = false

    override fun onCreate() {
        super.onCreate()
        if (!hasBluetoothPermission()) {
            stopSelf()
            return
        }
        isLive = true
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.small_icon)
            .setContentTitle(getString(R.string.auto_mode))
            .setContentText(getString(R.string.auto_unlock_service_notification))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        loadDevicesAndMonitor()
        return START_STICKY
    }

    private fun loadDevicesAndMonitor() {
        if (isLoadingDevices) return
        if (!hasBluetoothPermission()) {
            stopSelf()
            return
        }
        isLoadingDevices = true
        CHDeviceManager.getCandyDevices { result ->
            result.onSuccess { state ->
                devices = state.data
                if (devices.none { it.getIsNOHand() && it.getIsNOHandG() }) {
                    isLoadingDevices = false
                    stopSelf()
                    return@onSuccess
                }
                CHBleManager.enableScan { }
                isLoadingDevices = false
                startMonitorLoop()
            }
            result.onFailure {
                isLoadingDevices = false
                stopSelf()
            }
        }
    }

    private fun startMonitorLoop() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            while (true) {
                val armedDevices = devices.filter { it.getIsNOHand() && it.getIsNOHandG() }
                if (armedDevices.isEmpty()) {
                    stopSelf()
                    return@launch
                }

                armedDevices.forEach { device ->
                    when {
                        device.deviceStatus.value == CHDeviceLoginStatus.logined && device.rssi != null -> {
                            device.setIsNOHandG(false)
                            SesameWidgetNotificationManager.update(
                                this@AutoUnlockForegroundService,
                                devices,
                                device.deviceId.toString()
                            )
                            if (device.deviceStatus != CHDeviceStatus.Unlocked) {
                                (device as? CHSesame5)?.unlock(
                                    historytag = UserUtils.getEnvironmentIdWithByte()
                                ) { }
                                (device as? CHSesame2)?.unlock { }
                            }
                        }

                        device.deviceStatus == CHDeviceStatus.ReceivedAdV -> {
                            device.connect { }
                        }
                    }
                }
                delay(1_000)
            }
        }
    }

    private fun hasBluetoothPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.auto_mode),
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        serviceScope.cancel()
        isLive = false
        if (!candyHouseApplication.appLifecycleObserver.isAppForeground) {
            CHBleManager.disableScan { }
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "AutoUnlockConnectedDeviceChannel"
        private const val NOTIFICATION_ID = 1001

        @Volatile
        var isLive = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, AutoUnlockForegroundService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: RuntimeException) {
                // Geofence events are normally exempt from background-start restrictions.
                // If the OS still rejects this start, keep the armed flag for a later retry.
                L.d("AutoUnlockService", "Unable to start connected-device service: ${e.message}")
            }
        }
    }
}
