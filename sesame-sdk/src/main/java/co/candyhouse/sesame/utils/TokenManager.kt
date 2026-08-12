package co.candyhouse.sesame.utils

import com.amplifyframework.auth.AWSTemporaryCredentials
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import com.amplifyframework.kotlin.core.Amplify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Token管理类
 *
 * @author frey on 2025/9/15
 */
object TokenManager {

    fun getValidToken(callback: (Result<String?>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { getValidTokenValue() }.fold(
                onSuccess = { callback(Result.success(it)) },
                onFailure = { callback(Result.failure(it)) }
            )
        }
    }

    suspend fun getValidTokenValue(): String? {
        val session = Amplify.Auth.fetchAuthSession()
        if (!session.isSignedIn) return null
        return (session as? AWSCognitoAuthSession)
            ?.userPoolTokensResult
            ?.value
            ?.idToken
    }

    suspend fun getCredentials(): Triple<String, String, String?> {
        val session = Amplify.Auth.fetchAuthSession() as AWSCognitoAuthSession
        val credentials = session.awsCredentialsResult.value
            ?: error("AWS credentials are unavailable")
        return Triple(
            credentials.accessKeyId,
            credentials.secretAccessKey,
            (credentials as? AWSTemporaryCredentials)?.sessionToken
        )
    }
}
