package co.candyhouse.app.tabs.devices.model

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import co.candyhouse.app.R
import co.candyhouse.app.ext.CHDeviceWrapperManager
import co.candyhouse.app.ext.aws.AWSStatus
import co.candyhouse.app.tabs.MainActivity
import co.candyhouse.app.tabs.devices.ssm2.getIsWidget
import co.candyhouse.app.tabs.devices.ssm2.getLevel
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.app.tabs.devices.ssm2.getRank
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHDeviceStatusDelegate
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHHub3Delegate
import co.candyhouse.sesame.open.device.CHSesameLock
import co.candyhouse.sesame.open.device.CHWifiModule2Delegate
import co.candyhouse.sesame.server.CHAPIClientBiz
import co.candyhouse.sesame.server.dto.CHUserKey
import co.candyhouse.sesame.server.dto.cheyKeyToUserKey
import co.candyhouse.sesame.server.dto.userKeyToCHKey
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.CHResultState
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.SharedPreferencesUtils
import co.candyhouse.sesame.utils.isInternetAvailable
import co.receiver.widget.SesameForegroundService
import co.receiver.widget.SesameReceiver
import co.utils.GuestUploadFlag
import co.utils.alertview.AlertView
import co.utils.alertview.enums.AlertStyle
import co.utils.getHistoryTag
import co.utils.isAutoConnect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.security.MessageDigest

class BeanDevices(
    val list: List<CHDevices>,
    val deviceId: String? = null
)

data class LockDeviceStatus(var id: String, var model: Byte, var status: Byte)

