package candyhouse.sesameos.ir.domain.bizAdapter.air.ui


import android.content.Context
import android.widget.Toast
import candyhouse.sesameos.ir.R
import candyhouse.sesameos.ir.base.IrCompanyCode
import candyhouse.sesameos.ir.base.IrRemote
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.uiBase.ConfigUpdateCallback
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.uiBase.UIConfigAdapter
import candyhouse.sesameos.ir.domain.bizAdapter.air.handler.AirProcessor
import candyhouse.sesameos.ir.ext.Ext
import candyhouse.sesameos.ir.ext.IRDeviceType
import candyhouse.sesameos.ir.ext.UIResourceExtension
import candyhouse.sesameos.ir.models.IrControlItem
import candyhouse.sesameos.ir.models.ItemType
import candyhouse.sesameos.ir.models.UIControlConfig
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.utils.L
import com.google.gson.Gson
import kotlin.collections.forEach

/**
 * 每个模式下有不同的风速、摆风、温度等控制项；后续需要扩展出一个列表来。
 * 当前按键更新结束后，需要更新界面。并且需要同步到服务器和其他界面。
 */
class AirControllerConfigAdapter(val context: Context) : UIConfigAdapter {
    private val tag = javaClass.simpleName
    private val gson = Gson()
    private var config: UIControlConfig? = null
    private var updateCallback: ConfigUpdateCallback? = null
    private var currentTemperature: Int = 25

    private var isPowerOn: Boolean = false
    private var currentModeIndex: Int = 0
    private var currentFanSpeedIndex: Int = 0
    private var currentVerticalSwingIndex: Int = 0
    private var currentSwingSwitchIndex: Int = 0
    private var currentState: String = ""

