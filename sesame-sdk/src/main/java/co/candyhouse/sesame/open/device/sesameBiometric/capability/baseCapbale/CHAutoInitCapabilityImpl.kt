package co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale

import co.candyhouse.sesame.ble.SSM3ResponsePayload
import co.candyhouse.sesame.ble.os3.base.SesameOS3Payload
import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.utils.L

/**
 * 自动初始化的能力基类
 */
abstract class CHAutoInitCapabilityImpl {
    // 支持对象
    protected lateinit var support: CHCapabilitySupport
    private val tag = this.javaClass.simpleName

    protected fun setupSupport(support: CHCapabilitySupport) {
        this.support = support
    }

    /**
     * 检查是否已初始化
     */
    protected fun isInitialized(): Boolean {
        return ::support.isInitialized
    }

    /**
     * 检查蓝牙可用性
     */
    protected fun <T> checkBleAvailability(result: CHResult<T>): Boolean {
        if (!isInitialized()) {
            result.invoke(Result.failure(IllegalStateException("Capability not initialized")))
            L.d(tag, "Error:! checkBleAvailability: Capability not initialized")
            return false
        }
        return support.isBleAvailable(result)
    }

    /**
     * 安全发送命令
     */
    protected fun <T> sendCommandSafely(
        payload: SesameOS3Payload,
        result: CHResult<T>,
        onSuccess: (SSM3ResponsePayload) -> Unit
    ) {
        if (!checkBleAvailability(result)) return
        support.sendCommand(payload) { response ->
            onSuccess(response)
        }
    }
}