package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.repository

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrControlItem
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.UIControlConfig
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.handleBase.HandlerCallback
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.handleBase.RemoteHandlerAdapter
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.uiBase.ConfigUpdateCallback
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.uiBase.RemoteUIAdapter
import co.candyhouse.server.CHResult
import co.candyhouse.sesame.utils.L

/**
 * 红外远程控制器，使用请先初始化 initialize
 */
class RemoteRepository(val context: Context) : ViewModelProvider.Factory {
    private lateinit var uiAdapter: RemoteUIAdapter
    private lateinit var handlerAdapter: RemoteHandlerAdapter
    private var haveInitialized = false

    /**
     * 初始化远程控制器
     * @param type 远程控制器类型
     * @param uiItemCallback UI配置更新回调
     * @param handlerCallback 按键处理回调
     */
    fun initialize(type: Int, uiItemCallback: ConfigUpdateCallback, handlerCallback: HandlerCallback) {
        val factory = RemoteAdapterFactoryManager.getFactory(type)
        val uiType = RemoteAdapterFactoryManager.getUIType(type)
        uiAdapter = factory.createUIAdapter(context,uiType)
        handlerAdapter = factory.createHandlerAdapter(context, uiAdapter)
        uiAdapter.setConfigUpdateCallback(uiItemCallback)
        handlerAdapter.setHandlerCallback(handlerCallback)
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
        return uiAdapter.loadConfig()
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
        return uiAdapter.loadUIParams()
    }

    /**
     * 处理按键点击事件
     * @param item 点击的按键
     * @param hub3DeviceId hub3 设备ID
     * @param remoteDevice 遥控器设备
     */
    fun handleItemClick(item: IrControlItem, hub3DeviceId: String, remoteDevice: IrRemote) {
        if (!haveInitialized) {
            L.e("RemoteRepository", "RemoteRepository not initialized")
            throw Exception("RemoteRepository not initialized")
        }
        val handleSuccess = uiAdapter.handleItemClick(item, hub3DeviceId, remoteDevice)
        if (handleSuccess) {
            handlerAdapter.handleItemClick(item, hub3DeviceId, remoteDevice)
        }
    }

    fun addIrDeviceToMatter(irRemote: IrRemote?, hub3DeviceId: String) {
        if (!haveInitialized) {
            L.e("RemoteRepository", "RemoteRepository not initialized")
            throw Exception("RemoteRepository not initialized")
        }
        handlerAdapter.addIrDeviceToMatter(irRemote, hub3DeviceId)
    }

    /**
     * 清除临时数据
     */
    fun clearConfigCache() {
        if (!haveInitialized) {
            L.e("RemoteRepository", "RemoteRepository not initialized")
            return
        }
        uiAdapter.clearConfigCache()
    }

    /**
     * 清除临时数据
     */
    fun clearHandlerCache() {
        if (!haveInitialized) {
            L.e("RemoteRepository", "RemoteRepository not initialized")
            return
        }
        handlerAdapter.clearHandlerCache()
    }

    fun getCurrentState(hub3DeviceId: String, remoteDevice: IrRemote): String {
        return handlerAdapter.getCurrentState(hub3DeviceId, remoteDevice)
    }

    fun getCurrentIRType(): Int {
        return handlerAdapter.getCurrentIRType()
    }

    fun modifyRemoteIrDeviceInfo(device: String, remoteDevice: IrRemote, onResponse: CHResult<Any>) {
        handlerAdapter.modifyIRDeviceInfo(device, remoteDevice, onResponse)
    }

    fun setCurrentSate(state: String?) {
        uiAdapter.setCurrentSate(state)
    }
}