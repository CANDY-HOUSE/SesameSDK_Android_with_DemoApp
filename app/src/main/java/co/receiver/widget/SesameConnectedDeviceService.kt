package co.receiver.widget

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import co.candyhouse.app.R
import co.candyhouse.app.candyHouseApplication
import co.candyhouse.app.tabs.MainActivity
import co.candyhouse.app.tabs.devices.ssm2.getIsNOHand
import co.candyhouse.app.tabs.devices.ssm2.getIsNOHandG
import co.candyhouse.app.tabs.devices.ssm2.getIsWidget
import co.candyhouse.app.tabs.devices.ssm2.setIsNOHandG
import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.devices.CHSesame2
import co.candyhouse.sesame.open.devices.CHSesame5
import co.candyhouse.sesame.open.devices.CHSesameBike
import co.candyhouse.sesame.open.devices.CHSesameBike2
import co.candyhouse.sesame.open.devices.CHSesameBot
import co.candyhouse.sesame.open.devices.CHSesameBot2
import co.candyhouse.sesame.open.devices.CHWifiModule2Delegate
import co.candyhouse.sesame.open.devices.base.CHDeviceLoginStatus
import co.candyhouse.sesame.open.devices.base.CHDeviceStatus
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.open.devices.base.CHSesameLock
import co.candyhouse.sesame.server.CHAPIClientBiz
import co.candyhouse.sesame.server.CHIotManagerPublic
import co.candyhouse.sesame.server.dto.ensureSafeStateInfo
import co.candyhouse.sesame.utils.L
import co.utils.UserUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Collections

