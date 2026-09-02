package co.candyhouse.app.tabs.devices.model

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.candyhouse.app.R
import co.candyhouse.app.ext.BotScriptStore
import co.candyhouse.app.ext.CHDeviceWrapperManager
import co.candyhouse.app.ext.aws.AWSStatus
import co.candyhouse.app.ext.userKey
import co.candyhouse.app.tabs.MainActivity
import co.candyhouse.app.tabs.devices.ssm2.chDeviceOrderComparator
import co.candyhouse.app.tabs.devices.ssm2.getIsNOHand
import co.candyhouse.app.tabs.devices.ssm2.getIsWidget
import co.candyhouse.app.tabs.devices.ssm2.getLevel
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.app.tabs.devices.ssm2.getOrderKey
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.devices.CHHub3Delegate
import co.candyhouse.sesame.open.devices.CHSesameBot2
import co.candyhouse.sesame.open.devices.CHWifiModule2Delegate
import co.candyhouse.sesame.open.devices.base.CHDeviceLoginStatus
import co.candyhouse.sesame.open.devices.base.CHDeviceStatus
import co.candyhouse.sesame.open.devices.base.CHDeviceStatusDelegate
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.open.devices.base.CHProductModel
import co.candyhouse.sesame.open.devices.base.CHSesameLock
import co.candyhouse.sesame.server.CHAPIClientBiz
import co.candyhouse.sesame.server.CHIotManagerPublic
import co.candyhouse.sesame.server.dto.BotScriptRequest
import co.candyhouse.sesame.server.dto.CHUserKey
import co.candyhouse.sesame.server.dto.cheyKeyToUserKey
import co.candyhouse.sesame.server.dto.ensureSafeStateInfo
import co.candyhouse.sesame.server.dto.userKeyToCHKey
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.CHResultState
import co.candyhouse.sesame.utils.Event
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.SharedPreferencesUtils
import co.candyhouse.sesame.utils.isInternetAvailable
import co.receiver.widget.AutoUnlockGeofenceManager
import co.receiver.widget.SesameWidgetNotificationManager
import co.utils.GuestUploadFlag
import co.utils.UserUtils
import co.utils.alertview.AlertView
import co.utils.alertview.enums.AlertStyle
import co.utils.getHistoryTag
import co.utils.isLockDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.Collections

class BeanDevices(val deviceId: String? = null)

data class LockDeviceStatus(var id: String, var model: Byte, var status: Byte)

