package co.candyhouse.server

import android.content.Context
import co.candyhouse.app.ext.TokenManager
import co.candyhouse.app.tabs.account.CHUserKey
import co.candyhouse.sesame.utils.ApiClientConfigBuilder
import co.utils.AppIdentifyIdUtil
import co.utils.SharedPreferencesUtils
import com.amazonaws.auth.AWSCredentialsProvider
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext

typealias CHResult<T> = (Result<CHResultState<T>>) -> Unit

sealed class CHResultState<T>(val data: T) {
    open class CHResultStateNetworks<T>(data: T) : CHResultState<T>(data)
    open class CHResultStateBle<T>(data: T) : CHResultState<T>(data)
}

object CHLoginAPIManager {
    private lateinit var jpAPIClient: CHLoginAPIClient
    private lateinit var appContext: Context
    private val httpScope = CoroutineScope(IO + SupervisorJob())

    fun setupAPi(context: Context, provider: AWSCredentialsProvider) {
        appContext = context.applicationContext
        val factory = ApiClientConfigBuilder.buildApiClientFactory(
            credentialsProvider = provider,
            region = "ap-northeast-1"
        )

        jpAPIClient = factory.build(CHLoginAPIClient::class.java)
    }

    private fun <T, R, A> T.makeApiCall(onResponse: CHResult<A>, block: T.() -> R) {
        httpScope.launch(EmptyCoroutineContext, CoroutineStart.DEFAULT) {
            runCatching { block() }
                .onFailure { onResponse.invoke(Result.failure(it)) }
        }
    }

    fun upLoadKeys(keys: List<CHUserKey>, onResponse: CHResult<Array<CHUserKey>>) {
        makeApiCall(onResponse) {
            val res = jpAPIClient.updateKeys(AppIdentifyIdUtil.get(appContext), keys)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }

    fun putKey(key: CHUserKey, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            val res = jpAPIClient.putKey(AppIdentifyIdUtil.get(appContext), key)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }

    fun getDevicesList(onResponse: CHResult<Array<CHUserKey>>) {
        makeApiCall(onResponse) {
            val res = jpAPIClient.getDevicesList(AppIdentifyIdUtil.get(appContext))
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }

    fun removeKey(keyId: String, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            val res = jpAPIClient.removeKey(AppIdentifyIdUtil.get(appContext), keyId)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }

    fun addFriend(friendID: String, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            val res = jpAPIClient.addFriend(AppIdentifyIdUtil.get(appContext), friendID)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }

    fun uploadUserDeviceToken(onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            val res = jpAPIClient.uploadDeviceToken(AppIdentifyIdUtil.get(appContext), SharedPreferencesUtils.deviceToken!!)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }

    fun getWebUrlByScene(scene: String, extInfo: Map<String, String>? = null, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            TokenManager.getValidToken { result ->
                result.fold(
                    onSuccess = {
                        val token = result.getOrNull()
                        val requestBody = ScenePayload(scene = scene, token = token, extInfo = extInfo)
                        val response = jpAPIClient.getWebUrlByScene(AppIdentifyIdUtil.get(appContext), requestBody)
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
