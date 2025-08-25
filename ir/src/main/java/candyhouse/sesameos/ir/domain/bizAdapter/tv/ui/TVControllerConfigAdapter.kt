package candyhouse.sesameos.ir.domain.bizAdapter.tv.ui

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


class TVControllerConfigAdapter(val context: Context) : UIConfigAdapter {
    private val tag = javaClass.simpleName
    private val gson = Gson()
    private var config: UIControlConfig? = null
    private var updateCallback: ConfigUpdateCallback? = null
    private var isMatched: Boolean = false
    var irTable: MutableList<Array<UInt>> = mutableListOf()
    var irRow: Array<UInt> = arrayOf()
    var specialIrRow: Array<UInt> = arrayOf()
    var currentCode = -1

    fun setupTable(context: Context) {
//        val jsonString = context.resources.openRawResource(R.raw.tv_table)
//            .bufferedReader()
//            .use { it.readText() }
//        val stringArray = Gson().fromJson(jsonString, Array<Array<String>>::class.java)
//        irTable.addAll(
//            stringArray.map { row ->
//                row.map { str ->
//                    if (str.startsWith("0x") || str.startsWith("0X")) {
//                        str.substring(2).toInt(16).toUInt()
//                    } else {
//                        str.toInt().toUInt()
//                    }
//                }.toTypedArray()
//            }.toTypedArray()
//        )
    }

    override suspend fun loadConfig(): UIControlConfig {
        if (config == null) {
            val jsonString = context.resources.openRawResource(R.raw.tv_control_config)
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

    override fun handleItemClick(
        item: IrControlItem,
        device: CHHub3,
        remoteDevice: IrRemote
    ): Boolean {
        // 不做显示上的处理
        return true
    }

    override fun addIrDeviceToMatter(irRemote: IrRemote?, hub3: CHHub3) :Boolean {
        // TODO: 目前不支持添加设备到Matter
        L.d(tag, "addIrDeviceToMatter: not supported yet")
        return false
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
                iconRes = UIResourceExtension.getResource(context, controlConfig),
                optionCode = controlConfig.operateCode
            )
        }
    }

    /**
     * 获取当前State
     */
    fun getCurrentState(): String {
        return ""
    }

    /**
     * 获取code 对应的操作数组
     * 注意：由于table的数据量可能很大，考虑内存与性能，统一作以下处理：
     * 自动匹配场景下，从raw中读取到irTable后，一直放在内存，直到退出界面：适配键值时，可能需要多次从irTable中取row数据。
     * 控制键界面发送键值场景下，从raw中读取到irTable后，取出对应的row，然后释放 irTable：此场景下 row的数据是固定的。
     *
     */
    fun getTableRow(item: IrControlItem, code: Int): UIntArray {
        val SharpSpecialCode = 13709
        val SharpSpecialCodeReLcation = 13710
        val XiaomiSpecialCode = 12937
        val XiaomiSpecialCodeReLcation = 12936
        if (isMatched && irTable.size == 0 || irTable.size == 0) {
            setupTable(context)
        }
        if (irRow.size == 0 || code != currentCode) {
            currentCode = code
            irRow = irTable[code]
            if (code == SharpSpecialCode) { // sharp tv 异常数据处理
                specialIrRow = irTable[SharpSpecialCodeReLcation]
            } else if (code == XiaomiSpecialCode) { // xiaomi tv 异常数据处理
                specialIrRow = irTable[XiaomiSpecialCodeReLcation]
            }
        }
        if (!isMatched) {
            irTable.clear()
        }
        if (code == SharpSpecialCode && item.type == ItemType.HOME && specialIrRow.size > 0) {
            return specialIrRow.toUIntArray()
        } else if (code == XiaomiSpecialCode && (item.type != ItemType.POWER_STATUS_ON || item.type != ItemType.POWER_STATUS_OFF) && specialIrRow.size > 0) {
            return specialIrRow.toUIntArray()
        }
        return irRow.toUIntArray()
    }
}

