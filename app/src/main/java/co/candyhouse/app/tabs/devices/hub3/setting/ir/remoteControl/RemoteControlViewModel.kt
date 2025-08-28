package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrMatchRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrControlItem
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.LayoutSettings
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.UIControlConfig
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.handleBase.HandlerCallback
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.uiBase.ConfigUpdateCallback
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.repository.RemoteRepository
import co.candyhouse.server.CHIRAPIManager
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
class RemoteControlViewModel(val context: Context, val remoteRepository: RemoteRepository) :
    ViewModel() {
    private val tag = RemoteControlViewModel::class.java.simpleName
    private val _uiState = MutableStateFlow<AirControlUiState>(AirControlUiState.Loading)
    val uiState: StateFlow<AirControlUiState> = _uiState.asStateFlow()
    private lateinit var hub3Device: CHHub3
    private val items: MutableList<IrControlItem> = mutableListOf()
    private var config: UIControlConfig? = null
    val irRemoteDeviceLiveData = MutableLiveData<IrRemote>()
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
        irRemoteDeviceLiveData.value?.let { remoteRepository.handleItemClick(item, hub3Device, it) }
    }

    fun addIrDeviceToMatter(irRemote: IrRemote?, hub3: CHHub3){
        irRemoteDeviceLiveData.value?.let { remoteRepository.addIrDeviceToMatter(irRemote, hub3) }
    }

    fun setDevice(device: CHHub3) {
        this.hub3Device = device
    }

    fun setRemoteDevice(remoteDevice: IrRemote) {
        this.irRemoteDeviceLiveData.value = remoteDevice
        remoteRepository.setCurrentSate(remoteDevice.state)
    }

    override fun onCleared() {
        super.onCleared()
        remoteRepository.clearConfigCache()
        remoteRepository.clearHandlerCache()
    }

    fun addIRDeviceInfo(hub3: CHHub3, remoteDevice: IrRemote ,block: (isSuccess:Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val state = remoteRepository.getCurrentState( hub3, remoteDevice)
            val irDeviceType = remoteRepository.getCurrentIRType()
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
                remoteRepository.modifyRemoteIrDeviceInfo(hub3Device,newIrRemote){
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

    fun getIrRemoteDevice(): IrRemote? {
       val irRemote = irRemoteDeviceLiveData.value
        irRemote?.let {
            it.state = remoteRepository.getCurrentState(hub3Device,it)
        }
        L.d(tag,"getIrRemoteDevice irRemote=${irRemote.toString()}")
        return irRemote
    }

    fun getDevice(): CHHub3 {
        return hub3Device
    }

    fun isDeviceInitialized(): Boolean {
        return ::hub3Device.isInitialized
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


class RemoteControlViewModelFactory(
    val context: Context,
    val remoteRepository: RemoteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RemoteControlViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RemoteControlViewModel(context, remoteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}