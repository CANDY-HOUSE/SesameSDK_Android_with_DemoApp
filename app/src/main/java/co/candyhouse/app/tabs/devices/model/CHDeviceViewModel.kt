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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.candyhouse.app.R
import co.candyhouse.app.ext.aws.AWSStatus
import co.candyhouse.app.tabs.MainActivity
import co.candyhouse.app.tabs.account.cheyKeyToUserKey
import co.candyhouse.app.tabs.account.getHistoryTag
import co.candyhouse.app.tabs.account.userKeyToCHKey
import co.candyhouse.app.tabs.devices.hub3.bean.IrRemoteRepository
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import co.candyhouse.app.tabs.devices.ssm2.getIsWidget
import co.candyhouse.app.tabs.devices.ssm2.getLevel
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.app.tabs.devices.ssm2.getRank
import co.candyhouse.app.tabs.devices.ssm2.uiPriority
import co.candyhouse.server.CHIRAPIManager
import co.candyhouse.server.CHLoginAPIManager
import co.candyhouse.server.CHResult
import co.candyhouse.server.CHResultState
import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.CHScanStatus
import co.candyhouse.sesame.open.device.CHDeviceLoginStatus
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHDeviceStatusDelegate
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.open.device.CHHub3Delegate
import co.candyhouse.sesame.open.device.CHSesameConnector
import co.candyhouse.sesame.open.device.CHWifiModule2
import co.candyhouse.sesame.open.device.CHWifiModule2Delegate
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.server.dto.CHUserKey
import co.candyhouse.sesame.utils.L
import co.receiver.widget.SesameForegroundService
import co.receiver.widget.SesameReceiver
import co.utils.JsonUtil
import co.utils.JsonUtil.parseList
import co.utils.SharedPreferencesUtils
import co.utils.alertview.AlertView
import co.utils.alertview.enums.AlertStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BeanDevices(
    val list: List<CHDevices>,
    val deviceId: String? = null
)

data class LockDeviceStatus(var id: String, var model: Byte, var status: Byte)

