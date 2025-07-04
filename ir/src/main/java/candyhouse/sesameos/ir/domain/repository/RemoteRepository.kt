package candyhouse.sesameos.ir.domain.repository

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import candyhouse.sesameos.ir.base.IrCompanyCode
import candyhouse.sesameos.ir.base.IrRemote
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.uiBase.ConfigUpdateCallback
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.uiBase.UIConfigAdapter
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.handleBase.HandlerCallback
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.handleBase.HandlerConfigAdapter
import candyhouse.sesameos.ir.models.IrControlItem
import candyhouse.sesameos.ir.models.UIControlConfig
import candyhouse.sesameos.ir.server.CHResult
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.utils.L

/**
 * 红外远程控制器，使用请先初始化 initialize
 */
class RemoteRepository(val context: Context) : ViewModelProvider.Factory {
    private lateinit var uiConfigAdapter: UIConfigAdapter
    private lateinit var handlerConfigAdapter: HandlerConfigAdapter
    private var haveInitialized = false

    /**
     * 初始化远程控制器
     * @param type 远程控制器类型
     * @param uiItemCallback UI配置更新回调
     * @param handlerCallback 按键处理回调
     */
    fun initialize(type: Int, uiItemCallback: ConfigUpdateCallback, handlerCallback: HandlerCallback) {
        val factory = RemoteAdapterFactoryManager.getFactory(type)
        uiConfigAdapter = factory.createUIConfigAdapter(context)
        handlerConfigAdapter = factory.createHandlerConfigAdapter(context,uiConfigAdapter)
        uiConfigAdapter.setConfigUpdateCallback(uiItemCallback)
        handlerConfigAdapter.setHandlerCallback(handlerCallback)
        haveInitialized = true
        clearHandlerCache()
        clearConfigCache()
        L.d("RemoteRepository", "RemoteRepository initialized")
    }

    /**
     * 加载界面配置
     * @return UIControlConfig 配置。返回解释到的原始配置数据
     */
    suspend fun loadUIConfig(): UIControlConfig {
        if (!haveInitialized) {
            L.e("RemoteRepository", "RemoteRepository not initialized")
            throw Exception("RemoteRepository not initialized")
        }
        return uiConfigAdapter.loadConfig()
    }
    /**
     * 加载界面参数
     * @return  UI 界面列表，
     */
    suspend fun loadUIParams(): MutableList<IrControlItem> {
        if (!haveInitialized) {
            L.e("RemoteRepository", "RemoteRepository not initialized")
            throw Exception("RemoteRepository not initialized")
        }
        return uiConfigAdapter.loadUIParams()
    }

    /**
     * 处理按键点击事件
     * @param item 点击的按键
     * @param device hub3宿主
     * @param remoteDevice 遥控器设备
     */
    fun handleItemClick(item: IrControlItem, device: CHHub3, remoteDevice: IrRemote) {
        if (!haveInitialized) {
            L.e("RemoteRepository", "RemoteRepository not initialized")
            throw Exception("RemoteRepository not initialized")
        }
        val handleSuccess = uiConfigAdapter.handleItemClick(item,device,remoteDevice)
        if (handleSuccess) {
            handlerConfigAdapter.handleItemClick(item, device, remoteDevice)
        }
    }

    /**
     * 清除临时数据
     */
    fun clearConfigCache() {
        if (!haveInitialized) {
            L.e("RemoteRepository", "RemoteRepository not initialized")
            return
        }
        uiConfigAdapter.clearConfigCache()
    }
    /**
     * 清除临时数据
     */
    fun clearHandlerCache() {
        if (!haveInitialized) {
            L.e("RemoteRepository", "RemoteRepository not initialized")
            return
        }
        handlerConfigAdapter.clearHandlerCache()
    }

    fun getCurrentState(hub3: CHHub3, remoteDevice: IrRemote): String {
       return handlerConfigAdapter.getCurrentState(hub3,remoteDevice)
    }

    fun getCurrentIRDeviceType(): Int {
        return handlerConfigAdapter.getCurrentIRDeviceType()
    }

    fun modifyRemoteIrDeviceInfo(device: CHHub3,remoteDevice: IrRemote,onResponse: CHResult<Any>) {
        handlerConfigAdapter.modifyIRDeviceInfo(device,remoteDevice,onResponse)
    }

    fun matchRemoteDevice(irRemote: IrRemote) {
        uiConfigAdapter.matchRemoteDevice(irRemote)
    }

    fun setCurrentSate(state: String?) {
        uiConfigAdapter.setCurrentSate(state)
    }

    fun getUiConfigAdapter(): UIConfigAdapter {
        return uiConfigAdapter
    }

    /**
     * 获取品牌列表
     * @return 品牌列表
     */
    suspend fun getCompanyCodeList(context: Context): List<IrCompanyCode> {
        return uiConfigAdapter.getCompanyCodeList(context)
    }

    fun getMatchUiItemList() : List<IrControlItem> {
        return uiConfigAdapter.getMatchUiItemList()
    }

    fun getMatchItem(position: Int,items:List<IrControlItem>): IrControlItem? {
        return uiConfigAdapter.getMatchItem(position,items)
    }

    fun initMatchParams() {
        uiConfigAdapter.initMatchParams()
    }
}