    override suspend fun loadConfig(): UIControlConfig {
        if (config == null) {
            val jsonString = context.resources.openRawResource(R.raw.air_control_config)
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

    override fun matchRemoteDevice(irRemote: IrRemote) {
        L.d(tag, "matchRemoteDevice: irRemote $irRemote")

        // 如果不是空调设备或已有code，直接返回
        if (irRemote.type != IRDeviceType.DEVICE_REMOTE_AIR || irRemote.code > 0) {
            return
        }

        // 获取空调类型列表
        val irTypeList = Ext.parseJsonToDeviceList(context, R.raw.air_control_type)

        // 先尝试通过model匹配
        if (matchByModel(irRemote, irTypeList)) {
            return
        }

        // 如果model匹配失败，尝试通过state匹配
        if (!matchByState(irRemote, irTypeList)) {
            L.e(tag, "matchRemoteDevi  ce: failed to match remote device")
        }

    }

    override fun setCurrentSate(state: String?) {
        if (state.isNullOrEmpty()) {
            return
        }
        currentState = state
        updateUIConfig()
    }

    override suspend fun getCompanyCodeList(context: Context): List<IrCompanyCode> {
        return Ext.parseCompanyTableToList(context,R.raw.air_conpany_code)
    }

    /**
     * currentMatchKeyIndex 0: 开机
     * currentMatchKeyIndex 1: 除湿
     * currentMatchKeyIndex 2: 送风
     * currentMatchKeyIndex 3: 制热
     * currentMatchKeyIndex 4: 自动
     * currentMatchKeyIndex 5: 制冷
     * currentMatchKeyIndex 6: 温度：26
     * currentMatchKeyIndex 7: 最小风量
     * currentMatchKeyIndex 8: 关机
     */
    /**
     *     val id: Int,
     *     val type: ItemType,
     *     val title: String,
     *     val value: String = "",
     *     val isSelected: Boolean = false,
     *     val iconRes: Int,
     *     val optionCode:String
     */
    override fun getMatchUiItemList(): List<IrControlItem> {
        val list: MutableList<IrControlItem> = mutableListOf()
        list.add(
            IrControlItem(
                id = 0,
                type = ItemType.POWER_STATUS_ON,
                title = context.getString(R.string.ir_remote_power_on),
                iconRes = 0,
                isSelected = false,
                value = "",
                optionCode = ""
            )
        )
        list.add(
            IrControlItem(
                id = 1,
                type = ItemType.MODE,
                title = context.getString(R.string.air_conditioner_model_auto_01),
                iconRes = 0,
                isSelected = false,
                value = "",
                optionCode = ""
            )
        )
        list.add(
            IrControlItem(
                id = 2,
                type = ItemType.MODE,
                title = context.getString(R.string.air_conditioner_model_hot_05),
                iconRes = 0,
                isSelected = false,
                value = "",
                optionCode = ""
            )
        )
        list.add(
            IrControlItem(
                id = 3,
                type = ItemType.MODE,
                title = context.getString(R.string.air_conditioner_model_cold_02),
                iconRes = 0,
                isSelected = false,
                value = "",
                optionCode = ""
            )
        )
        list.add(
            IrControlItem(
                id = 4,
                type = ItemType.TEMPERATURE_VALUE,
                title = context.getString(R.string.air_conditioner_temperature) + ":26°C",
                iconRes = 0,
                isSelected = false,
                value = "",
                optionCode = ""
            )
        )
        list.add(
            IrControlItem(
                id = 5,
                type = ItemType.FAN_SPEED,
                title = context.getString(R.string.air_conditioner_wind_speed_v3),
                iconRes = 0,
                isSelected = false,
                value = "",
                optionCode = ""
            )
        )
        list.add(
            IrControlItem(
                id = 6,
                type = ItemType.POWER_STATUS_OFF,
                title = context.getString(R.string.ir_remote_power_off),
                iconRes = 0,
                isSelected = false,
                value = "",
                optionCode = ""
            )
        )
        return list
    }

    override fun getMatchItem(position: Int,items:List<IrControlItem>): IrControlItem? {
        return when (position) {
            0 -> {
                return items[0]
            }
            1 -> { // 自动模式
                setModeIndex(4)
                return items[4]
            }
            2 -> { // 制热模式
                setModeIndex(3)
                return items[4]
            }
            3 -> { // 制冷模式
                setModeIndex(0)
                return items[4]
            }
            4 -> {//温度
                setTemperature(25)
                return items[3]
            }
            5 -> { // 最大风量
                setFanSpeedIndex(2)
                return items[6]
            }
            6 -> {
                return items[2]
            }
            else -> {
                return null
            }
        }
    }

    override fun initMatchParams() {
        setPower(false)
        setTemperature(25)
        setModeIndex(1)
        setFanSpeedIndex(2)
    }

    private fun updateUIConfig() {
        val air = AirProcessor()
        val parseSuccess = air.parseAirData(currentState)
        if (!parseSuccess) {
            L.e(tag, "updateUIConfig: failed to parse state")
            return
        }
        isPowerOn = getPower(air)
        currentTemperature = getTemperature(air)
        currentModeIndex = getModeIndex(air)
        currentFanSpeedIndex = getFanSpeedIndex(air)
        currentVerticalSwingIndex = getVerticalSwingIndex(air)
        currentSwingSwitchIndex = getHorizontalSwingIndex(air)
        L.d(tag,"updateUIConfig: isPowerOn $isPowerOn, currentTemperature $currentTemperature, currentModeIndex $currentModeIndex, currentFanSpeedIndex $currentFanSpeedIndex, currentVerticalSwingIndex $currentVerticalSwingIndex, currentSwingSwitchIndex $currentSwingSwitchIndex")
        config?.let {
            val items = createControlItems(it)
            L.d(tag, "updateUIConfig: items.size ${items.size}")
            items.forEach { item->
                updateCallback?.onItemUpdate(item)
            }
        }
    }

    private fun getPower(air: AirProcessor): Boolean {
        return air.mPower == 0x01
    }

    private fun getTemperature(air: AirProcessor): Int {
        return air.mTemperature
    }

    private fun getModeIndex(air: AirProcessor): Int {
        return when (air.mMode) {
            0x01 -> 0
            0x02 -> 1
            0x03 -> 2
            0x04 -> 3
            0x05 -> 4
            else -> 0
        }
    }

    private fun getFanSpeedIndex(air: AirProcessor): Int {
        return when (air.mWindRate) {
            0x01 -> 0
            0x02 -> 1
            0x03 -> 2
            0x04 -> 3
            else -> 0
        }
    }

    private fun getVerticalSwingIndex(air: AirProcessor): Int {
        return when (air.mWindDirection) {
            0x01 -> 0
            0x02 -> 1
            0x03 -> 2
            else -> 0
        }
    }

    private fun getHorizontalSwingIndex(air: AirProcessor): Int {
        return when (air.mAutomaticWindDirection) {
            0x01 -> 0
            0x00 -> 1
            else -> 0
        }
    }

    private fun matchByModel(irRemote: IrRemote, irTypeList: List<IrRemote>): Boolean {
        val matchedType = irTypeList.find { it.model == irRemote.model }
        if (matchedType != null) {
            irRemote.code = matchedType.code
            L.d(tag, "matchByModel: matched code ${irRemote.code}")
            return true
        }
        return false
    }

    private fun matchByState(irRemote: IrRemote, irTypeList: List<IrRemote>): Boolean {
        // 检查 state 是否为空
        val state = irRemote.state
        if (state == null) {
            L.d(tag, "matchByState: state is null")
            return false
        }

        // 转换十进制
        val code = convertToDecimal(state)
        if (code == null) {
            L.d(tag, "matchByState: failed to convert state to code")
            return false
        }

        // 查找匹配的类型
        val matchedType = irTypeList.find { it.code == code }
        if (matchedType == null) {
            L.d(tag, "matchByState: no matching type found for code $code")
            return false
        }
        // 更新信息
        irRemote.code = code
        irRemote.model = matchedType.model
        L.d(tag, "matchByState: matched code $code, model ${matchedType.model}")
        return true
    }


    private fun convertToDecimal(value: String): Int? {
        return try {
            // 检查字符串长度
            if (value.length < 8) {
                L.d(tag, "convertToDecimal length < 8")
                return null
            }

            // 提取第2、3位
            val byte2 = value.substring(4, 6)
            val byte3 = value.substring(6, 8)

            // 验证是否为有效的16进制数
            if (!isValidHex(byte2) || !isValidHex(byte3)) {
                L.d(tag, "convertToDecimal contains invalid hex")
                return null
            }

            // 合并并转换
            (byte2 + byte3).toInt(16)
        } catch (e: Exception) {
            L.d(tag, "convertToDecimal convert failed:${value} ${e.message}")
            null
        }
    }

    // 检查字符串是否为有效的16进制数
    private fun isValidHex(str: String): Boolean {
        return str.matches("[0-9A-Fa-f]+".toRegex())
    }


    override fun handleItemClick(item: IrControlItem, device: CHHub3, remoteDevice: IrRemote): Boolean {
        when (item.type) {
            ItemType.POWER_STATUS_ON -> {
                isPowerOn = true
                togglePowerOn(item, device)
                togglePowerOff(device)
            }

            ItemType.POWER_STATUS_OFF -> {
                isPowerOn = false
                togglePowerOn(device)
                togglePowerOff(item, device)
            }

            ItemType.TEMP_CONTROL_ADD -> return adjustTemperatureAdd(item, device)
            ItemType.TEMP_CONTROL_REDUCE -> return adjustTemperatureReduce(item, device)
            ItemType.MODE -> cycleMode(item, device)
            ItemType.FAN_SPEED -> cycleFanSpeed(item, device)
            ItemType.SWING_VERTICAL -> cycleVerticalSwing(item, device)
            ItemType.SWING_HORIZONTAL -> cycleSwingSwitch(item, device)
            else -> {
                L.e(tag, "Unknown item type")
            }
        }
        return true
    }

    private fun getCurrentIndexForType(type: ItemType): Int {
        return when (type) {
            ItemType.MODE -> currentModeIndex
            ItemType.FAN_SPEED -> currentFanSpeedIndex
            ItemType.SWING_VERTICAL -> currentVerticalSwingIndex
            ItemType.SWING_HORIZONTAL -> currentSwingSwitchIndex
            ItemType.POWER_STATUS_ON, ItemType.POWER_STATUS_OFF -> if (isPowerOn) 0 else 1
            else -> 0
        }
    }

    private fun getValueForType(type: ItemType): String {
        return when (type) {
            ItemType.TEMPERATURE_VALUE -> "${currentTemperature}°C"
            else -> ""
        }
    }

    private fun isItemSelected(type: ItemType): Boolean {
        return when (type) {
            ItemType.POWER_STATUS_ON -> isPowerOn
            ItemType.POWER_STATUS_OFF -> !isPowerOn
            else -> false
        }
    }

    private fun checkConfig(): Boolean {
        return config != null && config!!.controls.isNotEmpty()
    }

    private fun togglePowerOn(device: CHHub3) {
        val resId = if (isPowerOn) 0 else 1
        if (!checkConfig()) {
            return
        }
        val controlConfig = config!!.controls.find { it.type == "POWER_STATUS_ON" } ?: return
        val newItem = IrControlItem(
            id = controlConfig.id,
            type = ItemType.POWER_STATUS_ON,
            title = UIResourceExtension.getStringByIndex(context,controlConfig,resId),
            value = controlConfig.defaultValue,
            isSelected = isItemSelected(ItemType.POWER_STATUS_ON),
            iconRes = UIResourceExtension.getResourceByIndex(context, controlConfig, resId),
            optionCode = controlConfig.operateCode
        )
        updateCallback?.onItemUpdate(newItem)
    }


    private fun togglePowerOn(item: IrControlItem, device: CHHub3) {
        val resId = if (isPowerOn) 0 else 1
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
            iconRes = UIResourceExtension.getResourceByIndex(context, controlConfig, resId),
            optionCode = controlConfig.operateCode
        )
        updateCallback?.onItemUpdate(newItem)
    }

    private fun togglePowerOff(device: CHHub3) {
        val resId = if (isPowerOn) 0 else 1
        if (!checkConfig()) {
            return
        }
        val controlConfig = config!!.controls.find { it.type == "POWER_STATUS_OFF" } ?: return
        val newItem = IrControlItem(
            id = controlConfig.id,
            type = ItemType.POWER_STATUS_OFF,
            title = UIResourceExtension.getStringByIndex(context,controlConfig,resId),
            value = controlConfig.defaultValue,
            isSelected = isItemSelected(ItemType.POWER_STATUS_OFF),
            iconRes = UIResourceExtension.getResourceByIndex(context, controlConfig, resId),
            optionCode = controlConfig.operateCode
        )
        updateCallback?.onItemUpdate(newItem)
    }

    private fun togglePowerOff(item: IrControlItem, device: CHHub3) {
        var resId = if (isPowerOn) 0 else 1
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
            iconRes = UIResourceExtension.getResourceByIndex(context, controlConfig, resId),
            optionCode = controlConfig.operateCode
        )
        updateCallback?.onItemUpdate(newItem)
    }

