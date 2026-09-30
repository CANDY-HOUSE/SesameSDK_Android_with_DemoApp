package co.candyhouse.app.data.auth

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.edit
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.util.AppIdentity
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import com.amplifyframework.core.AmplifyConfiguration
import com.amplifyframework.kotlin.core.Amplify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.amplifyframework.core.Amplify as CoreAmplify

/** Existing Cognito storage is retained only for upgrade-session handoff. */
@SuppressLint("StaticFieldLeak")
object LegacySession {
    private lateinit var context: Context
    private val configurationLock = Mutex()
    private var configured = false

    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    private suspend fun ensureConfigured() = withContext(Dispatchers.IO) {
        configurationLock.withLock {
            if (!configured) {
                CoreAmplify.addPlugin(AWSCognitoAuthPlugin())
                CoreAmplify.configure(AmplifyConfiguration.fromJson(configuration()), context)
                configured = true
            }
        }
    }

    fun retired() = context.getSharedPreferences("web_session", 0).getBoolean("retired", false)

    suspend fun token(): String? {
        if (retired()) return null
        ensureConfigured()
        if (retired()) return null
        val session = Amplify.Auth.fetchAuthSession() as AWSCognitoAuthSession
        return if (!retired() && session.isSignedIn) requireNotNull(session.userPoolTokensResult.value?.idToken) { "User session unavailable" } else null
    }

    fun guestIdentityId(): String = AppIdentity.get(context)

    suspend fun retire() {
        context.getSharedPreferences("web_session", 0).edit { putBoolean("retired", true) }
        ensureConfigured()
        Amplify.Auth.signOut()
    }

    private fun configuration(): JSONObject {
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
                  }
                }
                """.trimIndent()
        )
    }

}
