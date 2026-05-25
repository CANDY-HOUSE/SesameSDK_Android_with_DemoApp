package co.candyhouse.sesame.server

import co.candyhouse.sesame.open.devices.base.CHDevices

/**
 * 公共访问器类
 *
 * @author frey on 2025/4/17
 */
object CHIotManagerPublic {
    /**
     * 启动IoT连接
     */
    fun startConnection() {
        CHIotManager.startConnection()
    }

    /**
     * 冷启动后，当列表完成刷新后补偿一次IoT订阅（解决kill APP，再登录拉取数据的情况）
     */
    fun subscribeDevicesIfConnected(updatedDevices: List<CHDevices>) {
        CHIotManager.subscribeDevicesIfConnected(updatedDevices)
    }

    /**
     * 退出登录后，清除IoT订阅缓存
     */
    fun clearIotSubscriptionCache() {
        CHIotManager.clearIotSubscriptionCache()
    }
}