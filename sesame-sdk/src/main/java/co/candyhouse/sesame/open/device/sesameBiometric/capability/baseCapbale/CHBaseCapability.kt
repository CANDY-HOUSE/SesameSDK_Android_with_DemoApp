package co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale

/**
 * 基础能力接口
 * 定义所有能力共有的方法
 */
interface CHBaseCapability {
    /**
     * 初始化能力
     */
    fun initialize(support: CHCapabilitySupport)

    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean
}