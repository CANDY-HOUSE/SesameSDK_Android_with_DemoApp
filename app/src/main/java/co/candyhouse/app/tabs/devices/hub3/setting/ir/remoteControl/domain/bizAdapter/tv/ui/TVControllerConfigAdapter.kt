package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.tv.ui

import android.content.Context
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.UIResourceExtension
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrControlItem
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.ItemType
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.UIControlConfig
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.handleBase.HXDCommandProcessor
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.handleBase.HXDParametersSwapper
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.uiBase.ConfigUpdateCallback
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.uiBase.UIConfigAdapter
import co.candyhouse.sesame.utils.L
import com.google.gson.Gson


class TVControllerConfigAdapter(val context: Context) : UIConfigAdapter {
    private val tag = javaClass.simpleName
    private val gson = Gson()
    private var config: UIControlConfig? = null
    private var updateCallback: ConfigUpdateCallback? = null
    val commandProcess = HXDCommandProcessor()
    val paramsSwapper = HXDParametersSwapper()

    override suspend fun loadConfig(): UIControlConfig {
        if (config == null) {
            val jsonString = context.assets.open("config/tv_control_config.json")
                .bufferedReader()
                .use { it.readText() }
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
        // 不做显示上的处理
        return true
    }

    private fun createControlItems(config: UIControlConfig): List<IrControlItem> {
        return config.controls.map { controlConfig ->
            val type = ItemType.valueOf(controlConfig.type)
            IrControlItem(
                id = controlConfig.id,
                type = type,
                title = UIResourceExtension.getStringByIndex(context, controlConfig, 0),
                value = "",
                isSelected = true,
                iconRes = UIResourceExtension.getResource(context, controlConfig)
            )
        }
    }

    /**
     * 获取当前State
     */
    fun getCurrentState(): String {
        return ""
    }
}

