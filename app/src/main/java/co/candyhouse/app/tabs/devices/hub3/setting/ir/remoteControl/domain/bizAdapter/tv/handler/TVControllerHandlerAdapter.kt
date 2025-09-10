package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.tv.handler

import android.content.Context
import android.widget.Toast
import co.candyhouse.app.R
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IROperation
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrControlItem
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.ItemType
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.IRType
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.handleBase.HandlerCallback
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.handleBase.HandlerConfigAdapter
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.uiBase.UIConfigAdapter
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.tv.ui.TVControllerConfigAdapter
import co.candyhouse.server.CHIRAPIManager
import co.candyhouse.server.CHResult
import co.candyhouse.sesame.server.HttpRespondCode
import co.candyhouse.sesame.utils.L
import com.amazonaws.mobileconnectors.apigateway.ApiClientException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class TVControllerHandlerAdapter(val context: Context, val uiConfigAdapter: UIConfigAdapter) :
    HandlerConfigAdapter {
    private val tag = TVControllerHandlerAdapter::class.java.simpleName

    override fun handleItemClick(item: IrControlItem, hub3DeviceId: String, remoteDevice: IrRemote) {
        GlobalScope.launch(Dispatchers.IO) {
            val command = buildCommand(item, remoteDevice)
            if (command.isEmpty()) {
                L.e(tag, "handleItemClick buildCommand is empty!")
                return@launch
            }
            L.d(tag,"handleItemClick buildCommand is:command=${command}, device:${ hub3DeviceId }")
            uiConfigAdapter.setCurrentSate(command)
            postCommand(hub3DeviceId, command)
            if (remoteDevice.uuid.isNotEmpty() && remoteDevice.haveSave) {
                CHIRAPIManager.updateIRDeviceState(
                    hub3DeviceId,
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

    override fun addIrDeviceToMatter(irRemote: IrRemote?, hub3DeviceId: String) {

        // IrRemote(model=AGLED, alias='AGLED AD9-CH1🖋️', uuid='3C0095A0-C481-4C4C-900C-C1FC6292BD7A', state=300092eeee367600004a, timestamp=1755826008980, type=57344, code=97, keys=[], direction='null')
        L.d("harry", "addIrDeviceToMatter: ${irRemote?.alias}  ${irRemote?.uuid}")
        if (irRemote == null || irRemote.uuid.isEmpty()) {
            L.e(tag, "addIrDeviceToMatter irRemote is null or uuid is empty")
            return
        }
        GlobalScope.launch(Dispatchers.IO) {
            // IrControlItem(id=1, type=POWER_STATUS_ON, title=電源, value=, isSelected=true, iconRes=0)
            val itemOn: IrControlItem = IrControlItem(
                id = 1,
                type = ItemType.POWER_STATUS_ON,
                title = "電源",
                value = "",
                isSelected = false,
                iconRes = 0
            )
            val onCommand = buildCommand(itemOn, irRemote)
            if (onCommand.isEmpty()) {
                L.e(tag, "addIrDeviceToMatter buildCommand is empty!")
                return@launch
            }
            L.d("harry", "addIrDeviceToMatter buildCommand is: onCommand=${onCommand}, device:${ hub3DeviceId }")

            val itemOff: IrControlItem = IrControlItem(
                id = 1,
                type = ItemType.POWER_STATUS_OFF,
                title = "電源",
                value = "",
                isSelected = true,
                iconRes = 0
            )
            val offCommand = buildCommand(itemOff, irRemote)
            if (offCommand.isEmpty()) {
                L.e(tag, "addIrDeviceToMatter buildCommand is empty!")
                return@launch
            }

            L.d("harry", "addIrDeviceToMatter buildCommand is: offCommand=${offCommand}, device:${hub3DeviceId}")
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

    override fun modifyIRDeviceInfo(hub3DeviceId: String, remoteDevice: IrRemote, onResponse: CHResult<Any>) {
        CHIRAPIManager.updateIRDevice(hub3DeviceId, remoteDevice.uuid, alias = remoteDevice.alias, onResponse = onResponse)
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
    @OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)
    private fun buildCommand(item: IrControlItem, remoteDevice: IrRemote): String {
        val configAdapter: TVControllerConfigAdapter = uiConfigAdapter as TVControllerConfigAdapter
        val key = configAdapter.paramsSwapper.getTVKey(item.type)
        try {
            val command = configAdapter.commandProcess.setKey(key)
                .setCode(remoteDevice.code)
                .buildNoneAirCommand()
            return command.toHexString()
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
        return (uiConfigAdapter as TVControllerConfigAdapter).getCurrentState()
    }

    override fun getCurrentIRType(): Int {
        return IRType.DEVICE_REMOTE_TV
    }

}