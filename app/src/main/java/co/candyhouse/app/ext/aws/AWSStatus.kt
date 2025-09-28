package co.candyhouse.app.ext.aws

import android.content.Context
import co.candyhouse.sesame.open.isInternetAvailable
import co.candyhouse.sesame.utils.L
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.UserState
import com.amazonaws.mobile.client.UserStateDetails
import com.amazonaws.mobile.config.AWSConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject

class AWSStatus {
    companion object {
        private var isLogin: Boolean = false
        private var subUUID: String? = null
        private var isInitialized = false
        private val initLock = Any()
        private var cachedUserState: UserState = UserState.SIGNED_OUT

        fun initAWSMobileClient(context: Context, callback: ((Boolean) -> Unit)? = null) {
            synchronized(initLock) {
                if (isInitialized) {
                    callback?.invoke(isLogin)
                    return
                }

                AWSMobileClient.getInstance().initialize(
                    context.applicationContext,
                    AWSConfiguration(JSONObject(AWSConfig.jpDevTeam)),
                    object : Callback<UserStateDetails?> {
                        override fun onResult(result: UserStateDetails?) {
                            isInitialized = true
                            cachedUserState = result?.userState ?: UserState.SIGNED_OUT
                            when (cachedUserState) {
                                UserState.SIGNED_IN -> {
                                    setAWSLoginStatus(true)
                                    if (subUUID == null) {
                                        GlobalScope.launch(Dispatchers.IO) {
                                            try {
                                                val attributes = AWSMobileClient.getInstance().getUserAttributes()
                                                setSubUUID(attributes["sub"])
                                            } catch (e: Exception) {
                                                L.e("AWSStatus", "获取用户属性失败", e)
                                            }
                                        }
                                    }
                                }

                                UserState.SIGNED_OUT -> {
                                    setAWSLoginStatus(false)
                                    setSubUUID(null)
                                }

                                else -> {
                                    setAWSLoginStatus(false)
                                    setSubUUID(null)
                                }
                            }
                            callback?.invoke(isLogin)
                        }

                        override fun onError(e: Exception) {
                            cachedUserState = UserState.SIGNED_OUT
                            setAWSLoginStatus(false)
                            callback?.invoke(false)
                        }
                    })
            }
        }

        fun getCachedUserState(): UserState = cachedUserState

        fun isInitialized(): Boolean = isInitialized

        fun setSubUUID(uuid: String?) {
            L.d("AWSStatus", "setSubUUID: $uuid")
            subUUID = uuid
        }

        fun getSubUUID(): String? {
            return subUUID
        }

        fun setAWSLoginStatus(isLogin: Boolean) {
            this.isLogin = isLogin
        }

        fun getLoginStatus(): Boolean {
            return isLogin
        }

        fun getAWSLoginStatus(): Boolean {
            return if (isInitialized && isLogin) {
                isInternetAvailable()
            } else {
                false
            }
        }
    }
}
