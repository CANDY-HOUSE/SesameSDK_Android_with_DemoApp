package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.air.handler

import android.content.Context
import android.widget.Toast
import co.candyhouse.app.R
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IROperation
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrControlItem
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.ItemType
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.air.ui.AirControllerConfigAdapter
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.IRType
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.handleBase.HandlerCallback
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.handleBase.HandlerConfigAdapter
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.uiBase.UIConfigAdapter
import co.candyhouse.server.CHIRAPIManager
import co.candyhouse.server.CHResult
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.server.HttpRespondCode
import co.candyhouse.sesame.utils.L
import com.amazonaws.mobileconnectors.apigateway.ApiClientException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AirControllerHandlerAdapter(val context: Context, val uiConfigAdapter: UIConfigAdapter) :
    HandlerConfigAdapter {
    private val tag = AirControllerHandlerAdapter::class.java.simpleName

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
            L.d(tag, "handleItemClick buildCommand is:command=${command}, device:${device.deviceId.toString().uppercase()}")
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

    override fun addIrDeviceToMatter(irRemote: IrRemote?, hub3: CHHub3) {
        // todo: 添加空调到Matter
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
        CHIRAPIManager.emitIRRemoteDeviceKey(deviceId, command = command, operation = IROperation.OPERATION_REMOTE_EMIT, irType = getCurrentIRType()){
            it.onSuccess {
                L.d(tag, "emitIRRemoteDeviceKey success  ${it.data}")
            }
            it.onFailure {
                if (it is ApiClientException) {
                    val exception: ApiClientException = it
                    if (exception.statusCode == HttpRespondCode.DATA_NOT_ALLOWED) {
                        GlobalScope.launch(Dispatchers.Main) {
                            Toast.makeText(context, R.string.key_unsupported, Toast.LENGTH_SHORT).show()
                        }
                    }
                    L.e(tag, "emitIRRemoteDeviceKey error :${exception.statusCode}   ${it.message}")
                } else {
                    L.e(tag, "emitIRRemoteDeviceKey error :${it.message}  ")
                }

            }
        }
    }

    /**
     * 生成命令。来自hxd格式。
     */
    @OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)
    private fun buildCommand(
        operationKey: Int,
        remoteDevice: IrRemote
    ): String {
        try {
            val configAdapter: AirControllerConfigAdapter = uiConfigAdapter as AirControllerConfigAdapter
            val command = configAdapter.hxdCommandProcessor.setKey(operationKey)
                .setCode(remoteDevice.code)
                .buildAirCommand()

            return command.toHexString().uppercase()
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

    override fun getCurrentIRType(): Int {
        return IRType.DEVICE_REMOTE_AIR
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
            ItemType.WIND_DIRECTION -> 0x04
            ItemType.AUTO_WIND_DIRECTION -> 0x05
            else -> {
                L.e(tag, "Unknown item type")
                0x01
            }
        }
    }
}