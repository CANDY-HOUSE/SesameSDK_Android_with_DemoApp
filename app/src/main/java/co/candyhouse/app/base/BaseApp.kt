package co.candyhouse.app.base

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.preference.PreferenceManager
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.ext.AppLifecycleObserver
import co.candyhouse.app.ext.aws.AWSStatus
import co.candyhouse.server.CHIRAPIManager
import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.server.CHIotManagerPublic
import co.candyhouse.sesame.utils.L
import co.receiver.TopicSubscriptionManager
import co.utils.SharedPreferencesUtils
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.ktx.remoteConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 应用基础类
 *
 * @author frey on 2025/3/27
 */
open class BaseApp : Application() {

    // 应用级协程作用域
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var appLifecycleObserver: AppLifecycleObserver

    lateinit var subscriptionManager: TopicSubscriptionManager

    override fun onCreate() {
        super.onCreate()
        setupLifecycleObserver()
        initializeServices()
    }

    private fun setupLifecycleObserver() {
        appLifecycleObserver = AppLifecycleObserver().also {
            ProcessLifecycleOwner.get().lifecycle.addObserver(it)
        }
    }

    private fun initializeServices() {
        with(applicationContext) {
            CHBleManager(this)
            CHIRAPIManager.initialize(this)
            SharedPreferencesUtils.init(PreferenceManager.getDefaultSharedPreferences(this))
        }
        setupCrashlytics()
        setupRemoteConfig()
        initializeAWS()
        setupSubscriptionManager()
    }

    private fun setupCrashlytics() {
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
    }

    private fun setupRemoteConfig() {
        val remoteConfig = Firebase.remoteConfig

        remoteConfig.setDefaultsAsync(
            mapOf(
                "show_shop_item" to true,
                "shop_home_url" to "https://jp.candyhouse.co/collections/frontpage"
            )
        )

        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                L.d("RemoteConfig", "Updated keys: " + configUpdate.updatedKeys)

                remoteConfig.activate().addOnCompleteListener {
                    L.d("RemoteConfig", "Updated keys is successful")
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                L.e("RemoteConfig", "Config update error", error)
            }
        })

        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                L.d("RemoteConfig", "Config fetched and activated")
            }
        }
    }

    private fun initializeAWS() {
        AWSStatus.initAWSMobileClient(this)
    }

    fun initIoTConnection() {
        appScope.launch {
            CHIotManagerPublic.startConnection()
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        appScope.cancel() // 取消所有协程
    }

    private fun setupSubscriptionManager() {
        subscriptionManager = TopicSubscriptionManager(this)
        subscriptionManager.checkAndSubscribeToTopics()
    }
}