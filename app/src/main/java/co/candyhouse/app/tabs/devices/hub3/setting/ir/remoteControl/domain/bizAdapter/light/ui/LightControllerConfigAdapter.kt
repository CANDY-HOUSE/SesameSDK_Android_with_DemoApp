package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.light.ui


import android.content.Context
import co.candyhouse.app.R
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.UIResourceExtension
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrControlItem
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.ItemType
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.UIControlConfig
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.uiBase.ConfigUpdateCallback
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.uiBase.UIConfigAdapter
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.utils.L
import com.google.gson.Gson


class LightControllerConfigAdapter(val context: Context) : UIConfigAdapter {
    private val tag = javaClass.simpleName
    private val gson = Gson()
    private var config: UIControlConfig? = null
    private var updateCallback: ConfigUpdateCallback? = null
    private var isPowerOn: Boolean = false

    override suspend fun loadConfig(): UIControlConfig {
        if (config == null) {
            val jsonString = context.assets.open("config/light_control_config.json")
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

    override fun handleItemClick(item: IrControlItem, device: CHHub3, remoteDevice: IrRemote): Boolean {
        when (item.type) {
            ItemType.POWER_STATUS_ON -> {
                isPowerOn = true
                updatePowerStatus()
            }
            ItemType.POWER_STATUS_OFF -> {
                isPowerOn = false
                updatePowerStatus()
            }
            // 其他按键直接发送对应的操作码
            else -> {
                if (!isPowerOn) {
//                    Toast.makeText(context, R.string.tv_power_off_tip, Toast.LENGTH_SHORT).show()
                    return false
                }
                return true
            }
        }
        return true
    }

    private fun updatePowerStatus() {
        config?.let { config ->
            // 更新开关机状态
            updatePowerItem(config, ItemType.POWER_STATUS_ON)
            updatePowerItem(config, ItemType.POWER_STATUS_OFF)
        }
    }

    private fun updatePowerItem(config: UIControlConfig, type: ItemType) {
        val controlConfig = config.controls.find { it.type == type.name } ?: return
        val item = IrControlItem(
            id = controlConfig.id,
            type = type,
            title = UIResourceExtension.getStringByIndex(context, controlConfig, 0),
            value = "",
            isSelected = when (type) {
                ItemType.POWER_STATUS_ON -> isPowerOn
                ItemType.POWER_STATUS_OFF -> !isPowerOn
                else -> false
            },
            iconRes = UIResourceExtension.getResource(context, controlConfig)
        )
        updateCallback?.onItemUpdate(item)
    }

    private fun createControlItems(config: UIControlConfig): List<IrControlItem> {
        return config.controls.map { controlConfig ->
            val type = ItemType.valueOf(controlConfig.type)
            IrControlItem(
                id = controlConfig.id,
                type = type,
                title = UIResourceExtension.getStringByIndex(context, controlConfig, 0),
                value = "",
                isSelected = when (type) {
                    ItemType.POWER_STATUS_ON -> isPowerOn
                    ItemType.POWER_STATUS_OFF -> !isPowerOn
                    else -> false
                },
                iconRes = UIResourceExtension.getResource(context, controlConfig)
            )
        }
    }

    /**
     * 获取当前State
     */
    fun getCurrentState():String {
        return ""
    }
}