    private fun adjustTemperatureAdd(item: IrControlItem, device: CHHub3):Boolean {
        if (!checkConfig()) {
            return false
        }
        if (!canAdjustTemperature()) {
            Toast.makeText(context, R.string.air_conditioner_temperature_refuse_change, Toast.LENGTH_SHORT).show()
            return false
        }
        val settings = config!!.settings.temperature
        currentTemperature = minOf(settings.max, currentTemperature + settings.step)
        val controlConfig = config!!.controls.find { it.type == "TEMPERATURE_VALUE" } ?: return false
        val newItem = IrControlItem(
            id = controlConfig.id,
            type = ItemType.TEMPERATURE_VALUE,
            title = UIResourceExtension.getTemperatureString(
                context,
                currentTemperature
            ),
            value = getValueForType(ItemType.TEMPERATURE_VALUE),
            isSelected = isItemSelected(ItemType.TEMPERATURE_VALUE),
            iconRes = 0,
            optionCode = controlConfig.operateCode
        )
        updateCallback?.onItemUpdate(newItem)
        return true
    }
    // 检查当前模式是否支持温度调节: 1.制冷模式 4.制热模式
    private fun canAdjustTemperature():Boolean {
        if (!checkConfig()) {
            return false
        }
        if (currentModeIndex == 1 || currentModeIndex == 4 ) {
            return true
        }
        return false
    }

