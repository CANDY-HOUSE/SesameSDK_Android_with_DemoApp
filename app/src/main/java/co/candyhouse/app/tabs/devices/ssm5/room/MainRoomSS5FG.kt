package co.candyhouse.app.tabs.devices.ssm5.room

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import candyhouse.sesameos.ir.ext.setDebouncedClick
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.databinding.FgRoomSs5MainBinding
import co.candyhouse.app.tabs.devices.model.bindLifecycle
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.toHexString
import co.utils.UserUtils
import co.utils.alertview.fragments.toastMSG
import co.utils.safeNavigate
import kotlinx.coroutines.*
import org.zakariya.stickyheaders.StickyHeaderLayoutManager
import java.text.SimpleDateFormat
import java.util.*
import co.candyhouse.app.ext.aws.AWSStatus
import co.candyhouse.sesame.ble.UUID4HistoryTag
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import co.candyhouse.sesame.open.CHResultState
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHDeviceStatusDelegate
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHSesame5
import co.candyhouse.sesame.open.device.CHSesame5History

class MainRoomSS5FG : BaseDeviceFG<FgRoomSs5MainBinding>() {
    private var cursor: Long? = null
    private var mHistorys = ArrayList<CHSesame5History>()
    private val mAdapter = SSM5HistoryAdapter(ArrayList<Pair<String, List<CHSesame5History>>>())
    private var isTest = false
    private var testCount = 0
    private var testStatus = "ok"
    private var delayms = 1000L
    override fun getViewBinder() = FgRoomSs5MainBinding.inflate(layoutInflater)

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind.rightIcon.setOnClickListener {
            safeNavigate(R.id.action_mainRoomSS5FG_to_SSM5SettingFG)
        }
        bind.menu.setOnLongClickListener {
            isTest = true
            if (isTest) {
                delayms += 1000L
                bind.menuTitle.text = "${mDeviceModel.ssmLockLiveData.value?.getNickname()},  ${delayms / 1000} s toggle"
            }
            true
        }
        mDeviceModel.ssmLockLiveData.observe(viewLifecycleOwner) { ssm5 ->
            // Update the UI
            if (ssm5 is CHSesame5) {
                bind.menuTitle.text = ssm5.getNickname()
                bind.ssmView.setLockImage(ssm5)
                bind.ssmView.setDebouncedClick(
                    debounceTime = 1000,
                    action = {
                        CHDeviceManager.vibrateDevice(requireContext())
                        ssm5.toggle(historytag = UserUtils.getUserIdWithByte()) {
                        }
                    })
            }
        }
        bind.swiperefresh.setOnRefreshListener { // 这里执行下拉刷新操作：比如请求网络数据
            refreshHistory()
        }
        bind.roomList.apply {
            layoutManager = StickyHeaderLayoutManager() // 第三方
        }
        refreshHistory()
    }


    private fun refreshHistory(isFromUser: Boolean = false) {
        showRefreshIndicator()
        val sesameDevice = mDeviceModel.ssmLockLiveData.value as? CHSesame5 ?: return
        val deviceId = sesameDevice.deviceId ?: return
        val subUUID = AWSStatus.getSubUUID()
        fetchHistoryFromDevice(sesameDevice, deviceId, subUUID, isFromUser)
    }

    private fun showRefreshIndicator() {
        bind.swiperefresh.post {
            bind.swiperefresh.isRefreshing = true
        }
    }

    private fun hideRefreshIndicator() {
        bind.swiperefresh.post {
            bind.swiperefresh.isRefreshing = false
        }
    }

    private fun fetchHistoryFromDevice(sesameDevice: CHSesame5, deviceId: UUID, subUUID: String?, isFromUser: Boolean) {
        sesameDevice.history(cursor, deviceId, subUUID) { result ->
            hideRefreshIndicator()
            result.onSuccess { historyResult ->
                val newHistories = historyResult.data.first
                if (newHistories.isEmpty() || !isAdded) return@onSuccess
                cursor = historyResult.data.second
                processHistories(newHistories, historyResult, isFromUser)
            }
            result.onFailure { error ->
                error.message?.let { message -> toastMSG(message) }
            }
        }
    }

    private fun processHistories(newHistories: List<CHSesame5History>, historyResult: CHResultState<Pair<List<CHSesame5History>, Long?>>, isFromUser: Boolean) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.Main) {
                    updateHistoryUI(newHistories, historyResult, isFromUser)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateHistoryUI(newHistories: List<CHSesame5History>, historyResult:  CHResultState<Pair<List<CHSesame5History>, Long?>>, isFromUser: Boolean) {
        logDebugInfo(newHistories)
        mergeAndSortHistories(newHistories)
        updateAdapterData()
        scrollToPosition(newHistories, historyResult, isFromUser)
    }
    private fun logDebugInfo(newHistories: List<CHSesame5History>) {
        if (BuildConfig.DEBUG) {
            for (i in mHistorys.indices) {
                L.d("uuid historyTag", "本地数据：$i ${mHistorys[i].date} ${mHistorys[i].recordID} ${mHistorys[i].historyTag?.toHexString()}")
            }
            for (i in newHistories.indices) {
                L.d("uuid historyTag", "服务器数据：$i ${newHistories[i].date} ${newHistories[i].recordID} ${newHistories[i].historyTag?.toHexString()}")
            }
        }
    }

    private fun mergeAndSortHistories(newHistories: List<CHSesame5History>) {
        // 添加新历史记录
        mHistorys.addAll(newHistories)
        if (BuildConfig.DEBUG) {
            for (i in mHistorys.indices) {
                L.d("uuid historyTag", "添加服务器数据后：$i ${mHistorys[i].date} ${mHistorys[i].recordID} ${mHistorys[i].historyTag?.toHexString()}")
            }
        }
        // 去重
        val historyMap = mHistorys.associateByTo(mutableMapOf()) { it.recordID }
        newHistories.forEach { newHistory ->
            historyMap[newHistory.recordID] = newHistory
        }
        mHistorys = ArrayList(historyMap.values)
        if (BuildConfig.DEBUG) {
            for (i in mHistorys.indices) {
                L.d("uuid historyTag", "本地去重后：$i ${mHistorys[i].date} ${mHistorys[i].recordID} ${mHistorys[i].historyTag?.toHexString()}")
            }
        }
        mHistorys.sortBy { it?.date?.time }
    }
    private fun updateAdapterData() {
        mAdapter.mGroupHistData.clear()
        mAdapter.mGroupHistData.addAll(mHistorys.groupBy {
            val date = it?.date
            if (date != null) {
                SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(date)
            } else {
                "Unknown Date"
            }
        }.toList())
        if (bind.roomList.adapter == null) {
            bind.roomList.adapter = mAdapter
        }

        // 通知数据变化
        mAdapter.notifyAllSectionsDataSetChanged()
    }

    // 滚动到适当位置
    private fun scrollToPosition(newHistories: List<CHSesame5History>, historyResult: CHResultState<Pair<List<CHSesame5History>, Long?>>, isFromUser: Boolean) {
        val layoutManager = bind.roomList.layoutManager ?: return

        if (bind.roomList.adapter == null && newHistories.isNotEmpty() ||
            historyResult is CHResultState.CHResultStateBLE ||
            isFromUser) {
            layoutManager.scrollToPosition(bind.roomList.adapter!!.itemCount - 1)
        }
    }

    override fun onResume() {
        super.onResume()
        val device = mDeviceModel.ssmLockLiveData.value
        if (device is CHSesame5) {
            mDeviceModel.ssmosLockDelegates[device] = object : CHDeviceStatusDelegate {
                override fun onBleDeviceStatusChanged(device: CHDevices, status: CHDeviceStatus, shadowStatus: CHDeviceStatus?) {
                    // L.d("hcia", "[ui][ss5][his] onBleDeviceStatusChanged:")
                    if (device.deviceStatus == CHDeviceStatus.ReceivedAdV) {
                        device.connect {}
                    }
                    bind.ssmView.setLock(device)
                    bind.ssmView.setLockImage(device)
                    if (isTest) {
                        if (device.deviceStatus == CHDeviceStatus.NoBleSignal) {
                            testStatus = "斷線"
                        }
                        if (device.deviceStatus == CHDeviceStatus.Locked) {
                            GlobalScope.launch {
                                delay(delayms)
                                device as CHSesame5
                                device.unlock(historytag = UserUtils.getUserIdWithByte()) { }
                                testCount++
                                testStatus = "ok"
                            }
                        }
                        if (device.deviceStatus == CHDeviceStatus.Unlocked) {
                            GlobalScope.launch {
                                delay(delayms)
                                device as CHSesame5
                                device.lock(historytag = UserUtils.getUserIdWithByte()) { }
                                testCount++
                                testStatus = "ok"
                            }
                        }
                    }
                }

                override fun onMechStatus(device: CHDevices) {
                    // L.d("hcia", "[ui][ss5][his] onMechStatusChanged:")
                    bind.ssmView.setLock(device)
                    bind.ssmView.setLockImage(device)
                    // L.d("postSSHistory", "onMechStatus")
                    if (device.mechStatus?.isStop == true) {
                        // L.d("hcia", "mDeviceModel.ssmLockLiveData.value?.deviceStatus?.value:" + mDeviceModel.ssmLockLiveData.value?.deviceStatus?.value)
                        // Hub3和手机同时蓝牙连线到同一台SS5上，历史记录可能会被Hub3先读走，并上传到云端服务器。 无论手机是否蓝牙连线SS5，都拉取历史记录，重复的历史记录用时间戳去重。
                        GlobalScope.launch {
                            delay(3000)
                            if (isAdded) {
                                cursor = (System.currentTimeMillis())  // 這裡是為了讓他拉取最新的資料，不然會拉到舊的資料。
                                L.d("harry", "cursor:$cursor")
                                refreshHistory(true)    // 一次拉15笔（在Pixel 6a上，大概是一页多点）；
                            }
                        }
                    }
                }
            }.bindLifecycle(viewLifecycleOwner)
        }
    }
}
