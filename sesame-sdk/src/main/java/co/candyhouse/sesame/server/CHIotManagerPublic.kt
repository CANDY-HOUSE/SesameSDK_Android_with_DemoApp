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
     * 账号退出时停止连接池并清除全部Topic
     */
    fun stopConnectionPool() {
        CHIotManager.stopConnectionPool()
    }

    /**
     * Session过期时使用当前Amplify凭证重建连接池
     */
    fun restartConnectionPool() {
        CHIotManager.restartConnectionPool()
    }

    /**
     * 回到前台时，如果IoT正在等待重试则立即重连
     */
    fun reconnectImmediatelyIfWaiting() {
        CHIotManager.reconnectImmediatelyIfWaiting()
    }

    /**
     * 冷启动后，当列表完成刷新后补偿一次IoT订阅（解决kill APP，再登录拉取数据的情况）
     */
    fun subscribeDevicesIfConnected(updatedDevices: List<CHDevices>) {
        CHIotManager.subscribeDevicesIfConnected(updatedDevices)
    }

    /**
     * 删除设备后解除该设备关联的全部Topic
     */
    fun unsubscribeDevice(deviceId: String) {
        CHIotManager.unsubscribeDevice(deviceId)
    }

    /**
     * 设置 IoT 重连成功回调（用于重连后刷新服务端设备列表）
     */
    fun setOnReconnected(callback: (() -> Unit)?) {
        CHIotManager.onReconnected = callback
    }
}
