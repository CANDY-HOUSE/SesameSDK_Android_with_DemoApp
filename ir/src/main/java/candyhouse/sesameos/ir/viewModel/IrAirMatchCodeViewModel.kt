package candyhouse.sesameos.ir.viewModel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import candyhouse.sesameos.ir.R
import candyhouse.sesameos.ir.base.IrCompanyCode
import candyhouse.sesameos.ir.base.IrMatchRemote
import candyhouse.sesameos.ir.base.IrRemote
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.IRType
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.handleBase.HandlerCallback
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.uiBase.ConfigUpdateCallback
import candyhouse.sesameos.ir.domain.repository.RemoteRepository
import candyhouse.sesameos.ir.models.IrControlItem
import candyhouse.sesameos.ir.server.CHIRAPIManager
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.utils.L
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID


class IrAirMatchCodeViewModel(val context: Context, val remoteRepository: RemoteRepository) :
    ViewModel() {
    private val tag = IrAirMatchCodeViewModel::class.java.simpleName
    private lateinit var hub3Device: CHHub3
    val irCompanyCodeLiveData = MutableLiveData<IrCompanyCode>()

    val irMatchRemoteListLiveData = MutableLiveData<List<IrMatchRemote>>()

    fun setupMatchData(productKey: Int) {
        val handlerCallback = object : HandlerCallback {
            override fun onItemUpdate(item: IrControlItem) {
            }
        }

        val uiItemCallback = object : ConfigUpdateCallback {

            override fun onItemUpdate(item: IrControlItem) {
            }
        }
        remoteRepository.initialize(productKey, uiItemCallback, handlerCallback)

    }

    fun setHub3Device(device: CHHub3) {
        this.hub3Device = device
    }

    fun isHub3DeviceInitialized(): Boolean {
        return ::hub3Device.isInitialized
    }

    fun setIrCompanyCode(remoteDevice: IrCompanyCode) {
        L.d(tag, "setIrCompanyCode remoteDevice=${remoteDevice.code.toString()}")
        this.irCompanyCodeLiveData.value = remoteDevice
        remoteRepository.setCurrentSate("")
//        matchIrDeviceCode()
    }

    override fun onCleared() {
        super.onCleared()
        unsubscribeIR()
        remoteRepository.clearConfigCache()
        remoteRepository.clearHandlerCache()
    }

    fun getCurrentIrDeviceType(): Int {
        return remoteRepository.getCurrentIRDeviceType()
    }

    fun isAirConditioner(): Boolean {
        return IRType.DEVICE_REMOTE_AIR == getCurrentIrDeviceType()
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun startSubscribeIR() {
        viewModelScope.launch {
            hub3Device.irModeSet(0x01) { }
            hub3Device.getIrLearnedData {
                it.onSuccess {
                    L.d(tag, "[IO][getLearnedData][OK]")
                    if (null == irCompanyCodeLiveData.value) {
                        L.e(tag, "irCompanyCodeLiveData is null, cannot match IR code")
                        irMatchRemoteListLiveData.value = emptyList()
                        return@onSuccess
                    }
                    L.d(tag, "getLearnedData ${getCurrentIrDeviceType()} ${irCompanyCodeLiveData.value!!.name} ")
                    val callResult = CHIRAPIManager.matchIrCode(it.data, getCurrentIrDeviceType(), irCompanyCodeLiveData.value!!.name) {
                        it.onSuccess { matchCodeResponse ->
                            L.d(tag, "matchIrCode success: ${matchCodeResponse.data}")
                            val irMatchCodeRequest = matchCodeResponse.data
                            val jsonString = Gson().toJson(irMatchCodeRequest)
                            val irMatchList = parseJsonToIrRemoteWithMatchList(jsonString, getCurrentIrDeviceType())
                            viewModelScope.launch(Dispatchers.Main) {
                                irMatchRemoteListLiveData.value = irMatchList
                            }
                        }
                        it.onFailure { error ->
                            L.e(tag, "matchIrCode error: ${error.message}")
                            viewModelScope.launch(Dispatchers.Main) {
                                irMatchRemoteListLiveData.value = emptyList()
                            }
                        }
                    }
                    if (!callResult) {
                        viewModelScope.launch(Dispatchers.Main) {
                            Toast.makeText(context, R.string.ir_wave_too_short, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        unsubscribeIR()
                    }
                }
                it.onFailure {
                    unsubscribeIR()
                }
            }
        }

    }

    fun stopSubscribeIR() {
        unsubscribeIR()
    }

    fun unsubscribeIR() {
        try {
            hub3Device.unsubscribeLearnData()
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
                    val companyCode = obj.get("companyCode")?.asDouble?.toInt() ?: -1
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
                        code = companyCode,
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
}


class IrMatchCodeViewModelFactory(
    val context: Context, val remoteRepository: RemoteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IrAirMatchCodeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return IrAirMatchCodeViewModel(context, remoteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}