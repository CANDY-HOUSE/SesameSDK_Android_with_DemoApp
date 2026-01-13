package co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale

import co.candyhouse.sesame.ble.SSM3ResponsePayload
import co.candyhouse.sesame.ble.os3.base.SesameOS3Payload
import co.candyhouse.sesame.utils.CHResult

/**
 * 基础能力实现抽象类
 * 提供所有能力实现的共有功能
 */
abstract class CHBaseCapabilityImpl : CHBaseCapability {
    // 支持对象，延迟初始化
    protected lateinit var support: CHCapabilitySupport

    /**
     * 初始化能力
     */
    override fun initialize(support: CHCapabilitySupport) {
        this.support = support
    }

    /**
     * 检查是否已初始化
     */
    override fun isInitialized(): Boolean {
        return ::support.isInitialized
    }

    /**
     * 检查蓝牙可用性
     * @return 如果蓝牙可用则返回 true，否则返回 false
     */
    protected fun <T> checkBleAvailability(result: CHResult<T>): Boolean {
        if (!isInitialized()) {
            result.invoke(Result.failure(IllegalStateException("Capability not initialized")))
            return false
        }
        return support.isBleAvailable(result)
    }

    /**
     * 安全发送命令
     * 自动处理初始化和蓝牙可用性检查
     */
    protected fun <T> sendCommandSafely(
        payload: SesameOS3Payload,
        result: CHResult<T>,
        onSuccess: (SSM3ResponsePayload) -> Unit
    ) {
        if (!checkBleAvailability(result)) return
        support.sendCommand(payload) { response -> onSuccess(response)
        }
    }
}