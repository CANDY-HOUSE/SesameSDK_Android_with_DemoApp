package co.candyhouse.app.base

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.preference.PreferenceManager
import candyhouse.sesameos.ir.server.CHIRAPIManager
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.ext.AppLifecycleObserver
import co.candyhouse.app.ext.aws.AWSStatus
import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.server.CHIotManagerPublic
import co.candyhouse.sesame.utils.L
import co.utils.SharedPreferencesUtils
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
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
        setupFirebase()
        setupCrashlytics()
        initializeAWS()
    }

    private fun setupFirebase() {
        Firebase.messaging.token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                task.result?.let { fcmToken ->
                    SharedPreferencesUtils.deviceToken = fcmToken
                    L.d("sf", "fcmToken:$fcmToken")
                }
            }
        }
    }

    private fun setupCrashlytics() {
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
    }

    private fun initializeAWS() {
        AWSStatus.initAWSMobileClient()
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
}