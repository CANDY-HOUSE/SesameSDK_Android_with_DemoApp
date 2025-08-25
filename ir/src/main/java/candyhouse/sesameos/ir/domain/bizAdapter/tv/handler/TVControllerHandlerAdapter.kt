package candyhouse.sesameos.ir.domain.bizAdapter.tv.handler

import android.content.Context
import android.widget.Toast
import candyhouse.sesameos.ir.R
import candyhouse.sesameos.ir.base.IrRemote
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.handleBase.HXDCommandProcessor
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.IRType
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.handleBase.HXDParametersSwapper
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.handleBase.HandlerCallback
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.handleBase.HandlerConfigAdapter
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.uiBase.UIConfigAdapter
import candyhouse.sesameos.ir.domain.bizAdapter.tv.ui.TVControllerConfigAdapter
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
import kotlinx.coroutines.launch


class TVControllerHandlerAdapter(val context: Context, val uiConfigAdapter: UIConfigAdapter) :
    HandlerConfigAdapter {
    private val tag = TVControllerHandlerAdapter::class.java.simpleName
    val commandProcess = HXDCommandProcessor()
    val paramsSwapper = HXDParametersSwapper()

    override fun handleItemClick(
        item: IrControlItem,
        device: CHHub3,
        remoteDevice: IrRemote
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            val command = buildCommand(item, remoteDevice)
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

    override fun addIrDeviceToMatter(irRemote: IrRemote?, hub3: CHHub3) {
        // todo: 添加红外设备到Matter
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
        CHIRAPIManager.emitIRRemoteDeviceKey(deviceId, command = command, operation = IROperation.OPERATION_REMOTE_EMIT, brandType = getCurrentIRDeviceType()) {
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
    @OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)
    private fun buildCommand(
        item: IrControlItem,
        remoteDevice: IrRemote
    ): String {
        val key = paramsSwapper.getTVKey(item.type)
        try {
            val command = commandProcess.setKey(key)
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

    override fun getCurrentState(
        device: CHHub3,
        remoteDevice: IrRemote
    ): String {
        return (uiConfigAdapter as TVControllerConfigAdapter).getCurrentState()
    }

    override fun getCurrentIRDeviceType(): Int {
        return IRType.DEVICE_REMOTE_TV
    }

}