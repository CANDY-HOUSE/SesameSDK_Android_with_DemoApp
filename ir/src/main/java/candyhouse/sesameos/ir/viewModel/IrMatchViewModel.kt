package candyhouse.sesameos.ir.viewModel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import candyhouse.sesameos.ir.base.IrCompanyCode
import candyhouse.sesameos.ir.base.IrRemote
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.uiBase.ConfigUpdateCallback
import candyhouse.sesameos.ir.domain.bizAdapter.air.ui.AirControllerConfigAdapter
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.handleBase.HandlerCallback
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.IRType
import candyhouse.sesameos.ir.domain.repository.RemoteRepository
import candyhouse.sesameos.ir.ext.IRDeviceType
import candyhouse.sesameos.ir.models.IrControlItem
import candyhouse.sesameos.ir.models.LayoutSettings
import candyhouse.sesameos.ir.models.UIControlConfig
import candyhouse.sesameos.ir.server.CHIRAPIManager
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.utils.L
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random


/**
 * 红外遥控器控制逻辑
 * add by wuying@cn.candyhouse.co
 */
class IrMatchViewModel(val context: Context, val remoteRepository: RemoteRepository) :
    ViewModel() {
    private val tag = "AirMatchViewModel"
    private val _uiState = MutableStateFlow<IrMatchUiState>(IrMatchUiState.Loading)
    private lateinit var device: CHHub3
    private val items: MutableList<IrControlItem> = mutableListOf()
    private var config: UIControlConfig? = null
    val irCompanyCodeLiveData = MutableLiveData<IrCompanyCode>()
    val irRemoteDeviceLiveData = MutableLiveData<IrRemote?>()
    val irMatchItemListLiveData = MutableLiveData<List<IrControlItem>>()
    private var currentMatchCodeIndex = 0

    fun setupMatchData(productKey: Int) {
        val handlerCallback = object : HandlerCallback {
            override fun onItemUpdate(item: IrControlItem) {
                if (config == null || items.isEmpty()) {
                    return
                }
                items.indexOfFirst { it.id == item.id }.takeIf { it != -1 }?.let {
                    items[it] = item
                }
                viewModelScope.launch(Dispatchers.Main) {
                    _uiState.value = IrMatchUiState.Success(
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
                    _uiState.value = IrMatchUiState.Success(
                        items = items,
                        layoutSettings = config!!.settings.layout,
                        timestamp = System.currentTimeMillis() * 10 + Random.nextInt(10)
                    )
                }
            }
        }
        remoteRepository.initialize(productKey, uiItemCallback, handlerCallback)
        loadInitialState()
        buildMatchItemList()
    }

    private fun buildMatchItemList() {
        val list = remoteRepository.getMatchUiItemList()
        viewModelScope.launch(Dispatchers.Main) {
            irMatchItemListLiveData.value = list
        }
    }

    fun getCurrentIrControlItem(position: Int): IrControlItem? {
        return remoteRepository.getMatchItem(position,items)
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            try {
                initMatchParams()
                config = remoteRepository.loadUIConfig()
                items.addAll(remoteRepository.loadUIParams())
                refreshAllItems(config!!)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = IrMatchUiState.Error(e)
            }
        }
    }

    private fun refreshAllItems(config: UIControlConfig) {
        viewModelScope.launch(Dispatchers.Main) {
            _uiState.value = IrMatchUiState.Success(
                items = items,
                layoutSettings = config.settings.layout,
                timestamp = System.currentTimeMillis() * 10 + Random.nextInt(10)
            )
        }
    }

    fun handleItemClick(item: IrControlItem, position: Int) {
        try {
            irCompanyCodeLiveData.value?.let {
                val currentItem = getCurrentIrControlItem(position)
                if (null == currentItem) {
                    L.d(tag,"ERROR! get item at position:${position}")
                    return
                }
                remoteRepository.handleItemClick(
                    currentItem, device, getCurrentRemoteDevice()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setDevice(device: CHHub3) {
        this.device = device
    }

    fun setIrCompanyCode(remoteDevice: IrCompanyCode) {
        L.d(tag, "setIrCompanyCode remoteDevice=${remoteDevice.code.toString()}")
        this.irCompanyCodeLiveData.value = remoteDevice
        remoteRepository.setCurrentSate("")
//        matchIrDeviceCode()
    }

    override fun onCleared() {
        super.onCleared()
        remoteRepository.clearConfigCache()
        remoteRepository.clearHandlerCache()
    }

    fun addIRDeviceInfo(name: String) {
        val remoteDevice = IrRemote(
            model = irCompanyCodeLiveData.value?.name,
            alias = name,
            uuid = UUID.randomUUID().toString().uppercase(),
            state = "",
            timestamp = System.currentTimeMillis(),
            type = IRDeviceType.DEVICE_REMOTE_AIR,
            code = irCompanyCodeLiveData.value?.code?.get(currentMatchCodeIndex + 1) ?: -1,
            keys = emptyArray(),
            direction = irCompanyCodeLiveData.value?.direction
        )
        viewModelScope.launch {
            val state = remoteRepository.getCurrentState(device, remoteDevice)
            val irDeviceType = remoteRepository.getCurrentIRDeviceType()
            CHIRAPIManager.addIRDevice(
                device, remoteDevice, state, irDeviceType, mutableListOf()
            ) { it ->
                viewModelScope.launch(Dispatchers.Main) {
                    it.onSuccess { result ->
                        L.d(tag, "addIRDeviceInfo success uuid=${remoteDevice.uuid}")
                        irRemoteDeviceLiveData.value = remoteDevice
                    }
                    it.onFailure {
                        L.d(tag, "addIRDeviceInfo fail!")
                        irRemoteDeviceLiveData.value = null
                        it.printStackTrace()
                    }
                }
            }
        }
    }

    fun getCurrentRemoteDevice(): IrRemote {
        return IrRemote(
            model = irCompanyCodeLiveData.value?.name,
            alias = irCompanyCodeLiveData.value?.name ?: "",
            uuid = "",
            state = "",
            timestamp = System.currentTimeMillis(),
            type = 0,
            code = irCompanyCodeLiveData.value?.code?.get(currentMatchCodeIndex + 1) ?: -1,
            keys = emptyArray(),
            direction = irCompanyCodeLiveData.value?.direction
        )
    }

    fun initMatchParams() {
        remoteRepository.initMatchParams()
    }

    fun matchNextCode() {
        if (currentMatchCodeIndex < getTotalCodeCounts() - 1) {
            currentMatchCodeIndex++
        }
    }

    fun getCurrentCodeIndex() = currentMatchCodeIndex

    fun getTotalCodeCounts(): Int {
        var totalSize = irCompanyCodeLiveData.value?.code?.size ?: 0
        if (totalSize > 1) {
            totalSize--
        }
        return totalSize
    }
}

// UiState
sealed class IrMatchUiState {
    object Loading : IrMatchUiState()
    data class Success(
        val items: List<IrControlItem>, val layoutSettings: LayoutSettings, val timestamp: Long
    ) : IrMatchUiState()

    data class Error(val exception: Throwable) : IrMatchUiState()
}


class IrMatchViewModelFactory(
    val context: Context, val remoteRepository: RemoteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IrMatchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return IrMatchViewModel(context, remoteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}