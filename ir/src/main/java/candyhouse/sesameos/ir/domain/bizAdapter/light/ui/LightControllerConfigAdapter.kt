package candyhouse.sesameos.ir.domain.bizAdapter.light.ui


import android.content.Context
import candyhouse.sesameos.ir.R
import candyhouse.sesameos.ir.base.IrCompanyCode
import candyhouse.sesameos.ir.base.IrRemote
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.uiBase.ConfigUpdateCallback
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.uiBase.UIConfigAdapter
import candyhouse.sesameos.ir.ext.Ext
import candyhouse.sesameos.ir.ext.UIResourceExtension
import candyhouse.sesameos.ir.models.IrControlItem
import candyhouse.sesameos.ir.models.ItemType
import candyhouse.sesameos.ir.models.UIControlConfig
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.utils.L
import com.google.gson.Gson


class LightControllerConfigAdapter(val context: Context) : UIConfigAdapter {
    private val tag = javaClass.simpleName
    private val gson = Gson()
    private var config: UIControlConfig? = null
    private var updateCallback: ConfigUpdateCallback? = null
    private var isPowerOn: Boolean = false
    private var isMatched: Boolean = false
    var irTable: MutableList<Array<UInt>> = mutableListOf()
    var irRow : Array<UInt> = arrayOf()
    var currentCode = -1

    fun setupTable(context: Context) {
        val jsonString = context.resources.openRawResource(R.raw.light_table)
            .bufferedReader()
            .use { it.readText() }

        // 先解析为字符串数组
        val stringArray = Gson().fromJson(jsonString, Array<Array<String>>::class.java)

        // 手动转换hex字符串为Int
        irTable.addAll(
            stringArray.map { row ->
                row.map { str ->
                    if (str.startsWith("0x") || str.startsWith("0X")) {
                        str.substring(2).toInt(16).toUInt()
                    } else {
                        str.toInt().toUInt()
                    }
                }.toTypedArray()
            }.toTypedArray()
        )
    }

    override suspend fun loadConfig(): UIControlConfig {
        if (config == null) {
            val jsonString = context.resources.openRawResource(R.raw.light_control_config)
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
        irTable.clear()
        irRow = arrayOf()
    }

    override fun setConfigUpdateCallback(uiItemCallback: ConfigUpdateCallback) {
        updateCallback = uiItemCallback
    }

    override fun matchRemoteDevice(irRemote: IrRemote) {
        if (irRemote.code > 0 ) {
            return
        }
        if (irRemote.state.isNullOrEmpty()){
            return
        }
        irRemote.code = convertToDecimal(irRemote.state!!) ?: 0
    }
    private fun convertToDecimal(value: String): Int? {
        return try {
            if (value.length < 8) {
                L.Companion.d(tag, "convertToDecimal length < 8")
                return null
            }
            val byte2 = value.substring(4, 6)
            val byte3 = value.substring(6, 8)
            if (!isValidHex(byte2) || !isValidHex(byte3)) {
                L.Companion.d(tag, "convertToDecimal contains invalid hex")
                return null
            }
            (byte2 + byte3).toInt(16)
        } catch (e: Exception) {
            L.Companion.d(tag, "convertToDecimal convert failed:${value} ${e.message}")
            null
        }
    }
    private fun isValidHex(str: String): Boolean {
        return str.matches("[0-9A-Fa-f]+".toRegex())
    }

    override suspend fun getCompanyCodeList(context: Context): List<IrCompanyCode> {
        return return Ext.parseCompanyTableToList(context,R.raw.light_conpany_code)
    }

    override fun getMatchUiItemList(): List<IrControlItem> {
        isMatched = true
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
                type = ItemType.BRIGHTNESS_UP,
                title = context.getString(R.string.light_brightness_up),
                iconRes = 0,
                isSelected = false,
                value = "",
                optionCode = ""
            )
        )
        list.add(
            IrControlItem(
                id = 2,
                type = ItemType.BRIGHTNESS_DOWN,
                title = context.getString(R.string.light_brightness_down),
                iconRes = 0,
                isSelected = false,
                value = "",
                optionCode = ""
            )
        )
        list.add(
            IrControlItem(
                id = 3,
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

    override fun getMatchItem(
        position: Int,
        items: List<IrControlItem>
    ): IrControlItem? {
        return when (position) {
            0 ->  getItemByType(ItemType.POWER_STATUS_ON,items) // 开
            1 ->  getItemByType(ItemType.BRIGHTNESS_UP,items) // 亮度+
            2 ->  getItemByType(ItemType.BRIGHTNESS_DOWN,items) // 亮度-
            3 ->  getItemByType(ItemType.POWER_STATUS_OFF,items) // 关
            else -> return null
        }
    }

    private fun getItemByType(type:ItemType,items: List<IrControlItem>):IrControlItem? {
        items.forEach{
            if (type == it.type) {
                return it
            }
        }
        return null
    }

    override fun initMatchParams() {

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
            title = UIResourceExtension.getString(context, controlConfig.titles[0]),
            value = "",
            isSelected = when (type) {
                ItemType.POWER_STATUS_ON -> isPowerOn
                ItemType.POWER_STATUS_OFF -> !isPowerOn
                else -> false
            },
            iconRes = UIResourceExtension.getIconResourceId(context, controlConfig.icons[0]),
            optionCode = controlConfig.operateCode
        )
        updateCallback?.onItemUpdate(item)
    }

    private fun createControlItems(config: UIControlConfig): List<IrControlItem> {
        return config.controls.map { controlConfig ->
            val type = ItemType.valueOf(controlConfig.type)
            IrControlItem(
                id = controlConfig.id,
                type = type,
                title = UIResourceExtension.getString(context, controlConfig.titles[0]),
                value = "",
                isSelected = when (type) {
                    ItemType.POWER_STATUS_ON -> isPowerOn
                    ItemType.POWER_STATUS_OFF -> !isPowerOn
                    else -> false
                },
                iconRes = UIResourceExtension.getIconResourceId(
                    context,
                    controlConfig.icons[0]
                ),
                optionCode = controlConfig.operateCode
            )
        }
    }

    /**
     * 获取当前State
     */
    fun getCurrentState():String {
        return ""
    }

    /**
     * 获取code 对应的操作数组
     * 注意：由于table的数据量可能很大，考虑内存与性能，统一作以下处理：
     * 自动匹配场景下，从raw中读取到irTable后，一直放在内存，直到退出界面：适配键值时，可能需要多次从irTable中取row数据。
     * 控制键界面发送键值场景下，从raw中读取到irTable后，取出对应的row，然后释放 irTable：此场景下 row的数据是固定的。
     *
     */
    fun getTableRow(code:Int): UIntArray {
        if (isMatched && irTable.size == 0 || irTable.size == 0) {
            setupTable(context)
        }
        if (irRow.size == 0 || code != currentCode) {
            currentCode = code
            irRow = irTable[code]
        }
        if (!isMatched) {
            irTable.clear()
        }
        return irRow.toUIntArray()
    }
}