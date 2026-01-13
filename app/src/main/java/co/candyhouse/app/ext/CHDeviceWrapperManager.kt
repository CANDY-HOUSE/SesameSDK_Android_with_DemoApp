package co.candyhouse.app.ext

import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.server.dto.CHUserKey

/**
 * 统一管理设备和UserKey的关联
 *
 * @author frey on 2025/9/15
 */
object CHDeviceWrapperManager {
    private val deviceWrappers = mutableMapOf<String, CHDeviceWrapper>()
    private val pendingUserKeys = mutableMapOf<String, CHUserKey>()

    // 批量更新 UserKey
    fun updateUserKeys(userKeys: List<CHUserKey>) {
        userKeys.forEach { userKey ->
            val deviceId = userKey.deviceUUID.lowercase()

            val wrapper = deviceWrappers[deviceId]
            if (wrapper != null) {
                wrapper.userKey = userKey
            } else {
                pendingUserKeys[deviceId] = userKey
            }
        }
    }

    // 添加或更新设备
    fun updateDevice(device: CHDevices) {
        val deviceId = device.deviceId?.toString()?.lowercase() ?: return

        val existingWrapper = deviceWrappers[deviceId]
        if (existingWrapper != null) {
            deviceWrappers[deviceId] = existingWrapper.copy(device = device)
        } else {
            val pendingUserKey = pendingUserKeys.remove(deviceId)
            deviceWrappers[deviceId] = CHDeviceWrapper(device, pendingUserKey)
        }
    }

    // 获取包装对象
    fun getWrapper(deviceId: String): CHDeviceWrapper? {
        return deviceWrappers[deviceId.lowercase()]
    }

    // 获取所有包装对象
    fun getAllWrappers(): List<CHDeviceWrapper> {
        return deviceWrappers.values.toList()
    }

    // 清理
    fun clear() {
        deviceWrappers.clear()
        pendingUserKeys.clear()
    }
}

data class CHDeviceWrapper(
    val device: CHDevices,
    var userKey: CHUserKey? = null
)

val CHDevices.userKey: CHUserKey?
    get() = CHDeviceWrapperManager.getWrapper(this.deviceId?.toString() ?: "")?.userKey