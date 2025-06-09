package co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale

/**
 * 事件处理能力接口
 * 提供事件代理管理功能
 */
interface CHEventCapability<T> {
    /**
     * 注册事件代理
     */
    fun registerEventDelegate(delegate: T)

    /**
     * 取消注册事件代理
     */
    fun unregisterEventDelegate(delegate: T)
}