    private fun adjustTemperatureReduce(item: IrControlItem, device: CHHub3): Boolean {
        if (!checkConfig()) {
            return false
        }
        if (!canAdjustTemperature()) {
            Toast.makeText(context, R.string.air_conditioner_temperature_refuse_change, Toast.LENGTH_SHORT).show()
            return false
        }
        val settings = config!!.settings.temperature
        currentTemperature = maxOf(settings.min, currentTemperature - settings.step)
        val controlConfig = config!!.controls.find { it.type == "TEMPERATURE_VALUE" } ?: return false
        val item = IrControlItem(
            id = controlConfig.id,
            type = ItemType.TEMPERATURE_VALUE,
            title = UIResourceExtension.getTemperatureString(
                context,
                currentTemperature
            ),
            value = getValueForType(ItemType.TEMPERATURE_VALUE),
            isSelected = isItemSelected(ItemType.TEMPERATURE_VALUE),
            iconRes = 0,
            optionCode = controlConfig.operateCode
        )
        updateCallback?.onItemUpdate(item)
        return true
    }

    private fun cycleMode(item: IrControlItem, device: CHHub3) {
        if (!checkConfig()) {
            return
        }
        val controlConfig = config!!.controls.find { it.type == "MODE" } ?: return
        currentModeIndex = (currentModeIndex + 1) % controlConfig.icons.size
        val newItem = IrControlItem(
            id = item.id,
            type = item.type,
            title = UIResourceExtension.getStringByIndex(context,controlConfig,currentModeIndex),
            value = item.value,
            isSelected = false,
            iconRes = UIResourceExtension.getResourceByIndex(context, controlConfig, currentModeIndex),
            optionCode = controlConfig.operateCode
        )
        L.d(tag, "cycleMode: newItem is ${newItem.toString()}")
        updateCallback?.onItemUpdate(newItem)
    }

