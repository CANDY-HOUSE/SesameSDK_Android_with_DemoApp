package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.uiBase

import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrControlItem
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.UIControlConfig

interface UIConfigAdapter {

    /**
     * 加载界面配置
     * @return 设备界面配置：含ui信息、设置信息
     */
    suspend fun loadConfig(): UIControlConfig
    /**
     * 加载界面显示配置
     * @return 界面ui显示配置
     */
    suspend fun loadUIParams(): MutableList<IrControlItem>

    /**
     * 清除配置缓存
     */
    fun clearConfigCache()

    /**
     * 处理点击事件
     */
    fun handleItemClick(item: IrControlItem, hub3DeviceId: String, remoteDevice: IrRemote): Boolean
    /**
     * 设置配置更新回调
     */
    fun setConfigUpdateCallback(uiItemCallback: ConfigUpdateCallback)

    /**
     * 设置当前状态
     */
    fun setCurrentSate(state: String?) {}
}