package co.utils

import co.candyhouse.app.R
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.utils.SharedPreferencesUtils
import co.candyhouse.sesame.utils.uuidToBytes
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback

object UserUtils {

    fun getUserId(): String? {
        SharedPreferencesUtils.userId?.let { return it } ?: run { return null }
    }

    fun getUserIdWithByte(): ByteArray? {
        return getUserId()?.let {
            val historyTagByteArray = it.uuidToBytes()
            historyTagByteArray
        } ?: run {
            // 协议要求，UUID为全ff
            "ffffffffffffffffffffffffffffffff".uuidToBytes()
        }
    }

    fun loadUserUserId() {
        runCatching {
            AWSMobileClient.getInstance().getUserAttributes(object : Callback<Map<String, String>> {
                override fun onResult(userAttributes: Map<String, String>) {
                    val sub = userAttributes["sub"]
//                    val email = userAttributes["email"]
                    SharedPreferencesUtils.userId = sub
                }

                override fun onError(e: Exception?) {

                }
            })

        }
    }

}

fun getHistoryTag(): ByteArray {
    return SharedPreferencesUtils.nickname?.toByteArray() ?: CHDeviceManager.app.getString(R.string.unLoginHistoryTag).toByteArray()
}