package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.air.ui


import android.content.Context
import android.widget.Toast
import co.candyhouse.app.R
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
import co.candyhouse.sesame.utils.L
import com.google.gson.Gson

class AirUIAdapter(val context: Context, val uiType: RemoteUIType) : RemoteUIAdapter {
    private val tag = javaClass.simpleName
    private val gson = Gson()
    private var config: UIControlConfig? = null
    private var updateCallback: ConfigUpdateCallback? = null
    private var currentState: String = ""

    val commandProcessor = HXDCommandProcessor()
    val parametersSwapper = HXDParametersSwapper()

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

    override fun setCurrentSate(state: String?) {
        if (state.isNullOrEmpty()) {
            return
        }
        currentState = state
        updateUIConfig()
    }

    private fun updateUIConfig() {
        val parseSuccess = commandProcessor.parseAirData(currentState)
        if (!parseSuccess) {
            L.e(tag, "updateUIConfig: failed to parse state")
            return
        }
        config?.let {
            val items = createControlItems(it)
            L.d(tag, "updateUIConfig: items.size ${items.size}")
            items.forEach { item ->
                updateCallback?.onItemUpdate(item)
            }
        }
    }

    override fun handleItemClick(item: IrControlItem, hub3DeviceId: String, remoteDevice: IrRemote): Boolean {
        when (item.type) {
            ItemType.POWER_STATUS_ON -> {
                commandProcessor.setPower(getPowerValue(true))
                togglePowerOn(item)
                togglePowerOff()
            }

            ItemType.POWER_STATUS_OFF -> {
                commandProcessor.setPower(getPowerValue(false))
                togglePowerOn()
                togglePowerOff(item)
            }

            ItemType.TEMP_CONTROL_ADD -> return adjustTemperatureAdd()
            ItemType.TEMP_CONTROL_REDUCE -> return adjustTemperatureReduce()
            ItemType.MODE -> cycleMode(item)
            ItemType.FAN_SPEED -> cycleFanSpeed(item)
            ItemType.WIND_DIRECTION -> cycleVerticalSwing(item)
            ItemType.AUTO_WIND_DIRECTION -> cycleSwingSwitch(item)
            else -> {
                L.e(tag, "Unknown item type")
            }
        }
        return true
    }

    private fun getCurrentIndexForType(type: ItemType): Int {
        return when (type) {
            ItemType.MODE -> getModeIndex()
            ItemType.FAN_SPEED -> getFanSpeedIndex()
            ItemType.WIND_DIRECTION -> getWindDirection()
            ItemType.AUTO_WIND_DIRECTION -> getAutomaticWindDirection()
            ItemType.POWER_STATUS_ON, ItemType.POWER_STATUS_OFF -> if (getPowerIndex()) 0 else 1
            else -> 0
        }
    }

    private fun getValueForType(type: ItemType): String {
        return when (type) {
            ItemType.TEMPERATURE_VALUE -> "${parametersSwapper.getTemperature(commandProcessor.getTemperature())}°C"
            else -> ""
        }
    }

    private fun isItemSelected(type: ItemType): Boolean {
        return when (type) {
            ItemType.POWER_STATUS_ON -> getPowerIndex()
            ItemType.POWER_STATUS_OFF -> !getPowerIndex()
            else -> false
        }
    }

    private fun checkConfig(): Boolean {
        return config != null && config!!.controls.isNotEmpty()
    }

    private fun togglePowerOn() {
        val resId = if (getPowerIndex()) 0 else 1
        if (!checkConfig()) {
            return
        }
        val controlConfig = config!!.controls.find { it.type == "POWER_STATUS_ON" } ?: return
        val newItem = IrControlItem(
            id = controlConfig.id,
            type = ItemType.POWER_STATUS_ON,
            title = UIResourceExtension.getStringByIndex(context, controlConfig, resId),
            value = controlConfig.defaultValue,
            isSelected = isItemSelected(ItemType.POWER_STATUS_ON),
            iconRes = UIResourceExtension.getResourceByIndex(context, controlConfig, resId)
        )
        updateCallback?.onItemUpdate(newItem)
    }


    private fun togglePowerOn(item: IrControlItem) {
        val resId = if (getPowerIndex()) 0 else 1
        if (!checkConfig()) {
            return
        }
        val controlConfig = config!!.controls.find { it.type == "POWER_STATUS_ON" } ?: return
        val newItem = IrControlItem(
            id = item.id,
            type = item.type,
            title = item.title,
            value = item.value,
            isSelected = isItemSelected(ItemType.POWER_STATUS_ON),
            iconRes = UIResourceExtension.getResourceByIndex(context, controlConfig, resId)
        )
        updateCallback?.onItemUpdate(newItem)
    }

