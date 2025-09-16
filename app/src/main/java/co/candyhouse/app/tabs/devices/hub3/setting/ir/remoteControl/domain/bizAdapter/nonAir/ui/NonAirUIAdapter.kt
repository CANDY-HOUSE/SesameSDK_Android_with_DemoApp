package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.nonAir.ui

import android.content.Context
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.UIResourceExtension
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrControlItem
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.ItemType
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.UIControlConfig
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.handleBase.HXDCommandProcessor
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.handleBase.HXDParametersSwapper
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.uiBase.ConfigUpdateCallback
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.uiBase.RemoteUIAdapter
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.uiBase.RemoteUIType
import com.google.gson.Gson

class NonAirUIAdapter(private val context: Context, private val uiType: RemoteUIType) : RemoteUIAdapter {

    private val tag = javaClass.simpleName
    private val gson = Gson()
    private var config: UIControlConfig? = null
    private var updateCallback: ConfigUpdateCallback? = null
    private var isPowerOn: Boolean = false

    val commandProcess = HXDCommandProcessor()
    val paramsSwapper = HXDParametersSwapper()

    override suspend fun loadConfig(): UIControlConfig {
        if (config == null) {
            val jsonString = context.assets.open(uiType.configPath).bufferedReader().use { it.readText() }
            config = gson.fromJson(jsonString, UIControlConfig::class.java)
        }
        if (null == config) {
            throw Exception("config is null")
        }
        return config!!
    }

    override suspend fun loadUIParams(): MutableList<IrControlItem> {
        return createControlItems(config!!).toMutableList()
    }

    override fun clearConfigCache() {
        config = null
    }

    override fun setConfigUpdateCallback(uiItemCallback: ConfigUpdateCallback) {
        updateCallback = uiItemCallback
    }

    override fun handleItemClick(item: IrControlItem, hub3DeviceId: String, remoteDevice: IrRemote): Boolean {
        // 无需做处理
        return true
    }

    private fun createControlItems(config: UIControlConfig): List<IrControlItem> {
        return config.controls.map { controlConfig ->
            val type = ItemType.valueOf(controlConfig.type)
            IrControlItem(
                id = controlConfig.id, type = type, title = UIResourceExtension.getStringByIndex(context, controlConfig, 0),
                value = "", isSelected = true, iconRes = UIResourceExtension.getResource(context, controlConfig)
            )
        }
    }

    fun getCurrentState(): String {
        return ""
    }

    fun getDeviceType(): RemoteUIType = uiType
}





