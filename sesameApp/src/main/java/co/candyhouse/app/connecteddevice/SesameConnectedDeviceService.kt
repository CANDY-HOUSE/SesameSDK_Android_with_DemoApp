package co.candyhouse.app.connecteddevice

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
import android.content.res.Configuration
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.IBinder
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import co.candyhouse.app.R
import co.candyhouse.app.SesameActivity
import co.candyhouse.app.SesameApp
import co.candyhouse.app.ble.LockSettings
import co.candyhouse.sesame.ble.CHDeviceLoginStatus
import co.candyhouse.sesame.ble.CHDeviceStatus
import co.candyhouse.sesame.ble.CHDevices
import co.candyhouse.sesame.ble.CHSesameLock
import co.candyhouse.sesame.ble.os2.bot.CHSesameBot
import co.candyhouse.sesame.ble.os3.bot2.CHSesameBot2
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResultState
import co.candyhouse.sesame.utils.L
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import kotlin.time.Duration.Companion.milliseconds

class SesameConnectedDeviceService : Service() {
    private val app get() = application as SesameApp
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val shown = mutableSetOf<Int>()
    private var updates: Job? = null
    private var pendingCommands = 0
    private var widgetChannel = CHANNEL
    private val network by lazy { getSystemService(ConnectivityManager::class.java) }
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            scope.launch { refresh() }
        }

        override fun onLost(network: Network) {
            scope.launch { refresh() }
        }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            scope.launch { refresh() }
        }
    }
    private val observer: (JSONArray) -> Unit = { refresh() }
    private val bluetooth = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON) app.ble.startBackground(); refresh()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate(); live = this
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            val oldChannel = manager.getNotificationChannel(CHANNEL)
            // Early H5 builds created www as LOW. Android cannot raise an existing channel's importance.
            // Keep user-selected settings; only migrate the unchanged, incorrectly created channel.
            if (manager.getNotificationChannel(WIDGET_CHANNEL) != null ||
                (Build.VERSION.SDK_INT >= 29 && oldChannel?.importance == NotificationManager.IMPORTANCE_LOW && !oldChannel.hasUserSetImportance())
            ) widgetChannel = WIDGET_CHANNEL
            manager.createNotificationChannel(NotificationChannel(widgetChannel, "sesame widget", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "sesame_widget"
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            })
            manager.createNotificationChannel(NotificationChannel(SERVICE_CHANNEL, getString(R.string.sesame_widget), NotificationManager.IMPORTANCE_LOW))
        }
        network.registerDefaultNetworkCallback(networkCallback)
        app.ble.observers.add(observer)
        ContextCompat.registerReceiver(this, bluetooth, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED), ContextCompat.RECEIVER_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            startForeground(
                SERVICE_ID,
                NotificationCompat.Builder(this, SERVICE_CHANNEL).setSmallIcon(R.drawable.ic_notification).setContentTitle(getString(R.string.Sesame)).setContentText(
                    getString(
                        if (app.ble.allDevices()
                                .any { app.devicePreferences.enabled("wid", it.deviceId.toString()) }
                        ) R.string.sesame_widget else R.string.auto_unlock_service_notification
                    )
                ).setOngoing(true).setSilent(true).setOnlyAlertOnce(true).setPriority(NotificationCompat.PRIORITY_LOW).build()
            )
        } catch (e: RuntimeException) {
            L.e("SesameWidget", "Unable to start connected service", e); stopSelf(); return START_NOT_STICKY
        }
        scope.launch {
            app.ble.restoreSaved(); app.ensureBackgroundWeb(); app.ble.startBackground()
            val id = intent?.getStringExtra("deviceUUID");
            val action = intent?.getStringExtra("operation")
            if (action != null) {
                val targets =
                    app.ble.allDevices().filter { (id == null && app.devicePreferences.enabled("wid", it.deviceId.toString())) || it.deviceId.toString().equals(id, true) }
                targets.forEach { device -> launch { command(device.deviceId.toString(), action) } }
            }
            refresh()
        }
        return START_STICKY
    }

    private suspend fun command(id: String, operation: String) {
        pendingCommands++
        try {
            withTimeout(30000.milliseconds) {
                runCatching {
                    val device = requireNotNull(app.ble.device(id))
                    if (device.deviceStatus.value == CHDeviceLoginStatus.logined) {
                        LockSettings.awaitResult<CHEmpty> { result ->
                            app.ble.handle("home.command", JSONObject().put("deviceUUID", id).put("operation", operation)) { ok, error ->
                                if (ok) result(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
                                else result(Result.failure(IllegalStateException(error)))
                            }
                        }
                    } else requireNotNull(app.bleBackend.requestCloud)("widgetCommand", JSONObject().put("operation", operation), id)
                }.onFailure { L.e("SesameWidget", "Command failed device=$id", it) }
            }
        } finally {
            pendingCommands--; refresh()
        }
    }

    private fun refresh() {
        val devices = app.ble.allDevices()
        devices.filter {
            app.devicePreferences.enabled("nohand", it.deviceId.toString()) && app.devicePreferences.enabled(
                "nohandg",
                it.deviceId.toString()
            ) && it.deviceStatus.value == CHDeviceLoginStatus.logined && it.rssi != null
        }.forEach { device ->
            val id = device.deviceId.toString(); app.devicePreferences.setEnabled("nohandg", id, false)
            if (device.deviceStatus != CHDeviceStatus.Unlocked) scope.launch { command(id, "unlock") }
        }
        if (updates?.isActive == true) return
        updates = scope.launch {
            delay(300.milliseconds)
            val widgets = app.ble.allDevices().filter { it is CHSesameLock && app.devicePreferences.enabled("wid", it.deviceId.toString()) }
            val manager = NotificationManagerCompat.from(this@SesameConnectedDeviceService)
            val expected = widgets.map { it.deviceId.hashCode() }.toMutableSet().apply { if (widgets.size > 1) add(ALL_ID) }
            (shown - expected).forEach { manager.cancel(it); shown.remove(it) }
            if (manager.areNotificationsEnabled()) {
                widgets.forEach { device ->
                    manager.notify(device.deviceId.hashCode(), notification(device)); shown.add(device.deviceId.hashCode()); delay(300.milliseconds)
                }
                if (widgets.size > 1) {
                    manager.notify(ALL_ID, allNotification()); shown.add(ALL_ID)
                }
            }
            val armed =
                app.ble.allDevices().any { app.devicePreferences.enabled("nohand", it.deviceId.toString()) && app.devicePreferences.enabled("nohandg", it.deviceId.toString()) }
            if ((!manager.areNotificationsEnabled() || widgets.isEmpty()) && !armed && pendingCommands == 0) stopSelf()
        }
    }

    private fun action(id: String?, operation: String): PendingIntent {
        val intent = Intent(this, SesameConnectedDeviceService::class.java).setAction("$id:$operation").putExtra("operation", operation).putExtra("deviceUUID", id)
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun builder() =
        NotificationCompat.Builder(this, widgetChannel).setSmallIcon(R.drawable.ic_notification).setStyle(NotificationCompat.DecoratedCustomViewStyle()).setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, SesameActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

    private fun notification(device: CHDevices): Notification {
        val id = device.deviceId.toString()
        val cloud = (0 until app.ble.cloudRows.length()).map { app.ble.cloudRows.getJSONObject(it) }.firstOrNull { it.optString("deviceUUID").equals(id, true) }
        val connected = device.deviceStatus.value == CHDeviceLoginStatus.logined
        val online = network.getNetworkCapabilities(network.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true && cloud?.optJSONObject("stateInfo")
            ?.optBoolean("wm2State") == true
        val status = if (connected) device.deviceStatus.name.lowercase() else if (online) cloud?.optJSONObject("stateInfo")
            ?.optString("CHSesame2Status") else device.deviceStatus.name.lowercase()
        val icon = if (device is CHSesameBot || device is CHSesameBot2) when (status) {
            "locked", "unlocked", "moved" -> R.drawable.swtich_unlocked
            "receivedadv", "bleconnecting" -> R.drawable.swtich_receive_ble
            "discoverservices", "waitingforauth" -> R.drawable.swtich_waitgatt
            "blelogining", "registering" -> R.drawable.swtich_logining
            else -> R.drawable.swtich_no_ble
        } else when (status) {
            "locked" -> R.drawable.icon_lock
            "unlocked", "moved" -> R.drawable.icon_unlock
            "receivedadv", "bleconnecting" -> R.drawable.icon_receiveblee
            "discoverservices", "waitingforauth" -> R.drawable.icon_waitgatt
            "blelogining", "registering" -> R.drawable.icon_logining
            "nosettings" -> R.drawable.icon_nosetting
            else -> R.drawable.icon_nosignal
        }
        val views = RemoteViews(packageName, R.layout.cell_weget_unlock).apply {
            setTextViewText(R.id.title, cloud?.optString("deviceName") ?: app.ble.deviceName(device))
            setImageViewResource(R.id.toggle, icon)
            setTextColor(
                R.id.title,
                if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) Color.WHITE else Color.BLACK
            )
            setOnClickPendingIntent(R.id.toggle, action(id, "toggle"))
            setImageViewResource(R.id.bl_img, if (connected) R.drawable.ic_bluetooth else R.drawable.ic_bluetooth_grey)
            setImageViewResource(R.id.wifi_img, if (online) R.drawable.ic_wifi_blue else R.drawable.ic_wifi_grey)
            setImageViewResource(R.id.hand_img, if (app.devicePreferences.enabled("nohandg", id)) R.drawable.ic_autounlock_active else R.drawable.ic_autounlock)
            setViewVisibility(R.id.hand_img, if (app.devicePreferences.enabled("nohand", id)) View.VISIBLE else View.GONE)
        }
        return builder().setCustomContentView(views).setCustomBigContentView(views).build()
    }

    private fun allNotification(): Notification {
        val views = RemoteViews(packageName, R.layout.cell_weget).apply {
            setOnClickPendingIntent(R.id.lock_all, action(null, "lock")); setOnClickPendingIntent(
            R.id.unlock_all,
            action(null, "unlock")
        )
        }
        return builder().setCustomContentView(views).build()
    }

    override fun onDestroy() {
        live = null; app.ble.observers.remove(observer); app.ble.stopBackground(); unregisterReceiver(bluetooth); network.unregisterNetworkCallback(networkCallback)
        shown.forEach { NotificationManagerCompat.from(this).cancel(it) }; app.stopBackgroundWeb()
        if (!app.activityPresent) app.ble.close()
        scope.cancel(); super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "www"
        private const val WIDGET_CHANNEL = "SesameLockWidgetChannel"
        private const val SERVICE_CHANNEL = "SesameConnectedDeviceChannel"
        private const val SERVICE_ID = 9011
        private const val ALL_ID = 9012
        var live: SesameConnectedDeviceService? = null; private set
        fun sync(context: Context) {
            val app = context.applicationContext as SesameApp
            val needed = app.ble.allDevices().any {
                val id = it.deviceId.toString(); (app.devicePreferences.enabled("wid", id) && NotificationManagerCompat.from(context)
                .areNotificationsEnabled()) || (app.devicePreferences.enabled("nohand", id) && app.devicePreferences.enabled("nohandg", id))
            }
            if (!needed) {
                context.stopService(Intent(context, SesameConnectedDeviceService::class.java)); return
            }
            try {
                ContextCompat.startForegroundService(context, Intent(context, SesameConnectedDeviceService::class.java))
            } catch (e: RuntimeException) {
                L.e("SesameWidget", "Connected service unavailable", e)
            }
        }
    }
}