class SesameConnectedDeviceService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val registeredDevices = Collections.synchronizedSet(mutableSetOf<CHDevices>())
    private var devices: List<CHDevices> = emptyList()
    private var isForeground = false
    private var connectivityManager: ConnectivityManager? = null
    private var validatedNetwork: Network? = null
    private var bluetoothStateReceiverRegistered = false
    private val iotReconnectedListener = ::restoreWidgetStatesAfterIotReconnect

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_OFF,
                BluetoothAdapter.STATE_ON -> refreshWidgetsAfterTransportChanged("Bluetooth")
            }
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val isValidated =
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            if (!isValidated) {
                if (validatedNetwork == network) {
                    validatedNetwork = null
                    refreshWidgetsAfterTransportChanged("Network unavailable")
                }
                return
            }
            if (validatedNetwork == network) return

            validatedNetwork = network
            refreshWidgetsAfterTransportChanged("Network available")
            if (widgetModeActive) {
                CHIotManagerPublic.reconnectImmediatelyIfWaiting()
            }
        }

        override fun onLost(network: Network) {
            if (validatedNetwork == network) {
                validatedNetwork = null
                refreshWidgetsAfterTransportChanged("Network lost")
            }
        }
    }

    private val deviceDelegate = object : CHWifiModule2Delegate {
        override fun onBleDeviceStatusChanged(
            device: CHDevices,
            status: CHDeviceStatus,
            shadowStatus: CHDeviceStatus?
        ) {
            handleDeviceState(device)
            refreshWidget(device)
        }
    }

    override fun onCreate() {
        super.onCreate()
        connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        runCatching {
            connectivityManager?.registerDefaultNetworkCallback(networkCallback)
        }.onFailure {
            L.e(LOG_TAG, "Unable to observe network recovery", it)
        }
        runCatching {
            ContextCompat.registerReceiver(
                this,
                bluetoothStateReceiver,
                IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
                ContextCompat.RECEIVER_EXPORTED
            )
            bluetoothStateReceiverRegistered = true
        }.onFailure {
            L.e(LOG_TAG, "Unable to observe Bluetooth state", it)
        }
        CHIotManagerPublic.addOnReconnectedListener(iotReconnectedListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isForeground) {
            isForeground = startForegroundSafely(intent?.action == ACTION_AUTO_UNLOCK)
            if (!isForeground) {
                stopSelf(startId)
                return START_NOT_STICKY
            }
            isLive = true
        }

        loadDevices(intent?.action, startId)
        return START_STICKY
    }

    private fun loadDevices(action: String?, startId: Int) {
        CHDeviceManager.getCandyDevices { result ->
            result.onSuccess { state ->
                devices = state.data
                registerDeviceDelegates(devices)
                refreshServiceModes()
                handleWidgetAction(action, devices)
            }
            result.onFailure { error ->
                L.e(LOG_TAG, "Unable to load devices", error)
                stopSelf(startId)
            }
        }
    }

    private fun registerDeviceDelegates(currentDevices: List<CHDevices>) {
        currentDevices.forEach { device ->
            if (registeredDevices.add(device)) {
                device.multicastDelegate.addDelegate(deviceDelegate, Dispatchers.Unconfined)
            }
        }

        registeredDevices.toList()
            .filterNot(currentDevices::contains)
            .forEach { device ->
                device.multicastDelegate.removeDelegate(deviceDelegate, Dispatchers.Unconfined)
                registeredDevices.remove(device)
            }
    }

    private fun refreshServiceModes() {
        val canPostNotifications =
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        val hasWidgets = canPostNotifications &&
                devices.any { it is CHSesameLock && it.getIsWidget() }
        val hasAutoUnlock = devices.any { it.getIsNOHand() && it.getIsNOHandG() }
        loadedDeviceIds = devices.mapTo(mutableSetOf()) { it.deviceId.toString() }
        widgetModeActive = hasWidgets
        autoUnlockModeActive = hasAutoUnlock

        if (!hasWidgets && !hasAutoUnlock) {
            stopSelf()
            return
        }

        if (hasBluetoothPermission()) {
            CHBleManager.enableScan { }
        }

        updateForegroundNotification(hasWidgets)
        devices.forEach(::handleDeviceState)
    }

    private fun handleDeviceState(device: CHDevices) {
        val supportsWidget = device is CHSesameLock && device.getIsWidget()
        val handlesWidgetFallback = supportsWidget && MainActivity.activity == null
        val waitsForAutoUnlock = device.getIsNOHand() && device.getIsNOHandG()
        if (!handlesWidgetFallback && !waitsForAutoUnlock) return

        if (device.deviceStatus == CHDeviceStatus.ReceivedAdV) {
            device.connect { }
            return
        }

        if (waitsForAutoUnlock &&
            device.deviceStatus.value == CHDeviceLoginStatus.logined &&
            device.rssi != null
        ) {
            device.setIsNOHandG(false)
            if (device.deviceStatus != CHDeviceStatus.Unlocked) {
                (device as? CHSesame5)?.unlock(
                    historytag = UserUtils.getEnvironmentIdWithByte()
                ) { }
                (device as? CHSesame2)?.unlock { }
            }
            refreshWidget(device)
            refreshServiceModes()
        }
    }

    private fun refreshWidget(device: CHDevices) {
        if (MainActivity.activity != null ||
            device !is CHSesameLock ||
            !device.getIsWidget()
        ) {
            return
        }
        SesameWidgetNotificationManager.refresh(
            this,
            devices,
            device.deviceId.toString()
        )
    }

    private fun refreshWidgetsAfterTransportChanged(source: String) {
        if (MainActivity.activity != null || !widgetModeActive) return
        L.d(LOG_TAG, "Refreshing widgets after $source changed")
        SesameWidgetNotificationManager.refresh(this, devices)
    }

    private fun restoreWidgetStatesAfterIotReconnect() {
        if (MainActivity.activity != null) return

        val widgetDevices = devices.filter { it is CHSesameLock && it.getIsWidget() }
        if (widgetDevices.isEmpty()) return

        L.d(LOG_TAG, "Restoring ${widgetDevices.size} widget states after IoT reconnect")
        CHAPIClientBiz.getDevicesList { result ->
            result.onSuccess { response ->
                val stateById = response.data.associateBy {
                    it.deviceUUID.lowercase()
                }
                widgetDevices.forEach { device ->
                    stateById[device.deviceId.toString().lowercase()]?.let { userKey ->
                        CHDeviceManager.applyServerState(
                            device,
                            userKey.ensureSafeStateInfo().stateInfo
                        )
                    }
                }

                if (serviceScope.isActive &&
                    MainActivity.activity == null &&
                    widgetModeActive
                ) {
                    SesameWidgetNotificationManager.refresh(this, devices)
                }

                L.d(LOG_TAG, "Widget states restored after IoT reconnect")
            }
            result.onFailure { error ->
                L.e(LOG_TAG, "Unable to restore widget states after IoT reconnect", error)
            }
        }
    }

    private fun handleWidgetAction(action: String?, currentDevices: List<CHDevices>) {
        if (action == null) return
        serviceScope.launch {
            when {
                action.startsWith(ACTION_TOGGLE_PREFIX) -> {
                    currentDevices.firstOrNull { device ->
                        device is CHSesameLock &&
                                device.getIsWidget() &&
                                action == ACTION_TOGGLE_PREFIX + device.deviceId.hashCode()
                    }?.let(::toggle)
                }

                action.startsWith(ACTION_OPEN_ALL_PREFIX) -> {
                    currentDevices.filter { it is CHSesameLock && it.getIsWidget() }
                        .forEach { device ->
                            actionOpenAll(device, true)
                            delay(500)
                        }
                }

                action.startsWith(ACTION_CLOSE_ALL_PREFIX) -> {
                    currentDevices.filter { it is CHSesameLock && it.getIsWidget() }
                        .forEach { device ->
                            actionOpenAll(device, false)
                            delay(500)
                        }
                }
            }
        }
    }

    private fun toggle(device: CHDevices) {
        (device as? CHSesame5)?.toggle(
            historytag = UserUtils.getEnvironmentIdWithByte()
        ) { }
        (device as? CHSesame2)?.toggle { }
        (device as? CHSesameBike)?.unlock { }
        (device as? CHSesameBike2)?.unlock(
            historytag = UserUtils.getEnvironmentIdWithByte()
        ) { }
        (device as? CHSesameBot)?.click { }
        (device as? CHSesameBot2)?.click(
            historytag = UserUtils.getEnvironmentIdWithByte()
        ) { }
    }

    private fun actionOpenAll(device: CHDevices, open: Boolean) {
        when (device) {
            is CHSesame5 -> {
                if (open) {
                    device.lock(historytag = UserUtils.getEnvironmentIdWithByte()) { }
                } else {
                    device.unlock(historytag = UserUtils.getEnvironmentIdWithByte()) { }
                }
            }

            is CHSesame2 -> {
                if (open) device.lock { } else device.unlock { }
            }

            is CHSesameBot -> device.click { }
            is CHSesameBot2 -> {
                device.click(historytag = UserUtils.getEnvironmentIdWithByte()) { }
            }

            is CHSesameBike -> if (!open) device.unlock { }
            is CHSesameBike2 -> if (!open) {
                device.unlock(historytag = UserUtils.getEnvironmentIdWithByte()) { }
            }
        }
    }

    private fun startForegroundSafely(autoUnlockOnly: Boolean): Boolean {
        createNotificationChannel()
        return try {
            val notification = createNotification(autoUnlockOnly)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            true
        } catch (error: RuntimeException) {
            L.e(LOG_TAG, "Unable to promote connected-device service", error)
            false
        }
    }

    private fun updateForegroundNotification(hasWidgets: Boolean) {
        if (!isForeground) return
        getSystemService(NotificationManager::class.java)?.notify(
            NOTIFICATION_ID,
            createNotification(autoUnlockOnly = !hasWidgets)
        )
    }

    private fun createNotification(autoUnlockOnly: Boolean): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.small_icon)
            .setContentTitle(
                getString(if (autoUnlockOnly) R.string.auto_mode else R.string.Sesame)
            )
            .setContentText(
                getString(
                    if (autoUnlockOnly) {
                        R.string.auto_unlock_service_notification
                    } else {
                        R.string.sesame_widget
                    }
                )
            )
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
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.sesame_widget),
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun hasBluetoothPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        CHIotManagerPublic.removeOnReconnectedListener(iotReconnectedListener)
        if (bluetoothStateReceiverRegistered) {
            runCatching { unregisterReceiver(bluetoothStateReceiver) }
            bluetoothStateReceiverRegistered = false
        }
        runCatching {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        }
        connectivityManager = null
        validatedNetwork = null
        registeredDevices.toList().forEach { device ->
            device.multicastDelegate.removeDelegate(deviceDelegate, Dispatchers.Unconfined)
        }
        registeredDevices.clear()
        serviceScope.cancel()
        isLive = false
        widgetModeActive = false
        autoUnlockModeActive = false
        loadedDeviceIds = emptySet()
        if (!candyHouseApplication.appLifecycleObserver.isAppForeground) {
            CHBleManager.disableScan { }
        }
        if (isForeground) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        // 服务日志标签。
        private const val LOG_TAG = "SesameConnectedService"
        // 通知栏小组件和自动解锁共用的通知渠道。
        private const val CHANNEL_ID = "SesameConnectedDeviceChannel"
        // 合并后的前台服务通知 ID。
        private const val NOTIFICATION_ID = 1001
        // 服务模式同步动作。
        private const val ACTION_SYNC = "sesame.action.connected.sync"
        // 地理围栏出圈后启动自动解锁等待。
        private const val ACTION_AUTO_UNLOCK = "sesame.action.connected.auto_unlock"
        // 通知栏单设备开关动作前缀。
        private const val ACTION_TOGGLE_PREFIX = "toggle_ssm"
        // 通知栏全部上锁动作前缀。
        private const val ACTION_OPEN_ALL_PREFIX = "open_all"
        // 通知栏全部解锁动作前缀。
        private const val ACTION_CLOSE_ALL_PREFIX = "close_all"

        @Volatile
        var isLive = false
            private set

        @Volatile
        private var widgetModeActive = false

        @Volatile
        private var autoUnlockModeActive = false

        @Volatile
        private var loadedDeviceIds: Set<String> = emptySet()

        fun sync(
            context: Context,
            hasWidgets: Boolean,
            hasAutoUnlock: Boolean,
            deviceIds: Set<String>
        ) {
            if (!hasWidgets && !hasAutoUnlock) {
                stop(context)
                return
            }
            if (!isLive ||
                hasWidgets != widgetModeActive ||
                hasAutoUnlock != autoUnlockModeActive ||
                deviceIds != loadedDeviceIds
            ) {
                start(context, ACTION_SYNC)
            }
        }

        fun startAutoUnlock(context: Context) {
            if (!hasBluetoothPermission(context)) {
                return
            }
            start(context, ACTION_AUTO_UNLOCK)
        }

        fun widgetActionPendingIntent(
            context: Context,
            requestCode: Int,
            action: String
        ): PendingIntent {
            val intent = Intent(context, SesameConnectedDeviceService::class.java)
                .setAction(action)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getService(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
        }

        fun toggleAction(deviceHash: Int): String = ACTION_TOGGLE_PREFIX + deviceHash

        fun openAllAction(requestId: Int): String = ACTION_OPEN_ALL_PREFIX + requestId

        fun closeAllAction(requestId: Int): String = ACTION_CLOSE_ALL_PREFIX + requestId

        fun stop(context: Context) {
            context.stopService(Intent(context, SesameConnectedDeviceService::class.java))
        }

        private fun start(context: Context, action: String) {
            val intent = Intent(context, SesameConnectedDeviceService::class.java)
                .setAction(action)
            try {
                if (isLive) {
                    context.startService(intent)
                } else {
                    ContextCompat.startForegroundService(context, intent)
                }
            } catch (error: RuntimeException) {
                L.e(LOG_TAG, "Unable to start connected-device service", error)
            }
        }

        private fun hasBluetoothPermission(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }
}
