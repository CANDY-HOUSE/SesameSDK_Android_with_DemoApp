package co.candyhouse.app.tabs.devices.ssm2.setting

import no.nordicsemi.android.dfu.DfuServiceInitiator

/**
 * 单例提供 DFU 启动器。注意：DfuServiceInitiator 构造时会绑定目标设备地址，
 * 若地址变化需要重新创建。这里仅在同一个地址重复使用时复用实例，避免重复配置。
 * 不建议在并发多个不同设备 DFU 时使用同一个 initiator；若需要并发可改为 Map<address, initiator>。
 */
object DfuStarterProvider {
    @Volatile
    private var cachedAddress: String? = null

    @Volatile
    private var cachedInitiator: DfuServiceInitiator? = null

    /**
     * 获取指定 address 的 DfuServiceInitiator。如果 address 与缓存不同则重新创建并配置。
     */
    @Synchronized
    fun get(address: String): DfuServiceInitiator? {
        if (cachedInitiator != null) {
            return null
        }
        cachedAddress = address
        cachedInitiator = DfuServiceInitiator(address).apply {
            // 通用配置集中放这里，业务层只需 setZip 与 start。
            setPacketsReceiptNotificationsEnabled(true)
            setPrepareDataObjectDelay(400)
            // 使用不安全的实验性无按钮安全 DFU（原代码如此）。
            setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true)
            setDisableNotification(false)
            setForeground(false)
        }
        return cachedInitiator!!
    }

    /**
     * 如果需要明确丢弃当前缓存（例如一次 DFU 完成后确保下次重新创建），可以调用此方法。
     */
    @Synchronized
    fun clear() {
        cachedInitiator = null
        cachedAddress = null
    }
}
