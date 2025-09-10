package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.air.handler

import android.content.Context
import android.widget.Toast
import co.candyhouse.app.R
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IROperation
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrControlItem
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.air.ui.AirControllerConfigAdapter
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.IRType
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.handleBase.HandlerCallback
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.handleBase.HandlerConfigAdapter
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.uiBase.UIConfigAdapter
import co.candyhouse.server.CHIRAPIManager
import co.candyhouse.server.CHResult
import co.candyhouse.sesame.server.HttpRespondCode
import co.candyhouse.sesame.utils.L
import com.amazonaws.mobileconnectors.apigateway.ApiClientException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

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

    override fun addIrDeviceToMatter(irRemote: IrRemote?, hub3DeviceId: String) {
        // todo: 添加空调到Matter
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

    override fun getCurrentState( hub3DeviceId: String, remoteDevice: IrRemote): String {
        return (uiConfigAdapter as AirControllerConfigAdapter).getCurrentState()
    }

    override fun getCurrentIRType(): Int {
        return IRType.DEVICE_REMOTE_AIR
    }
}