    private fun togglePowerOff() {
        val resId = if (getPowerIndex()) 0 else 1
        if (!checkConfig()) {
            return
        }
        val controlConfig = config!!.controls.find { it.type == "POWER_STATUS_OFF" } ?: return
        val newItem = IrControlItem(
            id = controlConfig.id,
            type = ItemType.POWER_STATUS_OFF,
            title = UIResourceExtension.getStringByIndex(context, controlConfig, resId),
            value = controlConfig.defaultValue,
            isSelected = isItemSelected(ItemType.POWER_STATUS_OFF),
            iconRes = UIResourceExtension.getResourceByIndex(context, controlConfig, resId)
        )
        updateCallback?.onItemUpdate(newItem)
    }

    private fun togglePowerOff(item: IrControlItem) {
        var resId = if (getPowerIndex()) 0 else 1
        if (!checkConfig()) {
            return
        }
        val controlConfig = config!!.controls.find { it.type == "POWER_STATUS_OFF" } ?: return
        val newItem = IrControlItem(
            id = item.id,
            type = item.type,
            title = item.title,
            value = item.value,
            isSelected = isItemSelected(ItemType.POWER_STATUS_ON),
            iconRes = UIResourceExtension.getResourceByIndex(context, controlConfig, resId)
        )
        updateCallback?.onItemUpdate(newItem)
    }

    private fun adjustTemperatureAdd(): Boolean {
        if (!checkConfig()) {
            return false
        }
        if (!canAdjustTemperature()) {
            Toast.makeText(context, R.string.air_conditioner_temperature_refuse_change, Toast.LENGTH_SHORT).show()
            return false
        }
        val settings = config!!.settings.temperature
        setTemperature(minOf(settings.max, getTemperature() + settings.step))
        val controlConfig = config!!.controls.find { it.type == "TEMPERATURE_VALUE" } ?: return false
        val newItem = IrControlItem(
            id = controlConfig.id,
            type = ItemType.TEMPERATURE_VALUE,
            title = UIResourceExtension.getTemperatureString(context, getTemperature()),
            value = getValueForType(ItemType.TEMPERATURE_VALUE),
            isSelected = isItemSelected(ItemType.TEMPERATURE_VALUE),
            iconRes = 0
        )
        updateCallback?.onItemUpdate(newItem)
        return true
    }

    // 检查当前模式是否支持温度调节: 1.制冷模式 4.制热模式
    private fun canAdjustTemperature(): Boolean {
        if (!checkConfig()) {
            return false
        }
        val currentModeIndex = getModeIndex()
        return currentModeIndex == 0 || currentModeIndex == 1 || currentModeIndex == 4
    }

    private fun adjustTemperatureReduce(): Boolean {
        if (!checkConfig()) {
            return false
        }
        if (!canAdjustTemperature()) {
            Toast.makeText(context, R.string.air_conditioner_temperature_refuse_change, Toast.LENGTH_SHORT).show()
            return false
        }
        val settings = config!!.settings.temperature
        setTemperature(maxOf(settings.min, getTemperature() - settings.step))
        val controlConfig = config!!.controls.find { it.type == "TEMPERATURE_VALUE" } ?: return false
        val item = IrControlItem(
            id = controlConfig.id,
            type = ItemType.TEMPERATURE_VALUE,
            title = UIResourceExtension.getTemperatureString( context, getTemperature()),
            value = getValueForType(ItemType.TEMPERATURE_VALUE),
            isSelected = isItemSelected(ItemType.TEMPERATURE_VALUE),
            iconRes = 0
        )
        updateCallback?.onItemUpdate(item)
        return true
    }

    private fun cycleMode(item: IrControlItem) {
        if (!checkConfig()) {
            return
        }
        val controlConfig = config!!.controls.find { it.type == "MODE" } ?: return
        val currentModeIndex = (getModeIndex() + 1) % controlConfig.icons.size
        setModel(currentModeIndex)
        val newItem = IrControlItem(
            id = item.id,
            type = item.type,
            title = UIResourceExtension.getStringByIndex(context, controlConfig, currentModeIndex),
            value = item.value,
            isSelected = false,
            iconRes = UIResourceExtension.getResourceByIndex(context, controlConfig, currentModeIndex)
        )
        L.d(tag, "cycleMode: newItem is ${newItem.toString()}")
        updateCallback?.onItemUpdate(newItem)
    }

