package candyhouse.sesameos.ir.domain.bizAdapter.tv.handler

import android.content.Context
import android.widget.Toast
import candyhouse.sesameos.ir.R
import candyhouse.sesameos.ir.base.IrRemote
import candyhouse.sesameos.ir.domain.bizAdapter.air.handler.AirProcessor
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.IRType
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
    val air = AirProcessor()
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
        item: IrControlItem,
        remoteDevice: IrRemote
    ): String {
        val key = getCurrentKey(item)
        try {
            var row = (uiConfigAdapter as TVControllerConfigAdapter).getTableRow(item, remoteDevice.code)
            val buf = mutableListOf<UByte>()
            buf.add(0x30u)
            buf.add(0x00u)
            buf.add(row[0].toUByte())
            buf.add(row[(key - 1) * 2 + 1].toUByte())
            buf.add(row[(key - 1) * 2 + 2].toUByte())
            buf.add(row[row.size - 4].toUByte())
            buf.add(row[row.size - 3].toUByte())
            buf.add(row[row.size - 2].toUByte())
            buf.add(row[row.size - 1].toUByte())

            // 计算校验和（所有字节的和的低8位）
            val checksum = buf.sumOf { it.toInt() }.toByte().toUByte()
            buf.add(checksum)
            return buf.toUByteArray().toHexString()
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

    /**
    KEY_TV_VOLUME_OUT =  0x01,// vol-
    KEY_TV_CHANNEL_IN =  0x02,// ch+
    KEY_TV_MENU =  0x03,// menu
    KEY_TV_CHANNEL_OUT =  0x04,// ch-
    KEY_TV_VOLUME_IN =  0x05,// vol+
    KEY_TV_POWER =  0x06,// power
    KEY_TV_MUTE =  0x07,// mute
    KEY_TV_KEY1 =  0x08,// 1 2 3 4 5 6 7 8 9
    KEY_TV_KEY2 =  0x09,
    KEY_TV_KEY3 =  0x0A,
    KEY_TV_KEY4 =  0x0B,
    KEY_TV_KEY5 =  0x0C,
    KEY_TV_KEY6 =  0x0D,
    KEY_TV_KEY7 =  0x0E,
    KEY_TV_KEY8 =  0x0F,
    KEY_TV_KEY9 =  0x10,
    KEY_TV_SELECT =  0x11,// -/--
    KEY_TV_KEY0 =  0x12,// 0
    KEY_TV_AV_TV =  0x13,// AV/TV
    KEY_TV_BACK =  0x14,// back
    KEY_TV_OK =  0x15,// ok
    KEY_TV_UP =  0x16,// up
    KEY_TV_LEFT =  0x17,// left
    KEY_TV_RIGHT =  0x18,// right
    KEY_TV_DOWN =  0x19,// down
    KEY_TV_HOME =  0x1A,// home
     */

    fun getCurrentKey(item: IrControlItem): Int {
        return when (item.type) {
            ItemType.POWER_STATUS_ON -> 0x06
            ItemType.POWER_STATUS_OFF -> 0x06
            ItemType.MUTE -> 0x07
            ItemType.BACK -> 0x14
            ItemType.UP -> 0x16
            ItemType.MENU -> 0x03
            ItemType.LEFT -> 0x17
            ItemType.OK -> 0x15
            ItemType.RIGHT -> 0x18
            ItemType.VOLUME_UP -> 0x05
            ItemType.DOWN -> 0x19
            ItemType.CHANNEL_UP -> 0x02
            ItemType.VOLUME_DOWN -> 0x01
            ItemType.HOME -> 0x1A
            ItemType.CHANNEL_DOWN -> 0x04
            else -> {
                L.e(tag, "Unknown item type")
                0x01
            }
        }
    }

}