    private fun cycleFanSpeed(item: IrControlItem, device: CHHub3) {

        if (!checkConfig()) {
            return
        }
        val itemConfig = config!!.controls.find { it.type == "FAN_SPEED" } ?: return
        currentFanSpeedIndex = (currentFanSpeedIndex + 1) % itemConfig.icons.size
        val newItem = IrControlItem(
            id = item.id,
            type = item.type,
            title = UIResourceExtension.getStringByIndex(context,itemConfig,currentFanSpeedIndex),
            value = item.value,
            isSelected = false,
            iconRes = UIResourceExtension.getResourceByIndex(context, itemConfig, currentFanSpeedIndex),
            optionCode = item.optionCode
        )
        updateCallback?.onItemUpdate(newItem)
    }

    private fun cycleVerticalSwing(item: IrControlItem, device: CHHub3) {

        if (!checkConfig()) {
            return
        }
        val itemConfig = config!!.controls.find { it.type == "SWING_VERTICAL" } ?: return
        currentVerticalSwingIndex = (currentVerticalSwingIndex + 1) % itemConfig.icons.size
        val newItem = IrControlItem(
            id = item.id,
            type = item.type,
            title = UIResourceExtension.getStringByIndex(context,itemConfig,currentVerticalSwingIndex),
            value = item.value,
            isSelected = false,
            iconRes = UIResourceExtension.getResourceByIndex(context, itemConfig, currentVerticalSwingIndex),
            optionCode = item.optionCode
        )
        updateCallback?.onItemUpdate(newItem)
    }

    private fun cycleSwingSwitch(item: IrControlItem, device: CHHub3) {
        if (!checkConfig()) {
            return
        }
        val itemConfig = config!!.controls.find { it.type == "SWING_HORIZONTAL" } ?: return
        currentSwingSwitchIndex = (currentSwingSwitchIndex + 1) % itemConfig.icons.size
        val newItem = IrControlItem(
            id = item.id,
            type = item.type,
            title = UIResourceExtension.getStringByIndex(context,itemConfig,currentSwingSwitchIndex),
            value = item.value,
            isSelected = false,
            iconRes = UIResourceExtension.getResourceByIndex(context, itemConfig, currentSwingSwitchIndex),
            optionCode = item.optionCode
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
                        title = UIResourceExtension.getTemperatureString(
                            context,
                            currentTemperature
                        ),
                        value = getValueForType(type),
                        isSelected = isItemSelected(type),
                        iconRes = 0,
                        optionCode = controlConfig.operateCode
                    )
                }
                else -> {
                    IrControlItem(
                        id = controlConfig.id,
                        type = type,
                        title = UIResourceExtension.getStringByIndex(context,controlConfig,currentIndex),
                        value = getValueForType(type),
                        isSelected = isItemSelected(type),
                        iconRes = UIResourceExtension.getResourceByIndex(context, controlConfig, currentIndex),
                        optionCode = controlConfig.operateCode
                    )
                }
            }
        }
    }

    /**
     * 获取空调开关机状态
     */
    fun getPower() = isPowerOn

    /**
     * 获取空调模式指引
     */
    fun getModeIndex(): Int {
        return currentModeIndex
    }

    /**
     * 获取空调风速指引
     */
    fun getFanSpeedIndex(): Int {
        return currentFanSpeedIndex
    }

    /**
     * 获取空调垂直摆风指引
     */
    fun getVerticalSwingIndex(): Int {
        return currentVerticalSwingIndex
    }

    /**
     * 获取空调摆风开关指引 （自动/关闭）
     */
    fun getHorizontalSwingIndex(): Int {
        return currentSwingSwitchIndex
    }

    /**
     * 获取空调温度
     */
    fun getTemperature(): Int {
        return currentTemperature
    }

    /**
     * 获取当前State
     */
    fun getCurrentState():String {
        return currentState
    }

    /**
     * 设置空调开关机状态
     */
    fun setPower(isPowerOn: Boolean) {
        this.isPowerOn = isPowerOn
    }

    /**
     * 设置空调模式指引
     */
    fun setModeIndex(index: Int) {
        this.currentModeIndex = index
    }

    /**
     * 设置空调风速指引
     */
    fun setFanSpeedIndex(index: Int) {
        currentFanSpeedIndex = index
    }

    /**
     * 设置空调垂直摆风指引
     */
    fun setVerticalSwingIndex(index: Int) {
        currentVerticalSwingIndex = index
    }

    /**
     * 设置空调摆风开关指引 （自动/关闭）
     */
    fun setHorizontalSwingIndex(index: Int) {
        currentSwingSwitchIndex = index
    }

    /**
     * 设置空调温度
     */
    fun setTemperature(temperature: Int) {
        currentTemperature = temperature
    }

}