package candyhouse.sesameos.ir.domain.bizAdapter.air.handler

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import candyhouse.sesameos.ir.R
import candyhouse.sesameos.ir.base.IrRemote
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.uiBase.UIConfigAdapter
import candyhouse.sesameos.ir.domain.bizAdapter.air.ui.AirControllerConfigAdapter
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.handleBase.HandlerCallback
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.handleBase.HandlerConfigAdapter
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.IRType
import candyhouse.sesameos.ir.ext.Ext
import candyhouse.sesameos.ir.ext.IROperation
import candyhouse.sesameos.ir.models.IrControlItem
import candyhouse.sesameos.ir.models.ItemType
import candyhouse.sesameos.ir.server.CHIRAPIManager
import candyhouse.sesameos.ir.server.CHResult
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.server.HttpRespondCode
import co.candyhouse.sesame.utils.L
import com.amazonaws.mobileconnectors.apigateway.ApiClientException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AirControllerHandlerAdapter(val context: Context, val uiConfigAdapter: UIConfigAdapter) :
    HandlerConfigAdapter {
    private val tag = AirControllerHandlerAdapter::class.java.simpleName
    val air = AirProcessor()

    init {
        air.setupTable(context)
    }

    override fun handleItemClick(
        item: IrControlItem,
        device: CHHub3,
        remoteDevice: IrRemote
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            val key = getCurrentKey(item)
            val command = buildCommand(key, remoteDevice)
            if (command.isEmpty()) {
                L.e(tag, "handleItemClick buildCommand is empty!")
                return@launch
            }
            L.d(
                tag,
                "handleItemClick buildCommand is:command=${command}, device:${
                    device.deviceId.toString().uppercase()
                }"
            )
            uiConfigAdapter.setCurrentSate(command)
            postCommand(device.deviceId.toString().uppercase(), command)
            if (remoteDevice.uuid.isNotEmpty() && remoteDevice.haveSave) {
                CHIRAPIManager.updateIRDeviceState(
                    device.deviceId.toString().uppercase(),
                    remoteDevice.uuid,
                    state = command,
                    onResponse = {
                        it.onSuccess {
                            L.d(tag, "updata state success{${remoteDevice.uuid}:${command}}")
                        }
                    }
                )
            }
        }
    }

    override fun modifyIRDeviceInfo(
        device: CHHub3,
        remoteDevice: IrRemote,
        onResponse: CHResult<Any>
    ) {
        CHIRAPIManager.updateIRDevice(
            device.deviceId.toString().uppercase(),
            remoteDevice.uuid,
            alias = remoteDevice.alias,
            onResponse = onResponse
        )
    }

    private fun postCommand(deviceId: String, command: String) {
        CHIRAPIManager.emitIRRemoteDeviceKey(deviceId, command = command, operation = IROperation.OPERATION_REMOTE_EMIT) {
            it.onSuccess {
                L.d(tag, "emitIRRemoteDeviceKey success  ${it.data}")
            }
            it.onFailure {
                if (it is ApiClientException) {
                    val exception: ApiClientException = it
                    if (exception.statusCode == HttpRespondCode.DATA_NOT_ALLOWED) {
                        GlobalScope.launch(Dispatchers.Main) {
                            Toast.makeText(context, R.string.key_unsupported, Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    L.e(
                        tag,
                        "--->emitIRRemoteDeviceKey error :${exception.statusCode}    /// ${it.message}"
                    )
                } else {
                    L.e(tag, "emitIRRemoteDeviceKey error :${it.message}  ")
                }

            }
        }
    }

    /**
     * 生成命令。来自hxd格式。
     */
    @OptIn(ExperimentalStdlibApi::class)
    private fun buildCommand(
        operationKey: Int,
        remoteDevice: IrRemote
    ): String {
        try {
            val configAdapter: AirControllerConfigAdapter =
                uiConfigAdapter as AirControllerConfigAdapter
            air.buildParamsWithPower(getPower(configAdapter.getPower()))
                .buildParamsWithModel(getMode(configAdapter.getModeIndex()))
                .buildParamsWithTemperature(getTemperature(configAdapter.getTemperature()))
                .buildParamsWithFanSpeed(getFanSpeed(configAdapter.getFanSpeedIndex()))
                .buildParamsWithWindDirection(getVerticalSwingIndex(configAdapter.getVerticalSwingIndex()))
                .buildParamsWithAutomaticWindDirection(getHorizontalSwingIndex(configAdapter.getHorizontalSwingIndex()))
            air.mKey = operationKey
            val data = air.search(remoteDevice.code)
            return data.toHexString().uppercase()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }


    override fun clearHandlerCache() {

    }

    override fun setHandlerCallback(handlerCallback: HandlerCallback) {

    }

    override fun getCurrentState(
        device: CHHub3,
        remoteDevice: IrRemote
    ): String {
        return (uiConfigAdapter as AirControllerConfigAdapter).getCurrentState()
    }

    override fun getCurrentIRDeviceType(): Int {
        return IRType.DEVICE_REMOTE_AIR
    }

    /**
     * 获取空调开关指令
     * 0x01//开机
     * 0x00//关机
     */
    fun getPower(isPowerOn: Boolean): Int {
        return if (isPowerOn) {
            0x01
        } else {
            0x00
        }
    }

    /**
     * 获取空调模式指令
     * 0x01//自动(默认)
     * 0X02//制冷：
     * 0X03//抽湿
     * 0x04//送风
     * 0x05//制热
     */
    fun getMode(mode: Int): Int {
        return when (mode) {
            0 -> 0x01
            1 -> 0x02
            2 -> 0x03
            3 -> 0x04
            4 -> 0x05
            else -> 0x01
        }
    }

    /**
     * 获取空调风速指引
     * 风量数据：
     * 0x01//自动
     * 0x02//低
     * 0x03//中
     * 0x04//高
     */
    fun getFanSpeed(index: Int): Int {
        return when (index) {
            0 -> 0x01
            1 -> 0x02
            2 -> 0x03
            3 -> 0x04
            else -> 0x01
        }
    }

    /**
     * 获取空调垂直摆风指引
     * 手动风向：
     * 0x01//向上
     * 0x02//中
     * 0x03//向下
     * 默认02,与显示对应;
     */
    fun getVerticalSwingIndex(index: Int): Int {
        return when (index) {
            0 -> 0x01
            1 -> 0x02
            2 -> 0x03
            else -> 0x02
        }
    }

    /**
     * 获取空调摆风开关指引 （自动/关闭）
     * 自动风向：
     * 0x01//打开,
     * 0x00//关闭,
     * 默认开:01
     */
    fun getHorizontalSwingIndex(index: Int): Int {
        return when (index) {
            0 -> 0x01
            1 -> 0x00
            else -> 0x01
        }
    }

    /**
     * 获取空调温度
     * 0x13//19度
     * 0x14//20度
     * 0x15//21度
     * ....
     * 0x1d//29度
     * 0x1e//30度
     * 默认：25度,0x19;
     */
    fun getTemperature(temperature: Int): Int {
        return temperature
    }

    /**
     * 获取当前按键指令
     * x01//电源
     * 0x02//模式
     * 0x03//风量
     * 0x04//手动风向
     * 0x05//自动风向
     * 0x06//温度加
     * 0x07//温度减
     * 表示当前按下的是哪个键
     */
    fun getCurrentKey(item: IrControlItem): Int {
        return when (item.type) {
            ItemType.POWER_STATUS_ON -> 0x01
            ItemType.POWER_STATUS_OFF -> 0x01
            ItemType.TEMP_CONTROL_ADD -> 0x06
            ItemType.TEMP_CONTROL_REDUCE -> 0x07
            ItemType.MODE -> 0x02
            ItemType.FAN_SPEED -> 0x03
            ItemType.SWING_VERTICAL -> 0x04
            ItemType.SWING_HORIZONTAL -> 0x05
            else -> {
                L.e(tag, "Unknown item type")
                0x01
            }
        }
    }
}