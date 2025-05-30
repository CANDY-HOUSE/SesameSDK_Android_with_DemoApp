package candyhouse.sesameos.ir.domain.bizAdapter.bizBase.uiBase

import android.content.Context
import candyhouse.sesameos.ir.base.IrCompanyCode
import candyhouse.sesameos.ir.base.IrRemote
import candyhouse.sesameos.ir.models.IrControlItem
import candyhouse.sesameos.ir.models.UIControlConfig
import co.candyhouse.sesame.open.device.CHHub3

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
    fun handleItemClick(item: IrControlItem, device: CHHub3, remoteDevice: IrRemote): Boolean
    /**
     * 设置配置更新回调
     */
    fun setConfigUpdateCallback(uiItemCallback: ConfigUpdateCallback)
    /**
     * 匹配远程设备参数
     */
    fun matchRemoteDevice(irRemote: IrRemote)

    /**
     * 设置当前状态
     */
    fun setCurrentSate(state: String?) {}

    /**
     * 获取空调品牌列表
     * @return 空调品牌列表
     */
    suspend fun getCompanyCodeList(context: Context): List<IrCompanyCode>


    fun getMatchUiItemList(): List<IrControlItem>

    fun getMatchItem(position: Int,items:List<IrControlItem>): IrControlItem?
    fun initMatchParams()
}