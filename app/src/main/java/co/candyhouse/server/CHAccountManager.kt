package co.candyhouse.server

import co.candyhouse.sesame.utils.L
import co.utils.SharedPreferencesUtils
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobileconnectors.apigateway.ApiClientFactory
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


        private fun <T, R, A> T.makeApiCall(onResponse: CHResult<A>, block: T.() -> R) {

            if (!AWSMobileClient.getInstance().isSignedIn) {
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

    fun putKeyInfor(key: CHDeviceInfor, onResponse: CHResult<Any>) {
        if (key.latitude == "null" || key.longitude == "null") {
//            L.d("hcia", "空不傳:" + key)
            return
        }
        makeApiCall(onResponse) {
//            L.d("hcia", "key:" + key)
            val res = jpAPIClient.putKeyInfor(key)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }

    fun putKey(key: CHUserKey, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            val res = jpAPIClient.putKey(key)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }


    fun getKeys(onResponse: CHResult<Array<CHUserKey>>) {
        makeApiCall(onResponse) {
            val res = jpAPIClient.getKeys()
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

    fun removeFriend(friendID: String, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            val res = jpAPIClient.removeFriend(friendID)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }

    fun getFriends(tag:String? = null,onResponse: CHResult<Array<CHUser>>) {
        makeApiCall(onResponse) {
            val res = jpAPIClient.getFriends(10,tag)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }

    fun getDeviceMember(deviceID: String, timeSign: String, onResponse: CHResult<Array<CHUser>>) {
        makeApiCall(onResponse) {
            val res = jpAPIClient.getFriendsWithDevice(deviceID, timeSign)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }

    fun devicesWithFriend(friendID: String, onResponse: CHResult<Array<CHUserKey>>) {
        makeApiCall(onResponse) {
            val res = jpAPIClient.deviceWithFriend(friendID)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }


    fun giveFriendDevice(key: Any, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            val res = jpAPIClient.addFriendDevice(key)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }

    fun removeFriendDevice(key: Any, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            val res = jpAPIClient.removeFriendDevice(key)
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

}
