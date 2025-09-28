package co.candyhouse.server

import co.candyhouse.app.ext.TokenManager
import co.candyhouse.app.tabs.account.CHUserKey
import co.utils.SharedPreferencesUtils
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobileconnectors.apigateway.ApiClientFactory
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext

typealias CHResult<T> = (Result<CHResultState<T>>) -> Unit

sealed class CHResultState<T>(val data: T) {
    open class CHResultStateNetworks<T>(data: T) : CHResultState<T>(data)
    open class CHResultStateBle<T>(data: T) : CHResultState<T>(data)
}

object CHLoginAPIManager {
    private lateinit var jpAPIClient: CHLoginAPIClient
    private var httpScope = CoroutineScope(IO)

    fun setupAPi(provider: AWSCredentialsProvider) {
        val factory = ApiClientFactory().credentialsProvider(provider).region("ap-northeast-1")
        jpAPIClient = factory.build(CHLoginAPIClient::class.java)
    }

    private fun <T, R, A> T.makeApiCall(onResponse: CHResult<A>, requireSignIn: Boolean = true, block: T.() -> R) {
        if (requireSignIn && !AWSMobileClient.getInstance().isSignedIn) {
            onResponse.invoke(Result.failure(Throwable("isSignedIn???")))
            return
        }
        httpScope.launch(EmptyCoroutineContext, CoroutineStart.DEFAULT) {
            runCatching {
                block()
            }.onFailure {
                onResponse.invoke(Result.failure(it))
            }.onSuccess {}
        }
    }

    fun upLoadKeys(keys: List<CHUserKey>, onResponse: CHResult<Array<CHUserKey>>) {
        makeApiCall(onResponse) {
            val res = jpAPIClient.updateKeys(keys)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }

    fun putKey(key: CHUserKey, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            val res = jpAPIClient.putKey(key)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }

    fun getDevicesList(onResponse: CHResult<Array<CHUserKey>>) {
        makeApiCall(onResponse) {
            val res = jpAPIClient.getDevicesList()
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }

    fun removeKey(keyId: String, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            val res = jpAPIClient.removeKey(keyId)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }


    fun addFriend(friendID: String, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            val res = jpAPIClient.addFriend(friendID)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }

    fun uploadUserDeviceToken(onResponse: CHResult<Any>) {

        makeApiCall(onResponse) {
            val res = jpAPIClient.uploadDeviceToken(SharedPreferencesUtils.deviceToken!!)

            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
            SharedPreferencesUtils.isUploadDeveceToken = true
        }
    }

    fun getWebUrlByScene(scene: String, extInfo: Map<String, String>? = null, onResponse: CHResult<Any>,) {
        makeApiCall(onResponse, requireSignIn = false) {
            TokenManager.getValidToken { result ->
                result.fold(
                    onSuccess = {
                        val token = result.getOrNull()

                        val requestBody = ScenePayload(scene = scene, token = token, extInfo = extInfo)
                        val response = jpAPIClient.getWebUrlByScene(requestBody)

                        val urlString = Gson().toJsonTree(response).asJsonObject.get("url").asString
                        onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(data = urlString)))
                    },
                    onFailure = {
                        onResponse.invoke(Result.failure(result.exceptionOrNull() ?: Exception("Token refresh failed")))
                    }
                )
            }
        }
    }
}
