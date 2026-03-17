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
import co.candyhouse.app.ext.BotScriptStore
import co.candyhouse.app.ext.CHDeviceWrapperManager
import co.candyhouse.app.ext.aws.AWSStatus
import co.candyhouse.app.ext.userKey
import co.candyhouse.app.tabs.MainActivity
import co.candyhouse.app.tabs.devices.ssm2.getIsWidget
import co.candyhouse.app.tabs.devices.ssm2.getLevel
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.app.tabs.devices.ssm2.getRank
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.device.CHDeviceLoginStatus
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHDeviceStatusDelegate
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHHub3Delegate
import co.candyhouse.sesame.open.device.CHProductModel
import co.candyhouse.sesame.open.device.CHSesameBot2
import co.candyhouse.sesame.open.device.CHSesameLock
import co.candyhouse.sesame.open.device.CHWifiModule2Delegate
import co.candyhouse.sesame.server.CHAPIClientBiz
import co.candyhouse.sesame.server.dto.BotScriptRequest
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
import co.utils.isLockDevice
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
import java.util.Collections

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

    private val botScriptInitInFlight = Collections.synchronizedSet(mutableSetOf<String>())

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
            viewModelScope.launch {
                CHDeviceWrapperManager.updateUserKeys(result.data.toList())

                result.data.forEach { userKey ->
                    val deviceId = userKey.deviceUUID.lowercase()

                    SharedPreferencesUtils.preferences.edit {
                        putString(deviceId, userKey.deviceName)
                        putInt("l$deviceId", userKey.keyLevel)
                        userKey.rank?.let { putInt("ra$deviceId", it) }
                    }

                    val scriptMetaMap = userKey.stateInfo.scriptList
                        ?.mapNotNull { item ->
                            val idx = item.actionIndex.toIntOrNull() ?: return@mapNotNull null
                            idx to BotScriptStore.ScriptMeta(
                                alias = item.alias,
                                displayOrder = item.displayOrder
                            )
                        }?.toMap()
                        ?: emptyMap()

                    if (scriptMetaMap.isNotEmpty()) {
                        BotScriptStore.merge(userKey.deviceUUID, scriptMetaMap)
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
                    listerChDeviceStatus(device) { changedDevice ->
                        updateNeeRefresh(changedDevice)

                        if (changedDevice is CHSesameBot2
                            && (changedDevice.productModel == CHProductModel.SesameBot2 || changedDevice.productModel == CHProductModel.SesameBot3)
                            && changedDevice.deviceStatus.value == CHDeviceLoginStatus.logined
                        ) {
                            L.d("CHDeviceViewModel", "发起SCRIPT_NAME_LIST")
                            changedDevice.getScriptNameList { r ->
                                r.onSuccess {
                                    val initKey = getBotScriptInitKey(changedDevice)
                                    val inited = SharedPreferencesUtils.preferences.getBoolean(initKey, false)
                                    if (!inited) {
                                        initBotScriptDefaults(changedDevice)
                                    }
                                    updateNeeRefresh(changedDevice)
                                }
                            }
                        }
                    }

                    // 拿到数据直接刷新
                    updateNeeRefresh(device)
                }
                if (myChDevices.value.isEmpty() && CHDeviceManager.isRefresh.get()) {
                    neeReflesh.postValue(BeanDevices(emptyList()))
                }
            }
        }
    }

    fun backgroundAutoConnect(device: CHDevices) {
        viewModelScope.launch(IO) {
            if (device.deviceStatus == CHDeviceStatus.ReceivedAdV && device.isLockDevice()) {
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
                clearBotScript(targetDevice)

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
                clearBotScript(targetDevice)
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

    fun initBotScriptDefaults(device: CHSesameBot2) {
        val deviceId = device.deviceId.toString()
        val initKey = getBotScriptInitKey(device)

        if (botScriptInitInFlight.contains(deviceId)) {
            return
        }
        botScriptInitInFlight.add(deviceId)

        val events = device.scripts.events
        if (events.isEmpty()) {
            botScriptInitInFlight.remove(deviceId)
            return
        }

        val deviceUUID = device.deviceId.toString()
        val bot2ScriptCurIndexKey = "${device.deviceId}_ScriptIndex"
        val currentIndex = SharedPreferencesUtils.preferences.getInt(bot2ScriptCurIndexKey, 0)

        val remoteScriptMap = device.userKey?.stateInfo?.scriptList
            ?.mapNotNull { item ->
                val idx = item.actionIndex.toIntOrNull() ?: return@mapNotNull null
                idx to item
            }?.toMap()
            ?: emptyMap()

        val metaMap = events.mapIndexed { index, _ ->
            val remote = remoteScriptMap[index]
            val finalAlias = if (!remote?.alias.isNullOrBlank()) remote.alias else "🎬 $index"
            val finalDisplayOrder = remote?.displayOrder ?: index

            index to BotScriptStore.ScriptMeta(
                alias = finalAlias,
                displayOrder = finalDisplayOrder
            )
        }.toMap()

        BotScriptStore.merge(deviceUUID, metaMap)

        var pendingCount = 0
        var finishedCount = 0
        var allSuccess = true

        events.forEachIndexed { index, _ ->
            val remote = remoteScriptMap[index]
            val needInitAlias = remote?.alias.isNullOrBlank()
            val needInitDisplayOrder = remote?.displayOrder == null
            val needInitIsDefault = remote?.isDefault == null

            if (!needInitAlias && !needInitDisplayOrder && !needInitIsDefault) {
                return@forEachIndexed
            }

            pendingCount++

            val req = BotScriptRequest(
                deviceUUID = deviceUUID.uppercase(),
                actionIndex = index.toString(),
                alias = if (needInitAlias) "🎬 $index" else null,
                isDefault = if (needInitIsDefault) {
                    if (index == currentIndex) 1 else 0
                } else null,
                actionData = null,
                displayOrder = if (needInitDisplayOrder) index else null,
                deleteAll = null
            )

            CHAPIClientBiz.updateBotScript(req) { result ->
                result.onSuccess {
                    finishedCount++
                    if (finishedCount == pendingCount) {
                        if (allSuccess) {
                            SharedPreferencesUtils.preferences.edit {
                                putBoolean(initKey, true)
                            }
                        }
                        botScriptInitInFlight.remove(deviceId)
                    }
                }

                result.onFailure {
                    allSuccess = false
                    finishedCount++
                    L.e("CHDeviceViewModel", "initBotScriptDefaults failed index=$index", it)

                    if (finishedCount == pendingCount) {
                        botScriptInitInFlight.remove(deviceId)
                    }
                }
            }
        }

        if (pendingCount == 0) {
            SharedPreferencesUtils.preferences.edit {
                putBoolean(initKey, true)
            }
            botScriptInitInFlight.remove(deviceId)
        }
    }

    fun forceInitBotScriptDefaults(device: CHSesameBot2) {
        val events = device.scripts.events
        if (events.isEmpty()) return

        val deviceUUID = device.deviceId.toString()
        val bot2ScriptCurIndexKey = "${device.deviceId}_ScriptIndex"
        val currentIndex = SharedPreferencesUtils.preferences.getInt(bot2ScriptCurIndexKey, 0)
        val initKey = getBotScriptInitKey(device)

        val metaMap = events.mapIndexed { index, _ ->
            index to BotScriptStore.ScriptMeta(
                alias = "🎬 $index",
                displayOrder = index
            )
        }.toMap()

        BotScriptStore.merge(deviceUUID, metaMap)

        var pendingCount = 0
        var successCount = 0

        events.forEachIndexed { index, _ ->
            pendingCount++

            val req = BotScriptRequest(
                deviceUUID = deviceUUID.uppercase(),
                actionIndex = index.toString(),
                alias = "🎬 $index",
                isDefault = if (index == currentIndex) 1 else 0,
                actionData = null,
                displayOrder = index,
                deleteAll = null
            )

            CHAPIClientBiz.updateBotScript(req) { result ->
                result.onSuccess {
                    successCount++
                    if (successCount == pendingCount) {
                        SharedPreferencesUtils.preferences.edit {
                            putBoolean(initKey, true)
                        }
                    }
                }
                result.onFailure {
                    L.e("CHDeviceViewModel", "forceInitBotScriptDefaults failed index=$index", it)
                }
            }
        }

        if (pendingCount == 0) {
            SharedPreferencesUtils.preferences.edit {
                putBoolean(initKey, true)
            }
        }
    }

    private fun getBotScriptInitKey(device: CHSesameBot2): String {
        return "${device.deviceId}_BotScriptInited"
    }

    fun clearBotScript(device: CHDevices) {
        if (device is CHSesameBot2 &&
            (device.productModel == CHProductModel.SesameBot2 || device.productModel == CHProductModel.SesameBot3)
        ) {
            val req = BotScriptRequest(
                deviceUUID = device.deviceId.toString().uppercase(),
                deleteAll = true
            )
            CHAPIClientBiz.updateBotScript(req) { result ->
                result.onSuccess {
                    L.d("clearBotScript", "clear bot script cloud data success")
                }
                result.onFailure {
                    L.e("clearBotScript", "clear bot script cloud data failed", it)
                }
            }

            BotScriptStore.clear(device.deviceId.toString())

            val bot2ScriptCurIndexKey = "${device.deviceId}_ScriptIndex"
            val botScriptInitKey = "${device.deviceId}_BotScriptInited"
            SharedPreferencesUtils.preferences.edit {
                remove(bot2ScriptCurIndexKey)
                remove(botScriptInitKey)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        syncJob?.cancel()
    }
}