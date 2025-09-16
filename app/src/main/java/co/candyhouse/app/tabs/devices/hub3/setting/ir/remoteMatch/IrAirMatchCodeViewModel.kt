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
import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.open.CHResultState
import co.candyhouse.sesame.server.IoTSubscriptionManager
import co.candyhouse.sesame.utils.L
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID


class IrAirMatchCodeViewModel(val context: Context) :
    ViewModel() {
    private val tag = IrAirMatchCodeViewModel::class.java.simpleName
    private var hub3DeviceId: String = ""
    val irMatchRemoteListLiveData = MutableLiveData<List<IrMatchRemote>>()
    val originRemoteLiveData = MutableLiveData<IrRemote>()
    val connectStatusLiveData = MutableLiveData<Boolean>()
    val matchingLiveData = MutableLiveData<Boolean>()
    var isLearningMode = false
    var hub3Mode = IROperation.MODE_CONTROL
    fun setHub3DeviceId(device: String) {
        this.hub3DeviceId = device
    }

    fun getHub3DeviceId(): String {
        return this.hub3DeviceId
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

    fun startSubscribeIRMode() {
        subscribeTopic(getGetModeTopic()) { it ->
            it.onSuccess { data->

                val jsonString = String(data.data)
                val mode = extractValueWithJson(jsonString)
                mode?.let {
                    hub3Mode = mode
                    if (hub3Mode != IROperation.MODE_REGISTER) {
                        viewModelScope.launch(Dispatchers.IO) {
                            delay(500) // 延迟500毫秒，防止干扰信息导致过快切换模式
                            if (isLearningMode && hub3Mode != IROperation.MODE_REGISTER) {
                                setModel(IROperation.MODE_REGISTER)
                            }
                        }

                    }
                } ?: run {
                    L.e(tag, "getIRMode failed: Invalid JSON format or missing 'ir_mode' key")
                }
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

    private fun unsubscribeIRMode() {
        try {
            unsubscribeTopic(getGetModeTopic())
        } catch (e: Exception) {
            L.e(tag, "unsubscribeIRMode error: ${e.message}")
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun startSubscribeIR() {
        viewModelScope.launch {
            subscribeTopic(getLeaningDataTopic()) {
                it.onSuccess {
                    viewModelScope.launch(Dispatchers.Main) { matchingLiveData.value = true }
                    CHIRAPIManager.matchIrCode(it.data, getCurrentIrDeviceType(), originRemoteLiveData.value!!.model!!) {
                        it.onSuccess { matchCodeResponse ->
                            startAutoMatch()
                            val irMatchCodeRequest = matchCodeResponse.data
                            val jsonString = Gson().toJson(irMatchCodeRequest)
                            val irMatchList = parseJsonToIrRemoteWithMatchList(jsonString, getCurrentIrDeviceType())
                            viewModelScope.launch(Dispatchers.Main) {
                                irMatchRemoteListLiveData.value = irMatchList
                                matchingLiveData.value = false
                            }
                        }
                        it.onFailure { error ->
                            startAutoMatch()
                            viewModelScope.launch(Dispatchers.Main) {
                                irMatchRemoteListLiveData.value = emptyList()
                                matchingLiveData.value = false
                            }
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
            connectStatusLiveData.value = true
            viewModelScope.launch(Dispatchers.IO) { setModel(IROperation.MODE_REGISTER) }
        }
    }

    fun setModel(model: Int) {
        CHIRAPIManager.setIRMode(model, hub3DeviceId) {
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
            unsubscribeTopic(getLeaningDataTopic())
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


class IrMatchCodeViewModelFactory(val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IrAirMatchCodeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return IrAirMatchCodeViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}