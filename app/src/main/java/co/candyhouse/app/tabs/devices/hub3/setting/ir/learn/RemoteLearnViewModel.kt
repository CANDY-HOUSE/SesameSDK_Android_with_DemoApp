package co.candyhouse.app.tabs.devices.hub3.setting.ir.learn

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IRCode
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IROperation
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.IRType
import co.candyhouse.server.CHIRAPIManager
import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.open.CHResultState
import co.candyhouse.sesame.server.IoTSubscriptionManager
import co.candyhouse.sesame.utils.L
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import kotlin.onFailure
import kotlin.onSuccess

class IRDeviceViewModel(val context: Context) : ViewModel() {

    val irCodeLiveData = MutableLiveData<MutableList<IRCode>>()
    val tag = this.javaClass.simpleName
    val irRemoteDeviceLiveData = MutableLiveData<IrRemote>()
    val irCodeChangedLiveData = MutableLiveData<IRCode>()
    private  var hub3DeviceId : String = ""
    var addDeviceSuccess = false

    /**
     * 获取指定IR设备的按键列表
     */
    fun getIrCodeListInfo(onSuccess: () -> Unit, onError: () -> Unit){
        fetchIrCodeList(hub3DeviceId,irRemoteDeviceLiveData.value!!.uuid,
            onSuccess = { list ->
                list.forEach {
                    it.deviceId = hub3DeviceId
                }
                irCodeLiveData.value = list.toMutableList()
                onSuccess()
            L.d(tag, "getIrCodeListInfo success")
        }, onError = {
                L.e(tag, "getIrCodeListInfo error")
                onError()
        })

    }

    private fun fetchIrCodeList(
        hubDeviceUuid: String,
        irDeviceUuid: String,
        onSuccess: (list: List<IRCode>) -> Unit,
        onError: () -> Unit
    ) {
        CHIRAPIManager.getIRCodes(hubDeviceUuid,irDeviceUuid) {
            viewModelScope.launch(Dispatchers.Main) {
                it.onSuccess {
                    onSuccess(it.data)
                }
                it.onFailure {
                    L.e(tag, "getIRDeviceKeysById error : ${it.message}")
                    onError()
                }
            }
        }
    }

    fun getMatchedIRCode(code: IRCode): IRCode? {
        val existingCodes = irCodeLiveData.value ?: return null
        val irIDInt = code.irID.toIntOrNull()

        // 查找匹配的已存在代码
        val matchingCode = existingCodes.find { existingCode ->
            val keyUUIDInt = existingCode.irID?.toIntOrNull()
            if (irIDInt != null && keyUUIDInt != null) {
                keyUUIDInt == irIDInt
            } else {
                existingCode.irID == code.irID
            }
        }

        return matchingCode
    }

    fun changeIRCode(
        item: IRCode, name: String, onSuccess: () -> Unit,
        onChangeError: () -> Unit
    ) {
        val irKey = getMatchedIRCode(item)
        val irDeviceKeyInfo =
            irKey?.let {
                IRCode(
                    it.irID ?: item.irID,
                    name,
                    it.uuid ?: item.uuid,
                    deviceId = item.deviceId
                )
            } ?: item
        CHIRAPIManager.changeIRCode(irDeviceKeyInfo, name) {
            it.onSuccess {
                L.d(tag, "changeIRCode success")
                onSuccess()
            }
            it.onFailure {
                onChangeError()
                L.e(tag, "changeIRCode error : ${it.message}")

            }
        }
    }

    fun deleteIRCode(
        item: IRCode, onSuccess: () -> Unit,
        onDeleteError: () -> Unit
    ) {
        CHIRAPIManager.deleteIRCode(item) {
            it.onSuccess {
                L.d(tag, "deleteIRDeviceKey success")
                onSuccess()
            }
            it.onFailure {
                onDeleteError()
                L.e(tag, "deleteIRDeviceKey =======> error : ${it.message}")
            }
        }
    }

    fun modifyIrRemoteInfo(
        deviceId: String, alias: String = "", onSuccess: () -> Unit = {},
        onError: () -> Unit = {}
    ) {
        val newRemoteDevice = irRemoteDeviceLiveData.value!!.clone()
        newRemoteDevice.alias = alias
        CHIRAPIManager.updateIRDevice(deviceId, newRemoteDevice.uuid, alias) {
            viewModelScope.launch(Dispatchers.Main) {
                irRemoteDeviceLiveData.value = newRemoteDevice
                it.onSuccess {
                    L.d(tag, "modifyIRDevice success")
                    onSuccess()
                }
                it.onFailure {
                    onError()
                    L.e(tag, "modifyIRDevice error : ${it.message}")
                }
            }
        }
    }

    fun addIRDeviceInfo(irCodes:List<IRCode>, onSuccess: () -> Unit = {}, onError: () -> Unit = {}) {
        viewModelScope.launch {
            val state = ""
            val irDeviceType = IRType.DEVICE_REMOTE_CUSTOM
            CHIRAPIManager.addIRDevice(hub3DeviceId, irRemoteDeviceLiveData.value!!, state, irDeviceType, irCodes) { it ->
                it.onSuccess { result ->
                    addDeviceSuccess = true
                    onSuccess()
                    L.d(tag, "addIRDeviceInfo success")
                }
                it.onFailure {
                    L.d(tag, "addIRDeviceInfo fail!")
                    onError()
                    it.printStackTrace()
                }
            }
        }
    }

    fun setHub3DeviceId(hub3DeviceId: String) {
        this.hub3DeviceId= hub3DeviceId
    }

