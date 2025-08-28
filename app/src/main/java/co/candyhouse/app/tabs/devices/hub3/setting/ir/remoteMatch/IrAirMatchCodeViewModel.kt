package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteMatch

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.candyhouse.app.R
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrMatchRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IROperation
import co.candyhouse.server.CHIRAPIManager
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.open.device.CHWifiModule2NetWorkStatus
import co.candyhouse.sesame.utils.L
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID


class IrAirMatchCodeViewModel(val context: Context) :
    ViewModel() {
    private val tag = IrAirMatchCodeViewModel::class.java.simpleName
    private lateinit var hub3Device: CHHub3
    val irMatchRemoteListLiveData = MutableLiveData<List<IrMatchRemote>>()
    val originRemoteLiveData = MutableLiveData<IrRemote>()
    val connectStatusLiveData = MutableLiveData<Boolean>()
    val matchingLiveData = MutableLiveData<Boolean>()
    var isLearningMode = false
    fun setHub3Device(device: CHHub3) {
        this.hub3Device = device
    }

    fun isHub3DeviceInitialized(): Boolean {
        return ::hub3Device.isInitialized
    }

    fun setOriginRemote(irRemote: IrRemote) {
        this.originRemoteLiveData.value = irRemote
    }

    override fun onCleared() {
        super.onCleared()
        unsubscribeIR()
    }

    fun getCurrentIrDeviceType(): Int {
        return originRemoteLiveData.value!!.type
    }

    fun startAutoMatch() {
        isLearningMode = true
        startSubscribeIRMode()
        startSubscribeIR()
        enterLearnMode()
    }

    private fun startSubscribeIRMode() {
        hub3Device.subscribeTopic(getGetModeTopic()) { it ->
            it.onSuccess { data->
                if (!isLearningMode) {
                    return@onSuccess
                }
                val jsonString = String(data.data)
                val mode = extractValueWithJson(jsonString)
                mode?.let {
                    if (mode != IROperation.MODE_REGISTER) {
                        setModel(IROperation.MODE_REGISTER)
                    }
                } ?: run {
                    L.e(tag, "getIRMode failed: Invalid JSON format or missing 'ir_mode' key")
                }
            }
        }
        CHIRAPIManager.getIRMode(hub3Device.deviceId.toString().uppercase()) {
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

    private fun unsubscribeIRMode() {
        hub3Device.unsubscribeTopic(getGetModeTopic())
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun startSubscribeIR() {
        viewModelScope.launch {
            hub3Device.subscribeTopic(getLeaningDataTopic()) {
                it.onSuccess {
                    viewModelScope.launch(Dispatchers.Main) { matchingLiveData.value = true }
                    val callResult = CHIRAPIManager.matchIrCode(it.data, getCurrentIrDeviceType(), originRemoteLiveData.value!!.model!!) {
                        it.onSuccess { matchCodeResponse ->
                            startAutoMatch()
                            val irMatchCodeRequest = matchCodeResponse.data
                            val jsonString = Gson().toJson(irMatchCodeRequest)
                            val irMatchList = parseJsonToIrRemoteWithMatchList(jsonString, getCurrentIrDeviceType())
                            L.e(tag, "Success: matchIrCode , list.size=${irMatchList.size}")
                            viewModelScope.launch(Dispatchers.Main) {
                                irMatchRemoteListLiveData.value = irMatchList
                                matchingLiveData.value = false
                            }
                        }
                        it.onFailure { error ->
                            L.e(tag, "Error: matchIrCode : ${error.message}")
                            startAutoMatch()
                            viewModelScope.launch(Dispatchers.Main) {
                                irMatchRemoteListLiveData.value = emptyList()
                                matchingLiveData.value = false
                            }
                        }
                    }
                    if (!callResult) {
                        startAutoMatch()
                        viewModelScope.launch(Dispatchers.Main) {
                            Toast.makeText(context, R.string.ir_wave_too_short, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                it.onFailure { // 订阅失败，可能网络问题或设备未连接
                    L.e(tag, "Error: subscribe topic:${getLeaningDataTopic()}")
                    viewModelScope.launch(Dispatchers.Main) { connectStatusLiveData.value = false }
                }
            }
        }
    }

    fun enterLearnMode() {
        L.d(tag, "enterLearnMode")
        viewModelScope.launch(Dispatchers.Main) {
            if (!checkHub3DeviceStatus()) {
                connectStatusLiveData.value = false
                return@launch
            }
            connectStatusLiveData.value = true
            viewModelScope.launch(Dispatchers.IO) { setModel(IROperation.MODE_REGISTER) }
        }
    }

    fun setModel(model: Int) {
        CHIRAPIManager.setIRMode(model, hub3Device.deviceId.toString().uppercase()) {
            it.onFailure {
                viewModelScope.launch(Dispatchers.Main) {
                    Toast.makeText(context, "${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun exitMatchMode() {
        isLearningMode = false
        unsubscribeIR()
        unsubscribeIRMode()
        setModel(IROperation.MODE_CONTROL)
    }

    fun unsubscribeIR() {
        try {
            hub3Device.unsubscribeTopic(getLeaningDataTopic())
        } catch (e: Exception) {
            L.e(tag, "unsubscribeLearnData error: ${e.message}")
        }
    }

    fun parseJsonToIrRemoteWithMatchList(jsonString: String, type: Int = 0): List<IrMatchRemote> {
        return try {
            val gson = Gson()
            val jsonObject = gson.fromJson(jsonString, JsonObject::class.java)
            if (jsonObject.has("result")) {
                val resultArray = jsonObject.getAsJsonArray("result")
                if (resultArray == null || resultArray.size() == 0) {
                    return emptyList()
                }
                resultArray.map { element ->
                    val obj = element.asJsonObject
                    val controlType = obj.getAsJsonObject("controlType")
                    val model = controlType.get("model")?.asString
                    val alias = controlType.get("alias")?.asString ?: ""
                    val direction = controlType.get("direction")?.asString
                    val brandCode = obj.get("companyCode")?.asDouble?.toInt() ?: -1
                    val bestMatchPercentage = obj.get("bestMatchPercentage")?.asDouble ?: 0.0
                    val matchPercent = String.format("%.2f%%", bestMatchPercentage)
                    // 创建 IrRemote
                    val irRemote = IrRemote(
                        model = model,
                        alias = alias,
                        uuid = UUID.randomUUID().toString().uppercase(),
                        state = null,
                        timestamp = System.currentTimeMillis(),
                        type = type,
                        code = brandCode,
                        keys = null,
                        direction = direction,
                        haveSave = true
                    )
                    IrMatchRemote(irRemote, matchPercent)
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun setSearchRemoteList(remotes: List<IrMatchRemote>) {
        irMatchRemoteListLiveData.value = remotes
    }

    fun checkHub3DeviceStatus(): Boolean {
        return (hub3Device.mechStatus as? CHWifiModule2NetWorkStatus)?.isAPWork == true
    }

    private fun getLeaningDataTopic()  = "hub3/${hub3Device.deviceId.toString().uppercase()}/ir/learned/data"
    private fun getGetModeTopic()  = "hub3/${hub3Device.deviceId.toString().uppercase()}/ir/mode"
}


class IrMatchCodeViewModelFactory(val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IrAirMatchCodeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return IrAirMatchCodeViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}