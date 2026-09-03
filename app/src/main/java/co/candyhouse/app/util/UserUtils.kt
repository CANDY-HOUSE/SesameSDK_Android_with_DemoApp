package co.candyhouse.app.util

import co.candyhouse.app.R
import co.candyhouse.app.ext.aws.AWSStatus
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.utils.SharedPreferencesUtils
import co.candyhouse.sesame.utils.uuidToBytes

object UserUtils {

    fun getUserId(): String? {
        SharedPreferencesUtils.userId?.let { return it } ?: run { return null }
    }

    suspend fun getSubId(): String? {
        getUserId()?.takeIf { it.isNotEmpty() }?.let { return it }
        loadUserUserId()
        return getUserId()?.takeIf { it.isNotEmpty() }
    }

    fun getEnvironmentIdWithByte(): ByteArray? {
        // history tag 只有两档（对齐 iOS）：订阅返回的 envId → 未取到则全ff
        SharedPreferencesUtils.environmentId?.takeIf { it.isNotEmpty() }?.let {
            return it.uuidToBytes()
        }
        // 协议要求，UUID为全ff
        return "ffffffffffffffffffffffffffffffff".uuidToBytes()
    }

    suspend fun loadUserUserId() {
        runCatching {
            val userAttributes = AWSStatus.getUserAttributes()
            val sub = userAttributes["sub"]
            SharedPreferencesUtils.userId = sub
        }
    }

}

fun getHistoryTag(): ByteArray {
    return SharedPreferencesUtils.name?.toByteArray() ?: CHDeviceManager.app.getString(R.string.unLoginHistoryTag).toByteArray()
}
