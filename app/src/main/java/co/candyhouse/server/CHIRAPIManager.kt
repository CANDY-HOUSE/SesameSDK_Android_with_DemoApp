package co.candyhouse.server

import android.content.Context
import co.candyhouse.server.dto.IrDeviceDeleteRequest
import co.candyhouse.sesame.open.CHConfiguration
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.getClientRegion
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.apigateway.ApiClientFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.coroutines.EmptyCoroutineContext


object CHIRAPIManager {
    private lateinit var jpAPIClient: CHIRAPIClient
    private var httpScope = CoroutineScope(IO)
    private var isInitialized = false

    fun initialize(context: Context) {
        setupAPI(context)
    }

    @Synchronized
    private fun setupAPI(appContext: Context) {
        if (isInitialized) {
            return
        }
        httpScope.launch(IO) {
            val credentialsProvider = CognitoCachingCredentialsProvider(
                appContext, CHConfiguration.CLIENT_ID,  // 身份集區 ID
                CHConfiguration.CLIENT_ID.getClientRegion() // 區域
            )
            val factory = ApiClientFactory().credentialsProvider(credentialsProvider).apiKey(
                CHConfiguration.API_KEY
            ).region("ap-northeast-1")
                .clientConfiguration(ClientConfiguration().apply {
                    userAgent = userAgent?.replace(
                        Regex("\\b[a-z]{2}_[A-Z]{2}\\b"),
                        Locale.getDefault().toString()
                    ) ?: userAgent
                })
            jpAPIClient = factory.build(CHIRAPIClient::class.java)
            isInitialized = true
        }
    }


    private fun <T, R, A> T.makeApiCall(onResponse: CHResult<A>, block: T.() -> R) {
        httpScope.launch(EmptyCoroutineContext, CoroutineStart.DEFAULT) {
            runCatching {
                try {
                    block()
                } catch (e: Exception) {
                    e.printStackTrace()
                    onResponse.invoke(Result.failure(e))
                }

            }.onFailure {
                onResponse.invoke(Result.failure(it))
            }.onSuccess {}
        }
    }

    /**
     *获取IR设备列表
     */
    fun fetchIRDevices(deviceUUID: String, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            try {
                val res = jpAPIClient.fetchIRDevices(deviceUUID)
                onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
            } catch (e: Exception) {
                L.e("sf", "fetchIRDevices Error ", e)
                onResponse.invoke(Result.failure(e))
            }
        }
    }

    /**
     *删除IR遥控器
     */
    fun deleteIRDevice(deviceUUID: String, subID: String, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            try {
                val requestBody = IrDeviceDeleteRequest(subID)
                val res = jpAPIClient.deleteIRDevice(deviceUUID, requestBody)
                onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
            } catch (e: Exception) {
                L.e("sf", "deleteIRDevice Error ", e)
                onResponse.invoke(Result.failure(e))
            }
        }
    }

}