class CHDeviceViewModel : ViewModel(), CHWifiModule2Delegate, CHDeviceStatusDelegate,
    CHHub3Delegate {

    private var syncJob: Job? = null
    val myChDevices = MutableStateFlow(ArrayList<CHDevices>())
    private val _neeRefresh = MutableLiveData<Event<BeanDevices>>()
    val neeRefresh: LiveData<Event<BeanDevices>> = _neeRefresh
    val ssmLockLiveData = MutableLiveData<CHDevices>()
    val ssmDeviceLiveDataForMatter = MutableLiveData<CHDevices>()
    private val delegateManager = DeviceViewModelDelegates(this)
    val ssmosLockDelegates = delegateManager.createSsmosLockDelegateObj()
    private val deviceStatusCallbacks = mutableMapOf<CHDevices, (CHDevices) -> Unit>()
    private val botScriptInitInFlight = Collections.synchronizedSet(mutableSetOf<String>())
    private val iotReconnectedListener = ::refreshDevices

    @Volatile
    private var isApplyingFullDeviceList = false

    // 搜索关键词
    val searchQuery = MutableStateFlow("")

    init {
        // IoT 重连成功后刷新服务端列表（含 stateInfo），对齐 iOS 重连后 getCHUserKeys
        CHIotManagerPublic.addOnReconnectedListener(iotReconnectedListener)
    }

    // 更新搜索关键词
    fun updateSearchQuery(query: String) {
        L.e("DeviceListFG", "updateSearchQuery $query")
        searchQuery.value = query
        notifyFullDeviceListChanged()
    }

    fun saveKeysToServer() {
        CHDeviceManager.getCandyDevices { it ->
            it.onSuccess { chResultState ->
                if (chResultState.data.isNotEmpty()) {
                    CHAPIClientBiz.upLoadKeys(chResultState.data.map {
                        cheyKeyToUserKey(it.getKey(), it.getLevel(), it.getNickname(), orderKey = it.getOrderKey())
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
                val serverUserKeys = result.data.toList()
                    .map { it.ensureSafeStateInfo() }
                CHDeviceWrapperManager.updateUserKeys(serverUserKeys)
                serverUserKeys.forEach { userKey ->
                    val deviceId = userKey.deviceUUID.lowercase()

                    SharedPreferencesUtils.preferences.edit {
                        putString(deviceId, userKey.deviceName)
                        putInt("l$deviceId", userKey.keyLevel)
                        userKey.rank?.let { putInt("ra$deviceId", it) }
                        // 本地 orderKey 与服务端保持一致：服务端没有则清掉本地残留，
                        // 否则会误判“已全有键”而跳过迁移、永不上传
                        val serverOrderKey = userKey.orderKey
                        if (serverOrderKey != null) putString("order_$deviceId", serverOrderKey)
                        else remove("order_$deviceId")
                    }

                    val scriptMetaMap = userKey.stateInfo.scriptList
                        .orEmpty()
                        .mapNotNull { item ->
                            val idx = item.actionIndex.toIntOrNull() ?: return@mapNotNull null
                            idx to BotScriptStore.ScriptMeta(
                                alias = item.alias,
                                displayOrder = item.displayOrder
                            )
                        }.toMap()

                    if (scriptMetaMap.isNotEmpty()) {
                        BotScriptStore.merge(userKey.deviceUUID, scriptMetaMap)
                    }
                }
                val devicesKeys = serverUserKeys.mapNotNull { userKey ->
                    try {
                        userKeyToCHKey(userKey, getHistoryTag())
                    } catch (e: IllegalArgumentException) {
                        L.d("UserKeyToCHKey", "Error converting userKey to CHKey: ${e.message}")
                        null
                    }
                }
                CHDeviceManager.receiveCHDeviceKeys(devicesKeys) { response ->
                    response.onSuccess { deviceResponse ->
                        // 用列表 stateInfo 直接写入设备状态，替代冷启动 IoT 快照（对齐 iOS）
                        val stateById = serverUserKeys.associateBy { it.deviceUUID.lowercase() }
                        deviceResponse.data.forEach { device ->
                            stateById[device.deviceId?.toString()?.lowercase()]?.let { userKey ->
                                CHDeviceManager.applyServerState(device, userKey.stateInfo)
                            }
                            CHDeviceWrapperManager.updateDevice(device)
                        }
                        viewModelScope.launch { migrateOrderKeysIfNeeded() }
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
        viewModelScope.launch {
            val isSignedIn = runCatching { AWSStatus.refreshAuthSessionNow() }
                .getOrElse { AWSStatus.getAWSLoginStatus() }
            if (isSignedIn) {
                syncDeviceFromServer()
            } else {
                refreshDevicesAsGuest()
            }
        }
    }

    private fun refreshDevicesAsGuest() {
        CHDeviceManager.getCandyDevices { result ->
            result.onFailure {
                _neeRefresh.postValue(Event(BeanDevices()))
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
                    it.getNickname(),
                    orderKey = it.getOrderKey()
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
                _neeRefresh.postValue(Event(BeanDevices()))
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

    /**
     * 恢复设备列表的状态监听代理（sharedDelegate）。
     * 当某个页面（如 webview 的 Hub3JSBridge）临时把自己注册为 ssmosLockDelegates[device] 后，
     * 退出时应调用此方法把代理恢复成列表用的 sharedDelegate，而不是直接 remove，
     * 否则列表收到 isRelayOn / mechStatus 变化时无法刷新界面。
     */
    fun restoreListStatusDelegate(device: CHDevices) {
        ssmosLockDelegates.remove(device)
        deviceStatusCallbacks[device]?.let { ssmosLockDelegates[device] = sharedDelegate }
    }


    private fun updateDevices(list: List<CHDevices>) {
        viewModelScope.launch {
            val updatedDevices = ArrayList(list).apply {
                sortWith(chDeviceOrderComparator)
            }
            synchronized(this@CHDeviceViewModel) {
                isApplyingFullDeviceList = true
                try {
                    updatedDevices.forEach { device ->
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
                    }
                    myChDevices.value = updatedDevices
                    synchronized(CHDeviceManager.listDevices) {
                        CHDeviceManager.listDevices.clear()
                        CHDeviceManager.listDevices.addAll(updatedDevices)
                    }
                } finally {
                    isApplyingFullDeviceList = false
                }
            }
            notifyFullDeviceListChanged()
            AutoUnlockGeofenceManager.sync(CHDeviceManager.app, updatedDevices)
            if (updatedDevices.any { it.getIsNOHand() } &&
                !AutoUnlockGeofenceManager.hasRequiredLocationPermission(CHDeviceManager.app)
            ) {
                MainActivity.activity?.requestAutoUnlockBackgroundPermissionIfNeeded()
            }
            CHIotManagerPublic.subscribeDevicesIfConnected(updatedDevices)
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

    fun handleAppGoToForeground(returnedFromBackground: Boolean) {
        CHIotManagerPublic.reconnectImmediatelyIfWaiting()
        if (returnedFromBackground) {
            refreshDevices()
        }
        viewModelScope.launch(Dispatchers.Main) {
            _neeRefresh.postValue(Event(BeanDevices()))
        }
    }

    fun updateNeeRefresh(device: CHDevices) {
        val deviceId = device.deviceId?.toString() ?: return
        updateNeeRefresh(deviceId)
    }

    fun updateNeeRefresh(deviceId: String) {
        if (deviceId.isBlank()) return

        if (isApplyingFullDeviceList) return

        val event = Event(BeanDevices(deviceId = deviceId))

        if (Looper.myLooper() == Looper.getMainLooper()) {
            _neeRefresh.value = event
        } else {
            _neeRefresh.postValue(event)
        }
    }

    private fun notifyFullDeviceListChanged() {
        val event = Event(BeanDevices())

        if (Looper.myLooper() == Looper.getMainLooper()) {
            _neeRefresh.value = event
        } else {
            _neeRefresh.postValue(event)
        }
    }

    suspend fun applyDeviceOrderFromUI(
        visibleOrderedDevices: List<CHDevices>,
        movedDeviceId: String? = null
    ): DeviceMoveResult {
        val currentAllDevicesSnapshot = ArrayList(myChDevices.value)
        val visibleOrderedSnapshot = ArrayList(visibleOrderedDevices)

        val newAllDevices = withContext(IO) {
            val currentIds = currentAllDevicesSnapshot.map { device ->
                device.deviceId?.toString()
            }

            val visibleIds = visibleOrderedSnapshot.map { device ->
                device.deviceId?.toString()
            }

            val hasInvalidVisibleId = visibleIds.any { it == null }
            val hasInvalidCurrentId = currentIds.any { it == null }

            val visibleNonNullIds = visibleIds.filterNotNull()
            val currentNonNullIds = currentIds.filterNotNull()

            val hasDuplicateVisibleId = visibleNonNullIds.size != visibleNonNullIds.toSet().size
            val hasDuplicateCurrentId = currentNonNullIds.size != currentNonNullIds.toSet().size

            val currentIdSet = currentNonNullIds.toSet()
            val hasUnknownVisibleId = visibleNonNullIds.any { it !in currentIdSet }

            val shouldFallback = hasInvalidVisibleId ||
                    hasInvalidCurrentId ||
                    hasDuplicateVisibleId ||
                    hasDuplicateCurrentId ||
                    hasUnknownVisibleId

            val rebuiltDevices = if (shouldFallback) {
                return@withContext ArrayList(currentAllDevicesSnapshot)
            } else {
                val isAllDevicesVisible =
                    visibleOrderedSnapshot.size == currentAllDevicesSnapshot.size

                if (isAllDevicesVisible) {
                    ArrayList(visibleOrderedSnapshot)
                } else {
                    val visibleIdSet = visibleNonNullIds.toSet()
                    val queue = ArrayDeque(visibleOrderedSnapshot)

                    ArrayList(
                        currentAllDevicesSnapshot.map { oldDevice ->
                            val id = oldDevice.deviceId?.toString()

                            if (id != null && id in visibleIdSet && queue.isNotEmpty()) {
                                queue.removeFirst()
                            } else {
                                oldDevice
                            }
                        }
                    )
                }
            }

            rebuiltDevices
        }

        // 找出本次唯一被移动的设备，只给它算新 orderKey（移动一项只更新一项，只上传一项）
        val oldIds = currentAllDevicesSnapshot.mapNotNull { it.deviceId?.toString() }
        val newIds = newAllDevices.mapNotNull { it.deviceId?.toString() }
        // 优先用拖动手势记录的被拖项；无则回退到 diff 猜测。顺序未变则不改动
        val movedId = if (oldIds == newIds) null
        else (movedDeviceId?.takeIf { it in newIds } ?: findMovedId(oldIds, newIds))
        var moved: CHDevices? = null
        var oldKey: String? = null
        var prevKey: String? = null
        var nextKey: String? = null
        if (movedId != null) {
            val idx = newAllDevices.indexOfFirst { it.deviceId?.toString() == movedId }
            if (idx >= 0) {
                moved = newAllDevices[idx]
                oldKey = moved.getOrderKey()
                // 取被移动项新相邻两项的 orderKey，交服务端在其间生成（不本地计算）
                prevKey = if (idx > 0) newAllDevices[idx - 1].getOrderKey() else null
                nextKey = if (idx < newAllDevices.size - 1) newAllDevices[idx + 1].getOrderKey() else null
            }
        }

        withContext(Dispatchers.Main) {
            myChDevices.value = newAllDevices
            notifyFullDeviceListChanged()
        }

        return DeviceMoveResult(newAllDevices, moved, oldKey, prevKey, nextKey)
    }

    data class DeviceMoveResult(
        val allOrdered: ArrayList<CHDevices>,
        val moved: CHDevices?,
        val oldKey: String?,
        val prevKey: String?,
        val nextKey: String?
    )

    // 单项移动检测：从两个全序列中剔除同一 id 后若相等，则该 id 即被移动项
    private fun findMovedId(old: List<String>, new: List<String>): String? {
        if (old.size != new.size || old == new) return null
        if (old.toHashSet() != new.toHashSet()) return null
        for (id in new) {
            if (old.filter { it != id } == new.filter { it != id }) return id
        }
        return null
    }

    // orderKey 迁移：本地标记只迁移一次；交服务端 merge 补键，成功后置标记并重拉刷新
    private suspend fun migrateOrderKeysIfNeeded() {
        val subId = UserUtils.getSubId() ?: ""
        val flagKey = "orderKeyMigrated_$subId"
        val prefs = SharedPreferencesUtils.preferences
        if (prefs.getBoolean(flagKey, false)) return
        CHAPIClientBiz.mergeDeviceOrder { result ->
            result.onSuccess {
                prefs.edit { putBoolean(flagKey, true) }
                refreshDevices()
            }
        }
    }

    // 拖动上传失败时：恢复被移动项的旧 orderKey，并按 orderKey 重新排序回滚 UI
    fun republishSortedDevices() {
        viewModelScope.launch(Dispatchers.Main) {
            val sorted = ArrayList(myChDevices.value).apply { sortWith(chDeviceOrderComparator) }
            myChDevices.value = sorted
            notifyFullDeviceListChanged()
        }
    }

    @SuppressLint("ServiceCast", "ImplicitSamInstance")
    fun updateWidgets(id: String? = null) {
        viewModelScope.launch(Dispatchers.Main) {
            synchronized(CHDeviceManager.listDevices) {
                CHDeviceManager.listDevices.clear()
                CHDeviceManager.listDevices.addAll(myChDevices.value)
                val isOpenWidget = CHDeviceManager.listDevices.any { it.getIsWidget() }
                if (isOpenWidget) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            CHDeviceManager.app,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        MainActivity.activity?.apply {
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                                201
                            )
                        }
                        return@synchronized
                    }
                    SesameWidgetNotificationManager.update(
                        CHDeviceManager.app,
                        CHDeviceManager.listDevices,
                        id
                    )
                } else {
                    SesameWidgetNotificationManager.cancelAll(
                        CHDeviceManager.app,
                        CHDeviceManager.listDevices
                    )
                }
            }
        }
    }

    fun updateAutoUnlock() {
        viewModelScope.launch(Dispatchers.Main) {
            val devices = myChDevices.value.toList()
            synchronized(CHDeviceManager.listDevices) {
                CHDeviceManager.listDevices.clear()
                CHDeviceManager.listDevices.addAll(devices)
            }
            AutoUnlockGeofenceManager.sync(CHDeviceManager.app, devices)
            updateWidgets()
        }
    }

    fun dropDevice(result: CHResult<CHEmpty>) {
        val targetDevice: CHDevices = ssmLockLiveData.value!!
        CHAPIClientBiz.removeKey(targetDevice.deviceId.toString()) {
            it.onSuccess {
                CHIotManagerPublic.unsubscribeDevice(targetDevice.deviceId.toString())
                myChDevices.value =
                    myChDevices.value.filter { device -> device.deviceId != targetDevice.deviceId } as ArrayList<CHDevices>
                _neeRefresh.postValue(Event(BeanDevices()))

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
                        CHIotManagerPublic.unsubscribeDevice(targetDevice.deviceId.toString())
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
        CHIotManagerPublic.removeOnReconnectedListener(iotReconnectedListener)
    }
}
