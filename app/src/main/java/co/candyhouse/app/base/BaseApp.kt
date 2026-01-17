package co.candyhouse.app.base

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.preference.PreferenceManager
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.ext.AppLifecycleObserver
import co.candyhouse.app.ext.aws.AWSStatus
import co.candyhouse.app.ext.webview.manager.WebViewSafeInitializer
import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.server.CHAPIClientBiz
import co.candyhouse.sesame.server.CHIotManagerPublic
import co.candyhouse.sesame.utils.AppIdentifyIdUtil
import co.candyhouse.sesame.utils.SharedPreferencesUtils
import co.receiver.TopicSubscriptionManager
import com.amazonaws.mobile.client.AWSMobileClient
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
            SharedPreferencesUtils.init(PreferenceManager.getDefaultSharedPreferences(this))
        }
        AppIdentifyIdUtil.warmUp(this)
        setupCrashlytics()
        initializeAWS()
        initializeWebViewProvider()
        setupSubscriptionManager()
    }

    private fun setupCrashlytics() {
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
    }

    private fun initializeAWS() {
        AWSStatus.initAWSMobileClient(this)
        setCHAPIClient()
    }

    private fun initializeWebViewProvider() {
        Handler(Looper.getMainLooper()).post {
            WebViewSafeInitializer.preloadWebViewProvider(this)
        }
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

    private fun setCHAPIClient() {
        CHAPIClientBiz.initialize(
            context = this,
            credentialsProvider = AWSMobileClient.getInstance(),
            region = "ap-northeast-1",
            apiKey = co.candyhouse.sesame.BuildConfig.API_GATEWAY_API_KEY
        )
    }
}