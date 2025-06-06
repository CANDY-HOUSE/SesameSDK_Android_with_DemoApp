package candyhouse.sesameos.ir.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import candyhouse.sesameos.ir.base.CHHub3IRCode
import candyhouse.sesameos.ir.base.IrRemote
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.IRType
import candyhouse.sesameos.ir.server.CHIRAPIManager
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.utils.L
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IRDeviceViewModel : ViewModel() {

    val irCodeLiveData = MutableLiveData<MutableList<CHHub3IRCode>>()
    val tag = this.javaClass.simpleName
    val irRemoteDeviceLiveData = MutableLiveData<IrRemote>()
    lateinit var chHub3 : CHHub3
    var addDeviceSuccess = false

    /**
     * 获取指定IR设备的按键列表
     */
    fun getIrCodeListInfo(onSuccess: () -> Unit, onError: () -> Unit){
        L.d("harry", "[getIrCodeListInfo] chHub3.deviceId.toString().uppercase(): " + chHub3.deviceId.toString().uppercase() + "; irRemoteDeviceLiveData.value!!.uuid: " + irRemoteDeviceLiveData.value!!.uuid)
        fetchIrCodeList(chHub3.deviceId.toString().uppercase(),irRemoteDeviceLiveData.value!!.uuid,
            onSuccess = { list ->
                list.forEach {
                    it.deviceId = chHub3.deviceId.toString().uppercase()
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
        onSuccess: (list: List<CHHub3IRCode>) -> Unit,
        onError: () -> Unit
    ) {
        CHIRAPIManager.getIRCodes(hubDeviceUuid,irDeviceUuid) {
            L.d("harry", "[getIRCodes] hubDeviceUuid: $hubDeviceUuid irDeviceUuid: $irDeviceUuid")
            viewModelScope.launch(Dispatchers.Main) {
                it.onSuccess {
                    L.d(tag, "getIRCodes success deviceId: $irDeviceUuid size=${it.data.size}; data: ${it.data}")
                    onSuccess(it.data)
                }
                it.onFailure {
                    L.e(tag, "getIRDeviceKeysById error : ${it.message}")
                    onError()
                }
            }
        }
    }

    /**
     * 匹配已存在的按键列表，更新名称
     */
    fun matchIrCode(listCodes: MutableList<CHHub3IRCode>,fetchIrCodes:MutableList<CHHub3IRCode>? = irCodeLiveData.value,  irRemoteId: String, deviceId: String) {
        if (fetchIrCodes.isNullOrEmpty()) {
            return
        }
        L.d(tag, "matchIrCode:deviceId=${deviceId} ${listCodes.toString()}")
        // 遍历入参列表，更新名称
        for (i in listCodes.indices) {
            val code = listCodes[i]
            val irIDInt = code.irID.toIntOrNull()
            // 查找匹配的已存在代码
            val matchingCode = fetchIrCodes.find { fetchIrCode ->
                val keyUUIDInt = fetchIrCode.irID?.toIntOrNull()
                if (irIDInt != null && keyUUIDInt != null) {
                    keyUUIDInt == irIDInt
                } else {
                    fetchIrCode.irID == code.irID
                }
            }
            if (null == matchingCode) {
                L.d(tag, "matchIrCode: is null ")
                listCodes[i] = CHHub3IRCode(code.irID, code.name, irRemoteId, deviceId)
            } else {
                var name = matchingCode.name ?: code.name
                var uuid = matchingCode.uuid ?: code.uuid
                L.d(tag, "matchIrCode: ${code.irID} ${name} ${uuid}")
                listCodes[i] = CHHub3IRCode(code.irID, name, uuid, deviceId)
            }
        }
        listCodes.forEach {
            L.d(tag, "matchIrCode, haveItem: ${it.toString()}")
        }
    }

    fun getMatchedIRCode(code: CHHub3IRCode): CHHub3IRCode? {
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
        item: CHHub3IRCode, name: String, onSuccess: () -> Unit,
        onChangeError: () -> Unit
    ) {
        val irKey = getMatchedIRCode(item)
        val irDeviceKeyInfo =
            irKey?.let {
                CHHub3IRCode(
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
//                getIRCodes(item.uuid, onSuccess, onLoadError)
            }
            it.onFailure {
                onChangeError()
                L.e(tag, "changeIRCode error : ${it.message}")

            }
        }
    }

    fun deleteIRCode(
        item: CHHub3IRCode, onSuccess: () -> Unit,
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

    fun addIRDeviceInfo(
        hub3: CHHub3,irCodes:List<CHHub3IRCode>, onSuccess: () -> Unit = {},
        onError: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val state = ""
            val irDeviceType = IRType.DEVICE_REMOTE_CUSTOM
            CHIRAPIManager.addIRDevice(
                hub3,
                irRemoteDeviceLiveData.value!!,
                state,
                irDeviceType,
                irCodes
            ) { it ->
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

    fun setCHHub3(chHub3: CHHub3) {
        this.chHub3= chHub3
    }

    fun getCHHub3() = chHub3

    fun setRemoteDevice(irRemote: IrRemote) {
        irRemoteDeviceLiveData.value = irRemote
    }

    fun getIrRemoteDevice(): IrRemote? {
        return if (addDeviceSuccess) {
            irRemoteDeviceLiveData.value!!
        } else {
            null
        }
    }

    fun emitIrCode(deviceId: String, code: String, irDeviceUUID: String, onSuccess: () -> Unit={}, onError: (message:String) -> Unit={}) {
        L.d("tag", "emitIRRemoteDeviceKey deviceId: $deviceId code: $code irDeviceUUID: $irDeviceUUID")
        CHIRAPIManager.emitIRRemoteDeviceKey(deviceId, learnedCommand = code, irDeviceUUID = irDeviceUUID) {
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

    override fun onCleared() {
        L.d(tag, "onCleared")
        super.onCleared()
        irCodeLiveData.value = mutableListOf()
    }

}