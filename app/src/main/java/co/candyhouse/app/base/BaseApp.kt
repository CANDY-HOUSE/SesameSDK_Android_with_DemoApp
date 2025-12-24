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
import co.receiver.TopicSubscriptionManager
import co.utils.AppIdentifyIdUtil
import co.utils.SharedPreferencesUtils
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
        AppIdentifyIdUtil.warmUp(this)
        setupCrashlytics()
        initializeAWS()
        setupSubscriptionManager()
    }

    private fun setupCrashlytics() {
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
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