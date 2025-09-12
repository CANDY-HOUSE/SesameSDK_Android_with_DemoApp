package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.air.handler

import android.content.Context
import android.widget.Toast
import co.candyhouse.app.R
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IROperation
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrControlItem
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.ItemType
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.air.ui.AirControllerConfigAdapter
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.IRType
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.handleBase.HandlerCallback
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.handleBase.HandlerConfigAdapter
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.uiBase.UIConfigAdapter
import co.candyhouse.server.CHIRAPIManager
import co.candyhouse.server.CHResult
import co.candyhouse.sesame.server.HttpRespondCode
import co.candyhouse.sesame.utils.L
import co.utils.hexStringToByteArray
import com.amazonaws.mobileconnectors.apigateway.ApiClientException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.text.toHexString

class AirControllerHandlerAdapter(val context: Context, val uiConfigAdapter: UIConfigAdapter) : HandlerConfigAdapter {
    private val tag = AirControllerHandlerAdapter::class.java.simpleName

    override fun handleItemClick(item: IrControlItem, hub3DeviceId: String, remoteDevice: IrRemote) {
        GlobalScope.launch(Dispatchers.IO) {
            val command = buildCommand(item, remoteDevice)
            if (command.isEmpty()) {
                L.e(tag, "handleItemClick buildCommand is empty!")
                return@launch
            }
            L.d(tag, "handleItemClick buildCommand is:command=${command}, device:${hub3DeviceId}")
            uiConfigAdapter.setCurrentSate(command)
            postCommand(hub3DeviceId, command)
            if (remoteDevice.uuid.isNotEmpty() && remoteDevice.haveSave) {
                CHIRAPIManager.updateIRDeviceState(hub3DeviceId, remoteDevice.uuid, state = command) {
                    it.onSuccess {
                        L.d(tag, "updata state success{${remoteDevice.uuid}:${command}}")
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun addIrDeviceToMatter(irRemote: IrRemote?, hub3DeviceId: String) {
        L.d("harry", "addIrDeviceToMatter: ${irRemote?.alias}  ${irRemote?.uuid}")
        if (irRemote == null || irRemote.uuid.isEmpty()) {
            L.e(tag, "addIrDeviceToMatter irRemote is null or uuid is empty")
            return
        }
        GlobalScope.launch(Dispatchers.IO) {
            // 默认是 off 状态， 不确定用户保存此遥控器时， UI 当前是什么状态
            // TODO: buildCommand 需要改进， 不要依赖于 UI 上的状态, 希望只根据 ItemType 来生成命令

            // 假定是 off 状态，生成 on 和 off 命令

            // IrControlItem(id=3, type=POWER_STATUS_OFF, title=關閉, value=, isSelected=false, iconRes=2131231013)
            val itemOff: IrControlItem = IrControlItem(
                id = 3, type = ItemType.POWER_STATUS_OFF, title = "關閉", value = "", isSelected = false, iconRes = 2131231013
            )
            var offCommand = buildCommand(itemOff, irRemote)
            if (offCommand.isEmpty()) {
                L.e(tag, "addIrDeviceToMatter buildCommand is empty!")
                return@launch
            }

            // UI可能是 on 状态，那么此时的 offCommand 实际是 onCommand, 确保改成 off
            val offCommandBuf = offCommand.hexStringToByteArray()
            offCommandBuf[8] = 0x00

            // 重新计算校验和
            offCommandBuf[15] = (offCommandBuf.dropLast(1).sumOf { it.toUByte().toInt() } and 0xFF).toByte()
            offCommand = offCommandBuf.toHexString().uppercase()
            L.d("harry", "addIrDeviceToMatter buildCommand is: offCommand=${offCommand}, device:${hub3DeviceId}")

            // IrControlItem(id=1, type=POWER_STATUS_ON, title=開啟, value=, isSelected=false, iconRes=2131231013)
            // val itemOn: IrControlItem = IrControlItem(
            //     id = 1, type = ItemType.POWER_STATUS_ON, title = "開啟", value = "", isSelected = false, iconRes = 2131231013
            // )
            // val onCommand = buildCommand(itemOn, irRemote)
            // if (onCommand.isEmpty()) {
            //     L.e(tag, "addIrDeviceToMatter buildCommand is empty!")
            //     return@launch
            // }

            // offCommand=300102CE19010201000102010000FF21, onCommand=300102CE19010201010102010000FF22
            // 根据 offCommand 生成 onCommand，
            val onCommandBuf = offCommand.hexStringToByteArray()
            onCommandBuf[8] = 0x01
            onCommandBuf[15] = (onCommandBuf.dropLast(1).sumOf { it.toUByte().toInt() } and 0xFF).toByte()
            val onCommand = onCommandBuf.toHexString().uppercase()
            L.d("harry", "addIrDeviceToMatter buildCommand is: onCommand=${onCommand}, device:${hub3DeviceId}")

            CHIRAPIManager.addIRRemoteDeviceToMatter(onCommand, offCommand, irRemote, hub3DeviceId) {
                it.onSuccess { response ->
                    L.d("harry", "addIrDeviceToMatter success: ${response.data}")
                }
                it.onFailure { error ->
                    L.e(tag, "addIrDeviceToMatter error: ${error.message}")
                }
            }
        }
    }

    override fun modifyIRDeviceInfo(
        hub3DeviceId: String, remoteDevice: IrRemote, onResponse: CHResult<Any>
    ) {
        CHIRAPIManager.updateIRDevice(
            hub3DeviceId, remoteDevice.uuid, alias = remoteDevice.alias, onResponse = onResponse
        )
    }

    private fun postCommand(deviceId: String, command: String) {
        CHIRAPIManager.emitIRRemoteDeviceKey(deviceId, command = command, operation = IROperation.OPERATION_REMOTE_EMIT, irType = getCurrentIRType()) {
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
    private fun buildCommand(item: IrControlItem, remoteDevice: IrRemote): String {
        L.d("harry", "buildCommand item: $item , remoteDevice: $remoteDevice")
        try {
            val configAdapter: AirControllerConfigAdapter = uiConfigAdapter as AirControllerConfigAdapter
            val key = configAdapter.parametersSwapper.getAirKey(item.type)
            val command = configAdapter.commandProcessor.setKey(key).setCode(remoteDevice.code).buildAirCommand()

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

    override fun getCurrentState(hub3DeviceId: String, remoteDevice: IrRemote): String {
        return (uiConfigAdapter as AirControllerConfigAdapter).getCurrentState()
    }

    override fun getCurrentIRType(): Int {
        return IRType.DEVICE_REMOTE_AIR
    }
}