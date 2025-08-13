package candyhouse.sesameos.ir.viewModel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import candyhouse.sesameos.ir.base.IrCompanyCode
import candyhouse.sesameos.ir.base.IrMatchRemote
import candyhouse.sesameos.ir.base.IrRemote
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.uiBase.ConfigUpdateCallback
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.handleBase.HandlerCallback
import candyhouse.sesameos.ir.domain.repository.RemoteRepository
import candyhouse.sesameos.ir.models.IrControlItem
import candyhouse.sesameos.ir.models.LayoutSettings
import candyhouse.sesameos.ir.models.UIControlConfig
import candyhouse.sesameos.ir.server.CHIRAPIManager
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.utils.L
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random


/**
 * 红外遥控器控制逻辑
 * add by wuying@cn.candyhouse.co
 */
class AirControlViewModel(val context: Context, val remoteRepository: RemoteRepository) :
    ViewModel() {
    private val tag = "AirControlViewModel"
    private val _uiState = MutableStateFlow<AirControlUiState>(AirControlUiState.Loading)
    val uiState: StateFlow<AirControlUiState> = _uiState.asStateFlow()
    private lateinit var device: CHHub3
    private val items: MutableList<IrControlItem> = mutableListOf()
    private var config: UIControlConfig? = null
    val irRemoteDeviceLiveData = MutableLiveData<IrRemote>()
    private var currentCompanyCode : IrCompanyCode? = null
    private val _isFirstLoad = MutableStateFlow(true)
    val isFirstLoad: StateFlow<Boolean> = _isFirstLoad.asStateFlow()
    val irMatchRemoteListLiveData = MutableLiveData(emptyList<IrMatchRemote>())



    fun initConfig(type: Int) {
        val handlerCallback = object : HandlerCallback {
            override fun onItemUpdate(item: IrControlItem) {
                if (config == null || items.isEmpty()) {
                    return
                }
                items.indexOfFirst { it.id == item.id }.takeIf { it != -1 }?.let {
                    items[it] = item
                }
                viewModelScope.launch(Dispatchers.Main) {
                    _uiState.value = AirControlUiState.Success(
                        items = items,
                        layoutSettings = config!!.settings.layout,
                        timestamp = System.currentTimeMillis() * 10 + Random.nextInt(10)
                    )
                }
            }
        }

        val uiItemCallback = object : ConfigUpdateCallback {

            override fun onItemUpdate(item: IrControlItem) {
                if (config == null || items.isEmpty()) {
                    return
                }
                items.indexOfFirst { it.id == item.id }.takeIf { it != -1 }?.let {
                    items[it] = item
                }
                viewModelScope.launch(Dispatchers.Main) {
                    _uiState.value = AirControlUiState.Success(
                        items = items,
                        layoutSettings = config!!.settings.layout,
                        timestamp = System.currentTimeMillis() * 10 + Random.nextInt(10)
                    )
                }
            }
        }
        remoteRepository.initialize(type, uiItemCallback, handlerCallback)
        loadInitialState()
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            try {
                config = remoteRepository.loadUIConfig()
                items.addAll(remoteRepository.loadUIParams())
                refreshAllItems(config!!)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = AirControlUiState.Error(e)
            }
        }
    }

    private fun refreshAllItems(config: UIControlConfig) {
        viewModelScope.launch(Dispatchers.Main) {
            _uiState.value = AirControlUiState.Success(
                items = items,
                layoutSettings = config.settings.layout,
                timestamp = System.currentTimeMillis() * 10 + Random.nextInt(10)
            )
        }
    }

    fun handleItemClick(item: IrControlItem) {
        irRemoteDeviceLiveData.value?.let { remoteRepository.handleItemClick(item, device, it) }
    }

    fun setDevice(device: CHHub3) {
        this.device = device
    }

    fun setRemoteDevice(remoteDevice: IrRemote) {
        this.irRemoteDeviceLiveData.value = remoteDevice
        remoteRepository.setCurrentSate(remoteDevice.state)
        matchIrDeviceCode()
        tryFindCompanyCode()
    }

    override fun onCleared() {
        super.onCleared()
        remoteRepository.clearConfigCache()
        remoteRepository.clearHandlerCache()
    }

    fun addIRDeviceInfo(hub3: CHHub3, remoteDevice: IrRemote ,block: (isSuccess:Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val state = remoteRepository.getCurrentState( hub3, remoteDevice)
            val irDeviceType = remoteRepository.getCurrentIRDeviceType()
            CHIRAPIManager.addIRDevice(hub3,remoteDevice,state,irDeviceType, mutableListOf()){ it ->
                it.onSuccess { result ->
                    viewModelScope.launch(Dispatchers.Main) {
                        irRemoteDeviceLiveData.value!!.haveSave = true
                        block.invoke(true)
                    }
                    L.d(tag,"addIRDeviceInfo success uuid=${remoteDevice.uuid}  result=${result.toString()}")
                }
                it.onFailure {
                    viewModelScope.launch(Dispatchers.Main) {
                        block.invoke(false)
                    }
                    L.d(tag, "addIRDeviceInfo fail!")
                    it.printStackTrace()
                }
            }
        }
    }

    fun modifyRemoteIrDeviceInfo(alias: String) {
        viewModelScope.launch {
            irRemoteDeviceLiveData.value?.let { irRemoteDeviceValue ->
                val newIrRemote = irRemoteDeviceValue.clone()
                newIrRemote.alias = alias
                remoteRepository.modifyRemoteIrDeviceInfo(device,newIrRemote){
                    it.onSuccess { result ->
                        viewModelScope.launch(Dispatchers.Main) {
                            irRemoteDeviceLiveData.value = newIrRemote
                        }
                        L.d(tag, "modifyRemoteIrDeviceInfo success")
                    }
                    it.onFailure {
                        L.d(tag, "modifyRemoteIrDeviceInfo fail!")
                        it.printStackTrace()
                    }
                }
            }
        }
    }

    fun matchIrDeviceCode() {
        if (irRemoteDeviceLiveData.value == null) {
            return
        }
        viewModelScope.launch {
            irRemoteDeviceLiveData.value?.let { irRemoteDeviceValue ->
                remoteRepository.matchRemoteDevice(irRemoteDeviceLiveData.value!!)
            }
        }
    }

    fun getIrRemoteDevice(): IrRemote? {
       val irRemote = irRemoteDeviceLiveData.value
        irRemote?.let {
            it.state = remoteRepository.getCurrentState(device,it)
        }
        L.d(tag,"getIrRemoteDevice irRemote=${irRemote.toString()}")
        return irRemote
    }

    fun getDevice(): CHHub3 {
        return device
    }

    fun isDeviceInitialized(): Boolean {
        return ::device.isInitialized
    }

    fun getCompanyCode(): IrCompanyCode? = currentCompanyCode

    private fun tryFindCompanyCode() {
        if (currentCompanyCode != null ){
            return
        }
        if (null == this.irRemoteDeviceLiveData.value){
            return
        }
        viewModelScope.launch {
            val companyCodeList = remoteRepository.getCompanyCodeList(context)
            if (companyCodeList.isEmpty()) {
                return@launch
            }
            irRemoteDeviceLiveData.value?.let { irRemote ->
                companyCodeList.forEach({ item ->
                    if (irRemote.model?.uppercase()?.startsWith(item.name.uppercase()) == true) {
                        currentCompanyCode = item
                        L.d(tag, "get companyCode: currentCompanyCode ${currentCompanyCode.toString()}")
                        return@launch
                    }
                })

            }
        }
    }

    fun deleteIrDeviceInfo(device: CHHub3, irRemote: IrRemote) {
        viewModelScope.launch {
            CHIRAPIManager.deleteIRDevice(device.deviceId.toString().uppercase(),irRemote.uuid){
                it.onSuccess {
                    L.d(tag, "deleteIrDeviceInfo success")
                }
                it.onFailure {
                    L.d(tag, "deleteIrDeviceInfo fail!")
                    it.printStackTrace()
                }
            }
        }
    }

    fun markAsLoaded() {
        _isFirstLoad.value = false
    }

    fun setSearchRemoteList(list: List<IrMatchRemote>) {
        irMatchRemoteListLiveData.value = list
    }

    fun getSearchRemoteList():List<IrMatchRemote> {
        return irMatchRemoteListLiveData.value ?: emptyList()
    }
}

// UiState
sealed class AirControlUiState {
    object Loading : AirControlUiState()
    data class Success(
        val items: List<IrControlItem>,
        val layoutSettings: LayoutSettings,
        val timestamp: Long
    ) : AirControlUiState()

    data class Error(val exception: Throwable) : AirControlUiState()
}


class AirControlViewModelFactory(
    val context: Context,
    val remoteRepository: RemoteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AirControlViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AirControlViewModel(context, remoteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}