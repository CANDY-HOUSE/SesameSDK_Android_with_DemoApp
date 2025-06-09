package co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale

import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBase

/**
 * 事件处理能力实现抽象类
 * 提供事件代理管理的通用实现
 */
abstract class CHEventCapabilityImpl<T, H : CHEventHandler> :
    CHBaseCapabilityImpl(),
    CHEventCapability<T> {

    // 事件处理器映射
    private val eventHandlers = mutableMapOf<T, H>()

    /**
     * 创建事件处理器
     */
    protected abstract fun createEventHandler(delegate: T): H

    /**
     * 注册事件代理
     */
    override fun registerEventDelegate(delegate: T) {
        if (!isInitialized() || support !is CHSesameBiometricBase) return

        val device = support as CHSesameBiometricBase
        val handler = createEventHandler(delegate)
        eventHandlers[delegate] = handler
        device.addEventHandler(handler)
    }

    /**
     * 取消注册事件代理
     */
    override fun unregisterEventDelegate(delegate: T) {
        if (!isInitialized() || support !is CHSesameBiometricBase) return

        val device = support as CHSesameBiometricBase
        eventHandlers[delegate]?.let { handler ->
            device.removeEventHandler(handler)
            eventHandlers.remove(delegate)
        }
    }
}