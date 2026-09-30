package co.candyhouse.app

import android.app.Application
import co.candyhouse.app.ble.BleController
import co.candyhouse.app.connecteddevice.BackgroundWebRuntime
import co.candyhouse.app.connecteddevice.DevicePreferences
import co.candyhouse.app.data.BleBackend
import co.candyhouse.app.data.auth.LegacySession
import co.candyhouse.app.data.local.DeviceKeyDatabase
import co.candyhouse.app.push.TopicSubscriptionManager
import co.candyhouse.sesame.ble.CHBleManager
import co.candyhouse.sesame.ble.CHBleSupport
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class SesameApp : Application() {
    val runtimeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val ble by lazy { BleController(runtimeScope, bleBackend) {} }
    val devicePreferences by lazy { DevicePreferences(this) }
    lateinit var bleBackend: BleBackend
        private set

    lateinit var subscriptionManager: TopicSubscriptionManager
        private set

    var activityPresent = false
    private var backgroundWeb: BackgroundWebRuntime? = null
    fun ensureBackgroundWeb() {
        if (!activityPresent && backgroundWeb == null) backgroundWeb = BackgroundWebRuntime(this)
    }

    fun stopBackgroundWeb() {
        backgroundWeb?.close(); backgroundWeb = null; if (!activityPresent) bleBackend.requestCloud = null
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseCrashlytics.getInstance().apply {
            isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
            if (BuildConfig.DEBUG) deleteUnsentReports()
        }
        LegacySession.initialize(this)
        DeviceKeyDatabase.context = this
        bleBackend = BleBackend(this)
        CHBleSupport.initialize(bleBackend, DeviceKeyDatabase.Keys, BuildConfig.AWS_IDENTITY_POOL_ID)
        CHBleManager(this)
        subscriptionManager = TopicSubscriptionManager(this)
    }
}
