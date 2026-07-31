package co.candyhouse.app.ext.aws

import android.content.Context
import co.candyhouse.app.BuildConfig
import co.candyhouse.sesame.utils.L
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignInOptions
import com.amplifyframework.auth.cognito.options.AuthFlowType
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.options.AuthUpdateUserAttributeOptions
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.AmplifyConfiguration
import com.amplifyframework.kotlin.core.Amplify as KotlinAmplify
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.api.aws.AWSApiPlugin
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
        private var cachedUserState: AWSLoginState = AWSLoginState.SIGNED_OUT

        fun initAmplify(context: Context, callback: ((Boolean) -> Unit)? = null) {
            synchronized(initLock) {
                if (!isInitialized) {
                    try {
                        Amplify.addPlugin(AWSCognitoAuthPlugin())
                        Amplify.addPlugin(AWSApiPlugin())
                        Amplify.configure(
                            AmplifyConfiguration.fromJson(buildAmplifyConfiguration()),
                            context.applicationContext
                        )
                        isInitialized = true
                    } catch (e: AmplifyException) {
                        L.e("AWSStatus", "Amplify initialize failed", e)
                        cachedUserState = AWSLoginState.SIGNED_OUT
                        setAWSLoginStatus(false)
                        callback?.invoke(false)
                        return
                    }
                }
            }

            refreshAuthSession(callback)
        }

        private fun buildAmplifyConfiguration(): JSONObject {
            return JSONObject(
                """
                {
                  "Version": "1.0",
                  "auth": {
                    "plugins": {
                      "awsCognitoAuthPlugin": {
                        "IdentityManager": {
                          "Default": {}
                        },
                        "CredentialsProvider": {
                          "CognitoIdentity": {
                            "Default": {
                              "PoolId": "${BuildConfig.AWS_IDENTITY_POOL_ID}",
                              "Region": "ap-northeast-1"
                            }
                          }
                        },
                        "CognitoUserPool": {
                          "Default": {
                            "PoolId": "${BuildConfig.AWS_USER_POOL_ID}",
                            "AppClientId": "${BuildConfig.AWS_APP_CLIENT_ID}",
                            "Region": "ap-northeast-1"
                          }
                        },
                        "Auth": {
                          "Default": {
                            "authenticationFlowType": "CUSTOM_AUTH"
                          }
                        }
                      }
                    }
                  },
                  "api": {
                    "plugins": {
                      "awsAPIPlugin": {
                        "chApi": {
                          "endpointType": "REST",
                          "endpoint": "${co.candyhouse.sesame.BuildConfig.ch_server}",
                          "region": "ap-northeast-1",
                          "authorizationType": "AWS_IAM"
                        }
                      }
                    }
                  }
                }
                """.trimIndent()
            )
        }

        fun refreshAuthSession(callback: ((Boolean) -> Unit)? = null) {
            Amplify.Auth.fetchAuthSession(
                { session ->
                    val signedIn = session.isSignedIn
                    cachedUserState = if (signedIn) AWSLoginState.SIGNED_IN else AWSLoginState.SIGNED_OUT
                    setAWSLoginStatus(signedIn)
                    if (signedIn && subUUID == null) {
                        GlobalScope.launch(Dispatchers.IO) {
                            try {
                                setSubUUID(getUserAttributes()["sub"])
                            } catch (e: Exception) {
                                L.e("AWSStatus", "Get user attributes failed", e)
                            }
                        }
                    } else if (!signedIn) {
                        setSubUUID(null)
                    }
                    callback?.invoke(signedIn)
                },
                {
                    cachedUserState = AWSLoginState.SIGNED_OUT
                    setAWSLoginStatus(false)
                    setSubUUID(null)
                    callback?.invoke(false)
                }
            )
        }

        suspend fun refreshAuthSessionNow(): Boolean {
            val session = KotlinAmplify.Auth.fetchAuthSession()
            val signedIn = session.isSignedIn
            cachedUserState = if (signedIn) AWSLoginState.SIGNED_IN else AWSLoginState.SIGNED_OUT
            setAWSLoginStatus(signedIn)
            if (!signedIn) {
                setSubUUID(null)
            }
            return signedIn
        }

        suspend fun getUserAttributes(): Map<String, String> {
            return KotlinAmplify.Auth.fetchUserAttributes()
                .associate { it.key.keyString to it.value }
        }

        suspend fun updateUserName(name: String) {
            KotlinAmplify.Auth.updateUserAttribute(
                AuthUserAttribute(AuthUserAttributeKey.name(), name),
                AuthUpdateUserAttributeOptions.defaults()
            )
        }

        suspend fun signUp(mail: String) {
            val options = AuthSignUpOptions.builder()
                .userAttribute(AuthUserAttributeKey.email(), mail)
                .build()
            KotlinAmplify.Auth.signUp(mail, "dummypwk", options)
        }

        suspend fun signIn(mail: String) =
            KotlinAmplify.Auth.signIn(
                mail,
                "dummypwk",
                AWSCognitoAuthSignInOptions.builder()
                    .authFlowType(AuthFlowType.CUSTOM_AUTH)
                    .build()
            )

        suspend fun confirmSignIn(code: String) =
            KotlinAmplify.Auth.confirmSignIn(code)

        suspend fun signOut() {
            val result = KotlinAmplify.Auth.signOut()
            if (result is AWSCognitoAuthSignOutResult.FailedSignOut) {
                throw result.exception
            }
            cachedUserState = AWSLoginState.SIGNED_OUT
            setAWSLoginStatus(false)
            setSubUUID(null)
        }

        suspend fun getUsername(): String? {
            return runCatching { KotlinAmplify.Auth.getCurrentUser().username }.getOrNull()
        }

        suspend fun getIdToken(): String? {
            val session = KotlinAmplify.Auth.fetchAuthSession()
            if (!session.isSignedIn) return null
            return (session as? AWSCognitoAuthSession)
                ?.userPoolTokensResult
                ?.value
                ?.idToken
        }

        fun getCachedUserState(): AWSLoginState = cachedUserState

        fun isInitialized(): Boolean = isInitialized

        fun isSignedIn(): Boolean = getAWSLoginStatus()

        fun setSubUUID(uuid: String?) {
            L.d("AWSStatus", "setSubUUID: $uuid")
            subUUID = uuid
        }

        fun getSubUUID(): String? {
            return subUUID
        }

        fun setAWSLoginStatus(isLogin: Boolean) {
            this.isLogin = isLogin
            cachedUserState = if (isLogin) AWSLoginState.SIGNED_IN else AWSLoginState.SIGNED_OUT
        }

        fun getLoginStatus(): Boolean {
            return isLogin
        }

        fun getAWSLoginStatus(): Boolean {
            return isInitialized && isLogin
        }
    }
}