class CHDeviceViewModel : ViewModel(), CHWifiModule2Delegate, CHDeviceStatusDelegate,
    CHHub3Delegate {

    var targetShareLevel: Int = 0
    var guestKeyId: String? = null
    private var userKeys: ArrayList<String> = ArrayList()
    val myChDevices = MutableStateFlow(ArrayList<CHDevices>())
    var neeReflesh = MutableLiveData<BeanDevices>()
    val ssmLockLiveData = MutableLiveData<CHDevices>()
    val ssmDeviceLiveDataForMatter = MutableLiveData<CHDevices>()
    private val delegateManager = DeviceViewModelDelegates(this)
    val ssmosLockDelegates = delegateManager.createSsmosLockDelegateObj()
    private val deviceStatusCallbacks = mutableMapOf<CHDevices, (CHDevices) -> Unit>()
    private val iRRepository = IrRemoteRepository()

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
        L.e("DeviceListFG","updateSearchQuery $query")
        searchQuery.value = query
    }

    fun saveKeysToServer() {
        CHDeviceManager.getCandyDevices { it ->
            it.onSuccess { chResultState ->
                if (chResultState.data.isNotEmpty()) {
                    CHLoginAPIManager.upLoadKeys(chResultState.data.map {
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
        CHLoginAPIManager.getDevicesList {
            receiveKeysFromServer(it)
            SharedPreferencesUtils.isNeedFreshDevice = false
        }
    }

    private fun receiveKeysFromServer(it: Result<CHResultState<Array<CHUserKey>>>) {
        it.onSuccess { result ->
            userKeys.clear()
            userKeys.addAll(result.data.map { it.deviceUUID.lowercase() })

            viewModelScope.launch {
                val userKeyMap = result.data.associateBy { it.deviceUUID.lowercase() }

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
                            val deviceId = device.deviceId?.toString()?.lowercase()
                            deviceId?.let { id ->
                                device.userKey = userKeyMap[id]
                            }
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

    fun refleshDevices() {
        val status = AWSStatus.getAWSLoginStatus()
        L.d("sf", "👘 refleshDevices islogin:$status")
        if (status) {
            syncDeviceFromServer()
        } else {
            updateDevices()
        }
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

    // 在主线程里使用 setValue 方法更新 LiveData； 防止数据竞争， 快速post时会丢失数据， 修复bug:
    // 【ID1001306】【Android】【app】关掉手机蓝牙，设备列表中的Sesame设备蓝牙图标不会自动置灰显示或置灰速度慢,需手动刷新才置灰(ios端正常)
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
                        { -1 * it.getRank() },
                        { it.uiPriority() },
                        { it.getNickname() })
                )
            }
            val filteredDevices = if (userKeys.isNotEmpty()) {
                updatedDevices.filter { device ->
                    userKeys.contains(device.deviceId.toString().lowercase()).also { isInUserKeys ->
                        if (!isInUserKeys) {
                            device.dropKey { }
                        }
                    }
                }
            } else {
                updatedDevices
            }
            synchronized(this@CHDeviceViewModel) {
                userKeys.clear()
                myChDevices.value = ArrayList(filteredDevices)
                myChDevices.value.forEach { device ->
                    device.delegate = delegateManager
                    backgroundAutoConnect(device)

                    // 红外设备
                    if (device is CHHub3) {
                        L.d("sf", "fetchIRDevices...")
                        fetchIRDevices(device)
                    }

                    // 设备列表更新
                    listerChDeviceStatus(device) {
                        updateNeeRefresh(it)
                    }

                    // 下拉刷新，更新界面
                    if (CHDeviceManager.isRefresh.get()) {
                        L.e("sf", "下拉刷新，设备ID=${device.deviceId}")
                        updateNeeRefresh(device)
                    }
                }
                // 下拉刷新增加判断，如果没有设备的处理
                if (myChDevices.value.isEmpty() && CHDeviceManager.isRefresh.get()) {
                    L.e("sf", "下拉刷新，没有发现任何设备")
                    neeReflesh.postValue(BeanDevices(emptyList()))
                }
            }
        }
    }

    private fun fetchIRDevices(device: CHHub3) {
        val uuid = device.deviceId.toString().uppercase(Locale.getDefault())
        CHIRAPIManager.fetchIRDevices(uuid) { it ->
            it.onSuccess { result ->
                L.d("sf", "data==== " + result.data.toString())
                val jsonString = JsonUtil.toJson(result.data)
                val tempList = jsonString.parseList<IrRemote>()
                viewModelScope.launch {
                    L.d("sf", "保存红外遥控器列表数据……")
                    iRRepository.setRemotes(uuid, tempList)
                    updateNeeRefresh(device)
                }
            }
            it.onFailure {
                L.d("sf", "result==== onFailure ${it.message}")
            }
        }
    }

    private val _channel = Channel<Int>()
    val channel = _channel.receiveAsFlow()
    fun deleteIRDevice(uuid: String, subid: String, type: Int) {
        CHIRAPIManager.deleteIRDevice(uuid, subid) { it ->
            it.onSuccess { result ->
                L.d("sf", "data==== " + result.data.toString())
                viewModelScope.launch {
                    // 删除成功，发送成功消息
                    _channel.send(type)
                }
            }
            it.onFailure {
                L.d("sf", "result==== onFailure ${it.message}")
            }
        }
    }

    fun getIrRemoteList(key: String): List<IrRemote> {
        L.d("sf", "获取红外线列表数据……")
        return iRRepository.getRemotesByKey(key)
    }

    suspend fun getHub3Data(uuid: String): Result<List<IrRemote>> {
        return withContext(IO) {
            try {
                // 先执行网络请求
                val fetchResult = suspendCoroutine { continuation ->
                    CHIRAPIManager.fetchIRDevices(uuid) { result ->
                        result.onSuccess { data ->
                            L.d("sf", "data==== ${data.data}")

                            val jsonString = JsonUtil.toJson(data.data)
                            val tempList = jsonString.parseList<IrRemote>()

                            viewModelScope.launch {
                                L.d("sf", "保存红外遥控器列表数据……")
                                iRRepository.setRemotes(uuid, tempList)
                                continuation.resume(true)
                            }
                        }
                        result.onFailure { error ->
                            L.d("sf", "result==== onFailure ${error.message}")
                            continuation.resume(false)
                        }
                    }
                }

                // 等待网络请求和数据保存完成后，再获取列表
                if (fetchResult) {
                    val result = getIrRemoteList(uuid)
                    Result.success(result)
                } else {
                    Result.failure(Exception("Fetch failed"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun backgroundAutoConnect(device: CHDevices) {// 根據不同設備判斷要不要自動斷線連線
        GlobalScope.launch(IO) { // 在 IO 调度器上启动新协程
            if (device.deviceStatus == CHDeviceStatus.ReceivedAdV) {
                if (device !is CHSesameConnector && device !is CHWifiModule2) {
                    L.d("backgroundAutoConnect", Thread.currentThread().name)
                    // 自动重连
                    device.connect { }
                }
            }
        }
    }

    fun handleAppGoToForeground() {
        GlobalScope.launch(Dispatchers.Main) {
            neeReflesh.postValue(BeanDevices(emptyList()))
        }
    }

    fun handleAppGoToBackground() {
        CHBleManager.mScanning = CHScanStatus.BleClose
        myChDevices.value.forEach {
            if (it.deviceStatus.value == CHDeviceLoginStatus.Login) {
                it.disconnect { }
            }
        }
    }

    @SuppressLint("ServiceCast", "ImplicitSamInstance")
    fun updateWidgets(id: String? = null) {
        GlobalScope.launch(Dispatchers.Main) {
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
        if (AWSStatus.getAWSLoginStatus()) {
            CHLoginAPIManager.removeKey(targetDevice.deviceId.toString()) {
                it.onSuccess {
                    myChDevices.value =
                        myChDevices.value.filter { device -> device.deviceId != targetDevice.deviceId } as ArrayList<CHDevices>
                    neeReflesh.postValue(BeanDevices(emptyList()))

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
        } else {
            targetDevice.dropKey {
                it.onSuccess {
                    refleshDevices()
                    SharedPreferencesUtils.preferences.edit() {
                        remove(targetDevice.deviceId.toString())
                    }
                    viewModelScope.launch {
                        result.invoke(Result.success(CHResultState.CHResultStateBle(CHEmpty())))
                    }
                }
                it.onFailure {
                    viewModelScope.launch {
                        result.invoke(Result.failure(it))
                    }
                }
            }
        }
    }

    fun resetDevice(result: CHResult<CHEmpty>) {
        val targetDevice: CHDevices = ssmLockLiveData.value!!

        if (AWSStatus.getAWSLoginStatus()) {
            // L.d("hcia", "登入刪除:")
            CHLoginAPIManager.removeKey(targetDevice.deviceId.toString()) {
                it.onSuccess {
                    targetDevice.reset {
                        it.onSuccess {
                            refleshDevices()
                            viewModelScope.launch {
                                result.invoke(Result.success(CHResultState.CHResultStateBle(CHEmpty())))
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
        } else {
            L.d("hcia", "未登入reset")
            targetDevice.reset {
                it.onSuccess {
                    refleshDevices()
                    viewModelScope.launch {
                        result.invoke(Result.success(CHResultState.CHResultStateBle(CHEmpty())))
                    }
                }
                it.onFailure {
                    viewModelScope.launch {
                        result.invoke(Result.failure(it))
                    }
                }
            }
        }
    }

    fun updateHub3IrDevice(irRemote: IrRemote, chDeviceId: String) {
        val localIrRemotes = iRRepository.getRemotesByKey(chDeviceId)
        localIrRemotes.let {
            val index = it.indexOfFirst { it.uuid == irRemote.uuid }
            if (index != -1) {
                it[index].state = irRemote.state
                iRRepository.setRemotes(chDeviceId, it)
            }
        }
    }

}