    // 避免lateinit property chHub3 has not been initialized
    fun getHub3DeviceId(): String {
        return hub3DeviceId
    }

    fun setRemoteDevice(irRemote: IrRemote) {
        irRemoteDeviceLiveData.value = irRemote
        L.d(tag, "setRemoteDevice irRemote=${irRemote.toString()}")
    }

    fun getIrRemoteDevice(): IrRemote? {
       return irRemoteDeviceLiveData.value
    }

    fun emitIrLearnCode(deviceId: String, code: String, irDeviceUUID: String, onSuccess: () -> Unit={}, onError: (message:String) -> Unit={}) {
        CHIRAPIManager.emitIRRemoteDeviceKey(deviceId, command = code, operation = IROperation.OPERATION_LEARN_EMIT, irDeviceUUID = irDeviceUUID) {
            it.onSuccess {
                onSuccess.invoke()
                L.d(tag, "emitIRRemoteDeviceKey success  ${it.data}")
            }
            it.onFailure {
                it.message?.let { message ->
                    onError.invoke(message)
                }
                L.e(tag, "emitIRRemoteDeviceKey error :${it.message}  ")
            }
        }
    }

    fun setMode(model: Int) {
        CHIRAPIManager.setIRMode(model, hub3DeviceId) {
            it.onFailure {
                viewModelScope.launch(Dispatchers.Main) {
                    Toast.makeText(context, "${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun unsubscribeLearnDataTopic() {
        try {
            unsubscribeTopic(getLeaningDataTopic())
        } catch (e: Exception) {
            L.e(tag, "unsubscribeTopic error: ${e.message}")
        }
    }

    fun unsubscribeGetModeTopic() {
        try {
            unsubscribeTopic(getGetModeTopic())
        } catch (e: Exception) {
            L.e(tag, "unsubscribeTopic error: ${e.message}")
        }
    }

    fun enterLearnMode() {
        L.d(tag, "enterLearnMode")
        viewModelScope.launch(Dispatchers.IO) {
            setMode(IROperation.MODE_REGISTER)
            try {
                subscribeTopic(getLeaningDataTopic()) { it ->
                    it.onSuccess {
                        val irDataNameUUID = UUID.randomUUID().toString() // 新学习的 UUID 格式的红外码的数据， 直接上云。 固件不再存到 Flash 里。
                        CHIRAPIManager.addHub3LearnedIrData(it.data, hub3DeviceId, irDataNameUUID, irRemoteDeviceLiveData.value!!.uuid, irDataNameUUID)
                        val irCode = IRCode(irDataNameUUID,"", uuid = irRemoteDeviceLiveData.value!!.uuid.uppercase(), deviceId = hub3DeviceId)
                        viewModelScope.launch(Dispatchers.Main) { irCodeChangedLiveData.value = irCode }
                    }
                }
            }catch (e: Exception) {
                L.e(tag, "enterLearnMode error: ${e.message}")
            }
        }
    }

    fun exitLearnMode() {
        L.d("tag", "exitLearnMode")
        viewModelScope.launch (Dispatchers.IO ) {
            setMode(IROperation.MODE_CONTROL)
            unsubscribeLearnDataTopic()
        }
    }

    fun getMode(onResponse: (Byte) -> Unit) {
        subscribeTopic(getGetModeTopic()) { it ->
            it.onSuccess { data->
                val jsonString = String(data.data)
                val mode = extractValueWithJson(jsonString)
                mode?.let { onResponse(it.toByte()) }
            }
        }
        CHIRAPIManager.getIRMode(hub3DeviceId) {
            it.onFailure {
                viewModelScope.launch(Dispatchers.Main) {
                    Toast.makeText(context, "${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    fun extractValueWithJson(jsonString: String): Int? {
        return try {
            val jsonObject = JSONObject(jsonString)
            jsonObject.optInt("ir_mode")
        } catch (e: Exception) {
            null
        }
    }

    override fun onCleared() {
        L.d(tag, "onCleared")
        super.onCleared()
        irCodeLiveData.value = mutableListOf()
    }
    fun updateLearnDataToServer() {
        viewModelScope.launch(Dispatchers.IO) {
            subscribeTopic(getLeaningDataTopic()) {
                it.onSuccess { data ->
                    unsubscribeLearnDataTopic()
                    val irDataNameUUID = UUID.randomUUID().toString()
                    // 旧的数据， 发射一次后， 固件会把数据发到云端， APP接收后， 生成 UUID ， 存到云端， 以后从云端发数据， 而不是发 id。
                    CHIRAPIManager.addHub3LearnedIrData(data.data, hub3DeviceId, irDataNameUUID, irRemoteDeviceLiveData.value!!.uuid, irDataNameUUID)
                }
            }
        }
    }

    private fun getLeaningDataTopic()  = "hub3/${hub3DeviceId}/ir/learned/data"
    private fun getGetModeTopic()  = "hub3/${hub3DeviceId}/ir/mode"

    fun unsubscribeTopic(topic: String) {
        L.d("unsubscribeLearnData", "topic: $topic")
        IoTSubscriptionManager.unsubscribeTopic(topic)
    }

    fun subscribeTopic(topic:String, result: CHResult<ByteArray>) {
        L.d("getIrLearnedData", "topic: $topic")
        IoTSubscriptionManager.subscribeTopic(topic) { it ->
            it.onSuccess {
                L.d("getIrLearnedData", "收到 " + it.data.size + " 个字节的红外数据")
                result.invoke(Result.success(CHResultState.CHResultStateNetworks(it.data)))
            }
        }
    }
}

class IRLearnViewModelFactory(
    val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IRDeviceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return IRDeviceViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}