    private fun cycleFanSpeed(item: IrControlItem) {

        if (!checkConfig()) {
            return
        }
        val itemConfig = config!!.controls.find { it.type == "FAN_SPEED" } ?: return
        val currentFanSpeedIndex = (getFanSpeedIndex() + 1) % itemConfig.icons.size
        setFanSpeed(currentFanSpeedIndex)
        val newItem = IrControlItem(
            id = item.id,
            type = item.type,
            title = UIResourceExtension.getStringByIndex(context, itemConfig, currentFanSpeedIndex),
            value = item.value,
            isSelected = false,
            iconRes = UIResourceExtension.getResourceByIndex(context, itemConfig, currentFanSpeedIndex)
        )
        updateCallback?.onItemUpdate(newItem)
    }

    private fun cycleVerticalSwing(item: IrControlItem) {

        if (!checkConfig()) {
            return
        }
        val itemConfig = config!!.controls.find { it.type == "WIND_DIRECTION" } ?: return
        val windDirection = (getWindDirection() + 1) % itemConfig.icons.size
        setWindDirection(windDirection)
        val newItem = IrControlItem(
            id = item.id,
            type = item.type,
            title = UIResourceExtension.getStringByIndex(context, itemConfig, windDirection),
            value = item.value,
            isSelected = false,
            iconRes = UIResourceExtension.getResourceByIndex(context, itemConfig, windDirection)
        )
        updateCallback?.onItemUpdate(newItem)
    }

    private fun cycleSwingSwitch(item: IrControlItem) {
        if (!checkConfig()) {
            return
        }
        val itemConfig = config!!.controls.find { it.type == "AUTO_WIND_DIRECTION" } ?: return
        val autoWindDirection = (getAutomaticWindDirection() + 1) % itemConfig.icons.size
        setAutoWindDirection(autoWindDirection)
        val newItem = IrControlItem(
            id = item.id,
            type = item.type,
            title = UIResourceExtension.getStringByIndex(context, itemConfig, autoWindDirection),
            value = item.value,
            isSelected = false,
            iconRes = UIResourceExtension.getResourceByIndex(context, itemConfig, autoWindDirection)
        )
        updateCallback?.onItemUpdate(newItem)
    }


    private fun createControlItems(config: UIControlConfig): List<IrControlItem> {
        return config.controls.map { controlConfig ->
            val type = ItemType.valueOf(controlConfig.type)
            val currentIndex = getCurrentIndexForType(type)
            when (type) {
                ItemType.TEMPERATURE_VALUE -> {
                    IrControlItem(
                        id = controlConfig.id,
                        type = type,
                        title = UIResourceExtension.getTemperatureString( context, getTemperature()),
                        value = getValueForType(type),
                        isSelected = isItemSelected(type),
                        iconRes = 0
                    )
                }

                else -> {
                    IrControlItem(
                        id = controlConfig.id,
                        type = type,
                        title = UIResourceExtension.getStringByIndex(context, controlConfig, currentIndex),
                        value = getValueForType(type),
                        isSelected = isItemSelected(type),
                        iconRes = UIResourceExtension.getResourceByIndex(context, controlConfig, currentIndex)
                    )
                }
            }
        }
    }

    private fun getPowerIndex(): Boolean {
        return parametersSwapper.getPowerIndex(commandProcessor.getPower())
    }

    private fun getPowerValue(isPowerOn: Boolean): Int {
        return parametersSwapper.getPowerValue(isPowerOn)
    }

    private fun getTemperature(): Int {
        return parametersSwapper.getTemperature(commandProcessor.getTemperature())
    }

    private fun setTemperature(temperature: Int) {
        commandProcessor.setTemperature(parametersSwapper.getTemperature(temperature))
    }

    private fun getModeIndex(): Int {
        return parametersSwapper.getModeIndex(commandProcessor.getModel())
    }

    private fun setModel(index: Int) {
        commandProcessor.setModel(parametersSwapper.getModeValue(index))
    }

    private fun getFanSpeedIndex(): Int {
        return  parametersSwapper.getFanSpeedIndex(commandProcessor.getFanSpeed())
    }

    private fun setFanSpeed(index: Int) {
        commandProcessor.setFanSpeed(parametersSwapper.getFanSpeedValue(index))
    }

    private fun getWindDirection(): Int {
        return parametersSwapper.getWindDirectionIndex(commandProcessor.getWindDirection())
    }

    private fun setWindDirection(index: Int) {
        commandProcessor.setWindDirection(parametersSwapper.getWindDirectionValue(index))
    }

    private fun getAutomaticWindDirection(): Int {
        return parametersSwapper.getAutoWindDirectionIndex(commandProcessor.getAutoDirection())
    }

    private fun setAutoWindDirection(index: Int) {
        commandProcessor.setAutoWindDirection(parametersSwapper.getAutoWindDirectionValue(index))
    }

    /**
     * 获取当前State
     */
    fun getCurrentState(): String {
        return currentState
    }
}