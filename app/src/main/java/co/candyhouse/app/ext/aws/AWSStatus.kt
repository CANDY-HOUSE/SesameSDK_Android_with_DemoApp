package co.candyhouse.app.ext.aws

import co.candyhouse.app.tabs.MainActivity
import co.candyhouse.sesame.open.isInternetAvailable
import co.candyhouse.sesame.utils.L
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.UserState
import com.amazonaws.mobile.client.UserStateDetails
import com.amazonaws.mobile.config.AWSConfiguration
import org.json.JSONObject

class AWSStatus {

    companion object {
        private var isLogin: Boolean = false
        private var subUUID: String? = null

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

        fun checkIsCompany(): Boolean {
            if (isLogin) {
                val email = AWSMobileClient.getInstance()?.username ?: return false
                if (email.isNotEmpty()) {
                    return listOf(
                        "@candyhouse.co",
                        "@cn.candyhouse.co",
                        "@b.b"
                    ).any { email.lowercase().endsWith(it) }
                }
            }
            return false
        }

        /**
         * 初始化AWSMobileClient
         */
        fun initAWSMobileClient() {
            AWSMobileClient.getInstance().initialize(
                MainActivity.activity,
                AWSConfiguration(JSONObject(AWSConfig.jpDevTeam)),
                object : Callback<UserStateDetails?> {
                    override fun onResult(result: UserStateDetails?) {
                        when (result?.userState) {
                            UserState.SIGNED_IN -> {
                                setAWSLoginStatus(true)
                                if(subUUID == null) {
                                    setSubUUID(AWSMobileClient.getInstance().getUserAttributes()["sub"])
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
                    }

                    override fun onError(e: Exception) {
                        setAWSLoginStatus(false)
                    }
                })
        }

        fun getAWSLoginStatus(): Boolean {
            initAWSMobileClient()

            return if (isLogin) {
                isInternetAvailable()
            } else {
                false
            }
        }
    }

}