class CHDeviceViewModel : ViewModel(), CHWifiModule2Delegate, CHDeviceStatusDelegate,
    CHHub3Delegate {

    private var syncJob: Job? = null
    val myChDevices = MutableStateFlow(ArrayList<CHDevices>())
    var neeReflesh = MutableLiveData<BeanDevices>()
    val ssmLockLiveData = MutableLiveData<CHDevices>()
    val ssmDeviceLiveDataForMatter = MutableLiveData<CHDevices>()
    private val delegateManager = DeviceViewModelDelegates(this)
    val ssmosLockDelegates = delegateManager.createSsmosLockDelegateObj()
    private val deviceStatusCallbacks = mutableMapOf<CHDevices, (CHDevices) -> Unit>()

    // 搜索关键词
    val searchQuery = MutableStateFlow("")

    // 过滤后的设备列表
    val filteredDevices = combine(myChDevices, searchQuery) { devices, query ->
        if (query.isEmpty()) {
            devices
        } else {
            devices.filter { device ->
                // 根据名称和UUID过滤（忽略大小写）
                device.getNickname().contains(query, ignoreCase = true) || device.deviceId?.toString()?.contains(query, ignoreCase = true) == true
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = myChDevices.value
    )

    // 更新搜索关键词
    fun updateSearchQuery(query: String) {
        L.e("DeviceListFG", "updateSearchQuery $query")
        searchQuery.value = query
    }

    fun saveKeysToServer() {
        CHDeviceManager.getCandyDevices { it ->
            it.onSuccess { chResultState ->
                if (chResultState.data.isNotEmpty()) {
                    CHAPIClientBiz.upLoadKeys(chResultState.data.map {
                        cheyKeyToUserKey(it.getKey(), it.getLevel(), it.getNickname())
                    }) {
                        it.onFailure {
                            MainActivity.activity?.let { act ->
                                if (!act.isFinishing && !act.isDestroyed) {
                                    act.runOnUiThread {
                                        AlertView(
                                            act.getString(R.string.upload_keys_fail),
                                            "",
                                            AlertStyle.DIALOG
                                        ).apply {
                                            show(act as AppCompatActivity)
                                        }
                                    }
                                }
                            }
                        }
                        syncDeviceFromServer()
                    }
                } else {
                    syncDeviceFromServer()
                }
            }
        }
    }

    private fun syncDeviceFromServer() {
        syncJob?.cancel()

        syncJob = viewModelScope.launch {
            CHAPIClientBiz.getDevicesList {
                receiveKeysFromServer(it)
            }
        }
    }

    private fun receiveKeysFromServer(it: Result<CHResultState<Array<CHUserKey>>>) {
        it.onSuccess { result ->
            if (result.data.isEmpty()) {
                updateDevices()
                return@onSuccess
            }
            viewModelScope.launch {
                CHDeviceWrapperManager.updateUserKeys(result.data.toList())

                result.data.forEach { userKey ->
                    val deviceId = userKey.deviceUUID.lowercase()

                    SharedPreferencesUtils.preferences.edit {
                        putString(deviceId, userKey.deviceName)
                        putInt("l$deviceId", userKey.keyLevel)
                        userKey.rank?.let { putInt("ra$deviceId", it) }
                    }
                }
                val userks = result.data.mapNotNull { userKey ->
                    try {
                        userKeyToCHKey(userKey, getHistoryTag())
                    } catch (e: IllegalArgumentException) {
                        L.d("UserKeyToCHKey", "Error converting userKey to CHKey: ${e.message}")
                        null
                    }
                }
                CHDeviceManager.receiveCHDeviceKeys(userks) { response ->
                    response.onSuccess { deviceResponse ->
                        deviceResponse.data.forEach { device ->
                            CHDeviceWrapperManager.updateDevice(device)
                        }
                        updateDevices(deviceResponse.data)
                    }
                    response.onFailure {
                        updateDevices()
                    }
                }
            }
        }
        it.onFailure {
            updateDevices()
            L.e("receiveKeysFromServer", "onFailure ${it.message}")
        }
    }

    fun refreshDevices() {
        val isSignedIn = AWSStatus.getAWSLoginStatus()
        if (isSignedIn) {
            syncDeviceFromServer()
        } else {
            refreshDevicesAsGuest()
        }
    }

    private fun refreshDevicesAsGuest() {
        CHDeviceManager.getCandyDevices { result ->
            result.onFailure {
                neeReflesh.postValue(BeanDevices(emptyList()))
            }

            result.onSuccess { state ->
                val local = state.data
                val localFp = fingerprintOfDevices(local)
                val uploadedFp = GuestUploadFlag.getFingerprint()

                val hasInternet = isInternetAvailable()

                if (localFp == "empty") {
                    if (hasInternet) syncDeviceFromServer() else updateDevices(emptyList())
                    return@onSuccess
                }

                val needUpload = uploadedFp == null || uploadedFp != localFp
                if (needUpload) {
                    uploadLocalDevicesForGuest(local, localFp)
                    return@onSuccess
                }

                if (hasInternet) syncDeviceFromServer() else updateDevices(local)
            }
        }
    }

    private fun uploadLocalDevicesForGuest(local: List<CHDevices>, localFp: String) {
        CHAPIClientBiz.upLoadKeys(
            local.map {
                cheyKeyToUserKey(
                    it.getKey(),
                    it.getLevel(),
                    it.getNickname()
                )
            }
        ) { uploadResult ->

            uploadResult.onSuccess {
                GuestUploadFlag.setFingerprint(localFp)
            }

            if (isInternetAvailable()) {
                syncDeviceFromServer()
            } else {
                updateDevices(local)
            }
        }
    }

    private fun fingerprintOfDevices(devices: List<CHDevices>): String {
        val joined = devices
            .mapNotNull { it.deviceId?.toString()?.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
            .joinToString("|")

        if (joined.isEmpty()) return "empty"

        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(joined.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun updateDevices() {
        CHDeviceManager.getCandyDevices {
            it.onSuccess {
                updateDevices(it.data)
            }
            it.onFailure {
                neeReflesh.postValue(BeanDevices(emptyList()))
            }
        }
    }

    private val sharedDelegate = object : CHDeviceStatusDelegate {

        override fun onMechStatus(device: CHDevices) {
            CoroutineScope(Dispatchers.Main).launch {
                deviceStatusCallbacks[device]?.invoke(device)
            }
            L.d("onMechStatus", "onMechStatus3: ${device.mechStatus?.position}")
        }

        override fun onBleDeviceStatusChanged(
            device: CHDevices,
            status: CHDeviceStatus,
            shadowStatus: CHDeviceStatus?
        ) {
            CoroutineScope(Dispatchers.Main).launch {
                deviceStatusCallbacks[device]?.invoke(device)
            }
            L.d("onMechStatus", "onBleDeviceStatusChanged3")
        }
    }

    private fun listerChDeviceStatus(chDevices: CHDevices, call: (device: CHDevices) -> Unit) {
        deviceStatusCallbacks[chDevices] = call
        ssmosLockDelegates[chDevices] = sharedDelegate
    }

    private fun updateNeeRefresh(device: CHDevices) {
        MainScope().launch {
            neeReflesh.value = BeanDevices(myChDevices.value, device.deviceId.toString())
        }
    }

    private fun updateDevices(list: List<CHDevices>) {
        viewModelScope.launch {
            val updatedDevices = ArrayList(list).apply {
                sortWith(
                    compareBy(
                        { -it.getRank() },
                        { it.getNickname() }
                    )
                )
            }
            synchronized(this@CHDeviceViewModel) {
                myChDevices.value = updatedDevices
                myChDevices.value.forEach { device ->
                    device.delegate = delegateManager
                    // 锁、bike、bot自动连接蓝牙
                    backgroundAutoConnect(device)

                    // 监听器（设备状态变化时会触发）
                    listerChDeviceStatus(device) {
                        L.d("hcia", "刷新锁-ID=${device.deviceId}")
                        updateNeeRefresh(it)
                    }

                    // 拿到数据直接刷新
                    updateNeeRefresh(device)
                }
                if (myChDevices.value.isEmpty() && CHDeviceManager.isRefresh.get()) {
                    L.e("hcia", "下拉刷新，没有发现任何设备")
                    neeReflesh.postValue(BeanDevices(emptyList()))
                }
            }
        }
    }

    fun backgroundAutoConnect(device: CHDevices) {
        viewModelScope.launch(IO) {
            if (device.deviceStatus == CHDeviceStatus.ReceivedAdV && device.isAutoConnect()) {
                L.d("backgroundAutoConnect", "自动连接设备ID=${device.deviceId}")
                device.connect { }
            }
        }
    }

    fun handleAppGoToForeground() {
        viewModelScope.launch(Dispatchers.Main) {
            neeReflesh.postValue(BeanDevices(emptyList()))
        }
    }

    @SuppressLint("ServiceCast", "ImplicitSamInstance")
    fun updateWidgets(id: String? = null) {
        ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.Main) {
            synchronized(CHDeviceManager.listDevices) {
                CHDeviceManager.listDevices.clear()
                CHDeviceManager.listDevices.addAll(myChDevices.value)
                val isOpenWidget = CHDeviceManager.listDevices.any { it.getIsWidget() }
                if (isOpenWidget) {
                    if (ContextCompat.checkSelfPermission(
                            CHDeviceManager.app,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                        != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(
                            CHDeviceManager.app,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        MainActivity.activity?.apply {
                            ActivityCompat.requestPermissions(
                                this, arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                ), 201
                            )
                        }
                        return@synchronized
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        if (ContextCompat.checkSelfPermission(
                                CHDeviceManager.app,
                                Manifest.permission.FOREGROUND_SERVICE_LOCATION
                            )
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            MainActivity.activity?.apply {
                                ActivityCompat.requestPermissions(
                                    this,
                                    arrayOf(Manifest.permission.FOREGROUND_SERVICE_LOCATION),
                                    201
                                )
                            }
                            return@synchronized
                        }

                    }
                    // SesameForegroundService.isLive 避免重启前台服务
                    if (!SesameForegroundService.isLive) {
                        CHDeviceManager.app.sendBroadcast(
                            Intent(
                                CHDeviceManager.app,
                                SesameReceiver::class.java
                            ).apply {
                                action = SesameReceiver.SERVER_ACTION
                            })
                    } else {
                        CHDeviceManager.app.sendBroadcast(Intent(SesameForegroundService.aciton).apply {
                            putExtra(SesameForegroundService.acitonKey, id)
                        })
                    }
                } else {
                    if (SesameForegroundService.isLive) {
                        CHDeviceManager.app.stopService(
                            Intent(
                                CHDeviceManager.app,
                                SesameForegroundService::class.java
                            )
                        )
                    }
                }
            }
        }
    }

    fun dropDevice(result: CHResult<CHEmpty>) {
        val targetDevice: CHDevices = ssmLockLiveData.value!!
        CHAPIClientBiz.removeKey(targetDevice.deviceId.toString()) {
            it.onSuccess {
                myChDevices.value =
                    myChDevices.value.filter { device -> device.deviceId != targetDevice.deviceId } as ArrayList<CHDevices>
                neeReflesh.postValue(BeanDevices(emptyList()))

                unregisterNotification(targetDevice)

                viewModelScope.launch {
                    result.invoke(Result.success(CHResultState.CHResultStateNetworks(CHEmpty())))
                }
                targetDevice.dropKey {
                    it.onSuccess {
                        SharedPreferencesUtils.preferences.edit() {
                            remove(targetDevice.deviceId.toString())
                        }
                    }
                }
            }
            it.onFailure {
                viewModelScope.launch {
                    result.invoke(Result.failure(it))
                }
            }
        }
    }

    fun resetDevice(result: CHResult<CHEmpty>) {
        val targetDevice: CHDevices = ssmLockLiveData.value!!
        CHAPIClientBiz.removeKey(targetDevice.deviceId.toString()) {
            it.onSuccess {
                unregisterNotification(targetDevice)
                targetDevice.reset {
                    it.onSuccess {
                        refreshDevices()
                        viewModelScope.launch {
                            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
                        }
                    }
                    it.onFailure {
                        viewModelScope.launch {
                            result.invoke(Result.failure(it))
                        }
                    }
                }
            }
            it.onFailure {
                L.d("hcia", "it:$it")
            }
        }
    }

    fun unregisterNotification(chDevice: CHDevices) {
        SharedPreferencesUtils.deviceToken?.let { fcmToken ->
            (chDevice as? CHSesameLock)?.disableNotification(fcmToken) { result ->
                result.onSuccess {
                    L.d("sf", "result is $result")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        syncJob?.cancel()
    }
}