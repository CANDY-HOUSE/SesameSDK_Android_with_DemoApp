package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.nonAir.handler

import android.content.Context
import android.widget.Toast
import co.candyhouse.app.R
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IROperation
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrControlItem
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.ItemType
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.handleBase.HandlerCallback
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.handleBase.RemoteHandlerAdapter
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.uiBase.RemoteUIAdapter
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.uiBase.RemoteUIType
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.nonAir.ui.NonAirUIAdapter
import co.candyhouse.server.CHIRAPIManager
import co.candyhouse.server.CHResult
import co.candyhouse.sesame.server.HttpRespondCode
import co.candyhouse.sesame.utils.L
import com.amazonaws.mobileconnectors.apigateway.ApiClientException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class NonAirHandlerAdapter(
    private val context: Context,
    private val uiAdapter: RemoteUIAdapter
) : RemoteHandlerAdapter {

    private val tag = javaClass.simpleName
    private val deviceType: RemoteUIType = (uiAdapter as NonAirUIAdapter).getDeviceType()

    override fun handleItemClick(item: IrControlItem, hub3DeviceId: String, remoteDevice: IrRemote) {
        GlobalScope.launch(Dispatchers.IO) {
            val command = buildCommand(item, remoteDevice)
            if (command.isEmpty()) {
                L.e(tag, "handleItemClick buildCommand is empty!")
                return@launch
            }
            L.d(tag, "handleItemClick buildCommand is:command=${command}, device:${hub3DeviceId}")
            uiAdapter.setCurrentSate(command)
            postCommand(hub3DeviceId, command)
            if (remoteDevice.uuid.isNotEmpty() && remoteDevice.haveSave) {
                CHIRAPIManager.updateIRDeviceState(
                    hub3DeviceId,
                    remoteDevice.uuid,
                    state = command,
                    onResponse = {
                        it.onSuccess {
                            L.d(tag, "update state success{${remoteDevice.uuid}:${command}}")
                        }
                    }
                )
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun addIrDeviceToMatter(irRemote: IrRemote?, hub3DeviceId: String) {
        L.d("harry", "addIrDeviceToMatter: ${irRemote?.alias}  ${irRemote?.uuid}")
        if (irRemote == null || irRemote.uuid.isEmpty()) {
            L.e(tag, "addIrDeviceToMatter irRemote is null or uuid is empty")
            return
        }

        GlobalScope.launch(Dispatchers.IO) {
            val (onTitle, offTitle) = when (deviceType) {
                RemoteUIType.LIGHT -> "開啟" to "關閉"
                else -> "電源" to "電源"
            }

            val itemOn = IrControlItem(
                id = 1,
                type = ItemType.POWER_STATUS_ON,
                title = onTitle,
                value = "",
                isSelected = false,
                iconRes = 0
            )
            val onCommand = buildCommand(itemOn, irRemote)
            if (onCommand.isEmpty()) {
                L.e(tag, "addIrDeviceToMatter buildCommand is empty!")
                return@launch
            }

            val itemOff = IrControlItem(
                id = if (deviceType == RemoteUIType.LIGHT) 3 else 1,
                type = ItemType.POWER_STATUS_OFF,
                title = offTitle,
                value = "",
                isSelected = true,
                iconRes = 0
            )
            val offCommand = buildCommand(itemOff, irRemote)
            if (offCommand.isEmpty()) {
                L.e(tag, "addIrDeviceToMatter buildCommand is empty!")
                return@launch
            }

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
                    L.e(tag, "--->emitIRRemoteDeviceKey error :${exception.statusCode}    /// ${it.message}")
                } else {
                    L.e(tag, "emitIRRemoteDeviceKey error :${it.message}  ")
                }
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)
    private fun buildCommand(item: IrControlItem, remoteDevice: IrRemote): String {
        try {
            val configAdapter = uiAdapter as NonAirUIAdapter
            val key = when (deviceType) {
                RemoteUIType.LIGHT -> configAdapter.paramsSwapper.getLightKey(item.type)
                RemoteUIType.TV -> configAdapter.paramsSwapper.getTVKey(item.type)
                RemoteUIType.FAN -> configAdapter.paramsSwapper.getFanKey(item.type)
                else -> 0
            }

            val command = configAdapter.commandProcess.setKey(key).setCode(remoteDevice.code).buildNonAirCommand()
            return command.toHexString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    override fun clearHandlerCache() {
        // 暂无需要清理的缓存
    }

    override fun setHandlerCallback(handlerCallback: HandlerCallback) {
        // 暂无需要设置的回调
    }

    override fun getCurrentState(hub3DeviceId: String, remoteDevice: IrRemote): String {
        return (uiAdapter as NonAirUIAdapter).getCurrentState()
    }

    override fun getCurrentIRType(): Int {
        return deviceType.irType
    }
}


