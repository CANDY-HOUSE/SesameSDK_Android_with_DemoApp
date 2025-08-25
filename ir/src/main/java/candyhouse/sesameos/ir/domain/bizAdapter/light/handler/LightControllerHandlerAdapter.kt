package candyhouse.sesameos.ir.domain.bizAdapter.light.handler

import android.content.Context
import android.widget.Toast
import candyhouse.sesameos.ir.R
import candyhouse.sesameos.ir.base.IrRemote
import candyhouse.sesameos.ir.domain.bizAdapter.air.handler.AirProcessor
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.IRType
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.handleBase.HandlerCallback
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.handleBase.HandlerConfigAdapter
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.uiBase.UIConfigAdapter
import candyhouse.sesameos.ir.domain.bizAdapter.light.ui.LightControllerConfigAdapter
import candyhouse.sesameos.ir.ext.IROperation
import candyhouse.sesameos.ir.models.IrControlItem
import candyhouse.sesameos.ir.models.ItemType
import candyhouse.sesameos.ir.server.CHIRAPIManager
import candyhouse.sesameos.ir.server.CHResult
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.server.HttpRespondCode
import co.candyhouse.sesame.utils.L
import com.amazonaws.mobileconnectors.apigateway.ApiClientException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LightControllerHandlerAdapter(val context: Context, val uiConfigAdapter: UIConfigAdapter) : HandlerConfigAdapter {
    private val tag = LightControllerHandlerAdapter::class.java.simpleName
    val air = AirProcessor()
    override fun handleItemClick(
        item: IrControlItem, device: CHHub3, remoteDevice: IrRemote
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            L.d("harry", "handleItemClick: $item + $remoteDevice")
            val key = getCurrentKey(item)
            val command = buildCommand(key, remoteDevice)
            if (command.isEmpty()) {
                L.e(tag, "handleItemClick buildCommand is empty!")
                return@launch
            }
            L.d(
                tag, "handleItemClick buildCommand is:command=${command}, device:${
                    device.deviceId.toString().uppercase()
                }"
            )
            uiConfigAdapter.setCurrentSate(command)
            postCommand(device.deviceId.toString().uppercase(), command)
            if (remoteDevice.uuid.isNotEmpty() && remoteDevice.haveSave) {
                CHIRAPIManager.updateIRDeviceState(
                    device.deviceId.toString().uppercase(), remoteDevice.uuid, state = command, onResponse = {
                        it.onSuccess {
                            L.d(tag, "updata state success{${remoteDevice.uuid}:${command}}")
                        }
                    })
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun addIrDeviceToMatter(irRemote: IrRemote?, hub3: CHHub3) {
        // IrRemote(model=AGLED, alias='AGLED AD9-CH1🖋️', uuid='3C0095A0-C481-4C4C-900C-C1FC6292BD7A', state=300092eeee367600004a, timestamp=1755826008980, type=57344, code=97, keys=[], direction='null')
        L.d("harry", "addIrDeviceToMatter: ${irRemote?.alias}  ${irRemote?.uuid}")
        if (irRemote == null || irRemote.uuid.isEmpty()) {
            L.e(tag, "addIrDeviceToMatter irRemote is null or uuid is empty")
            return
        }
        GlobalScope.launch(Dispatchers.IO) {
            // id=1, type=POWER_STATUS_ON, title=開啟, value=, isSelected=false, iconRes=0, optionCode=49153
            val itemOn: IrControlItem = IrControlItem(
                id = 1,
                type = ItemType.POWER_STATUS_ON,
                title = "開啟",
                value = "",
                isSelected = false,
                iconRes = 0,
                optionCode = "49153",
            )
            val keyOn = getCurrentKey(itemOn)
            val onCommand = buildCommand(keyOn, irRemote)
            if (onCommand.isEmpty()) {
                L.e(tag, "addIrDeviceToMatter buildCommand is empty!")
                return@launch
            }
            L.d("harry", "addIrDeviceToMatter buildCommand is: onCommand=${onCommand}, device:${hub3.deviceId.toString().uppercase()}")

            // IrControlItem(id=3, type=POWER_STATUS_OFF, title=關閉, value=, isSelected=true, iconRes=0, optionCode=49155)
            val itemOff: IrControlItem = IrControlItem(
                id = 3,
                type = ItemType.POWER_STATUS_OFF,
                title = "關閉",
                value = "",
                isSelected = true,
                iconRes = 0,
                optionCode = "49155",
            )
            val keyOff = getCurrentKey(itemOff)
            val offCommand = buildCommand(keyOff, irRemote)
            if (offCommand.isEmpty()) {
                L.e(tag, "addIrDeviceToMatter buildCommand is empty!")
                return@launch
            }

            L.d("harry", "addIrDeviceToMatter buildCommand is: offCommand=${offCommand}, device:${hub3.deviceId.toString().uppercase()}")
            CHIRAPIManager.addIRRemoteDeviceToMatter(onCommand, offCommand, irRemote, hub3) {
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
        device: CHHub3, remoteDevice: IrRemote, onResponse: CHResult<Any>
    ) {
        CHIRAPIManager.updateIRDevice(
            device.deviceId.toString().uppercase(), remoteDevice.uuid, alias = remoteDevice.alias, onResponse = onResponse
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
                            Toast.makeText(context, R.string.key_unsupported, Toast.LENGTH_SHORT).show()
                        }
                    }
                    L.e(
                        tag, "--->emitIRRemoteDeviceKey error :${exception.statusCode}    /// ${it.message}"
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
        operationKey: Int, remoteDevice: IrRemote
    ): String {
        try {
            val row = (uiConfigAdapter as LightControllerConfigAdapter).getTableRow(remoteDevice.code)
            val buf = mutableListOf<UByte>()
            buf.add(0x30u)
            buf.add(0x00u)
            buf.add(row[0].toUByte())
            buf.add(row[(operationKey - 1) * 2 + 1].toUByte())
            buf.add(row[(operationKey - 1) * 2 + 2].toUByte())
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
        device: CHHub3, remoteDevice: IrRemote
    ): String {
        return (uiConfigAdapter as LightControllerConfigAdapter).getCurrentState()
    }

    override fun getCurrentIRDeviceType(): Int {
        return IRType.DEVICE_REMOTE_LIGHT
    }

    /**
     * /*   	  开灯			关灯				亮度+			亮度-			模式			设置			定时+			定时-		  色温+			色温-			1			2			3			4			5			6			A			B			C			D*/
     * {0x1d,0x2c,0x25,0x2f,0x26,0x2a,0x23,0x2b,0x22,0x80,0xb9,0x82,0xbb,0xff,0x00,0xff,0x00,0x8b,0xb2,0x8a,0xb3,0xff,0x00,0xff,0x00,0xff,0x00,0xff,0x00,0xff,0x00,0xff,0x00,0xff,0x00,0xff,0x00,0xff,0x00,0xff,0x00,0x2c,0x52,0x09,0x00},
     *  POWER_STATUS_ON = 0x01,
     *  MODE = 0x05,
     *  POWER_STATUS_OFF = 0x02,
     *  BRIGHTNESS_UP = 0x03,
     *  BRIGHTNESS_DOWN = 0x04,
     *  COLOR_TEMP_UP = 0x09,
     *  COLOR_TEMP_DOWN = 0x0A,
     */
    fun getCurrentKey(item: IrControlItem): Int {
        return when (item.type) {
            ItemType.POWER_STATUS_ON -> 0x01
            ItemType.POWER_STATUS_OFF -> 0x02
            ItemType.MODE -> 0x05
            ItemType.BRIGHTNESS_UP -> 0x03
            ItemType.BRIGHTNESS_DOWN -> 0x04
            ItemType.COLOR_TEMP_UP -> 0x09
            ItemType.COLOR_TEMP_DOWN -> 0x0A
            else -> {
                L.e(tag, "Unknown item type")
                0x01
            }
        }
    }
}