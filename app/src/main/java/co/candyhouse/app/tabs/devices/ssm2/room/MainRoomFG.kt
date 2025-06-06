package co.candyhouse.app.tabs.devices.ssm2.room

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.databinding.FgRoomMainBinding
import co.candyhouse.app.tabs.devices.model.bindLifecycle
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.sesame.open.CHResultState
import co.candyhouse.sesame.open.device.CHDeviceLoginStatus
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHDeviceStatusDelegate
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHSesame2
import co.candyhouse.sesame.open.device.CHSesame2History
import co.candyhouse.sesame.open.device.NSError
import co.candyhouse.sesame.utils.L
import co.utils.UserUtils
import co.utils.safeNavigate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.zakariya.stickyheaders.StickyHeaderLayoutManager
import java.text.SimpleDateFormat
import java.util.Locale

class MainRoomFG : BaseDeviceFG<FgRoomMainBinding>() {
    private var cursor: Long? = null
    private var responseCursor: Long? = null

    private var mHistorys = ArrayList<CHSesame2History>()
    private var mHistoryss = ArrayList<Pair<String, List<CHSesame2History>>>()
    private val mAdapter = SSMHistoryAdapter(mHistoryss)
    override fun getViewBinder() = FgRoomMainBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.rightIcon.setOnClickListener {
            safeNavigate(R.id.action_mainRoomFG_to_SSM2SettingFG)

        }
        bind.swiperefresh.isEnabled = false
        mDeviceModel.ssmLockLiveData.observe(viewLifecycleOwner) { ssm2 ->
            // Update the UI
            if (ssm2 is CHSesame2) {
                bind.menuTitle.text = ssm2.getNickname()
                bind.ssmView.setLock(ssm2)
                bind.ssmView.setLockImage(ssm2)
                bind.ssmView.setOnClickListener { ssm2.toggle() {} }
            }


        }
        bind.roomList.apply {
            layoutManager = StickyHeaderLayoutManager() // 第三方

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    if (newState == 0) {// (0：靜止, 1：手動滾動, 2：自動滾動)
                        val layoutManager =
                            (bind.roomList.layoutManager as StickyHeaderLayoutManager)
                        val firstVisibleItemPosition =
                            layoutManager.getFirstVisibleItemViewHolder(false)?.positionInSection
                        val firstAdapterPosition =
                            layoutManager.getFirstVisibleItemViewHolder(false)?.adapterPosition

                        if (firstAdapterPosition != null) {
                            //  firstVisibleItemPosition==0  top of section ,
                            //  firstAdapterPosition  total count count form top ,
                            if (firstVisibleItemPosition == 0 && firstAdapterPosition < 20) {
                                L.d(
                                    "bbtig",
                                    "firstVisibleItemPosition == 0 && firstAdapterPosition < 20"
                                )
                                //                                hispage += 1
                                if (responseCursor != null) {
                                    cursor = responseCursor
                                }
                                refleshHistory()
                            }
                        }
                    }
                }
            })
        }


        refleshHistory()
    }


    private fun refleshHistory() {
//        L.d("hcia", "[UI] refleshHistory:")
        bind.swiperefresh.post {
            bind.swiperefresh.isRefreshing = true
        }
        val device = mDeviceModel.ssmLockLiveData.value

        if (device is CHSesame2) {
            device.getHistories(cursor) {
                it.onSuccess {
                    var sesameHistorys = it.data.first
                    responseCursor = it.data.second
                    sesameHistorys = sesameHistorys.filter { his ->
                        his is CHSesame2History.WEBLock || his is CHSesame2History.WEBUnlock || his is CHSesame2History.AutoLock || his is CHSesame2History.WM2Unlock || his is CHSesame2History.WM2Lock || his is CHSesame2History.ManualLocked || his is CHSesame2History.ManualUnlocked || his is CHSesame2History.ManualElse || his is CHSesame2History.BLEUnlock || his is CHSesame2History.BLELock
                    }
                    if (sesameHistorys.isEmpty()) {
                        return@onSuccess
                    }
                    bind.roomList.post {

                        mHistorys.addAll(sesameHistorys)
                        mHistorys =
                            mHistorys.distinctBy { (it.date.time) } as ArrayList<CHSesame2History> // 去時間重複
                        mHistorys.sortBy { it.date.time }


                        val mTestGoupHistory = mHistorys.groupBy {
                            SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(it.date)
                        }
                        mAdapter.mGroupHistData.clear()
                        mAdapter.mGroupHistData.addAll(mTestGoupHistory.toList())
                        if (bind.roomList.adapter == null) {
                            bind.roomList.adapter = mAdapter
                            if (sesameHistorys.isNotEmpty()) {
                                bind.roomList.layoutManager?.scrollToPosition(bind.roomList.adapter!!.itemCount - 1)
                            }
                        }
                        mAdapter.notifyAllSectionsDataSetChanged()
                        if (it is CHResultState.CHResultStateBLE || cursor == null) {
                            L.d("bbtig", "it is CHResultState.CHResultStateBLE || cursor == null")
                            bind.roomList.layoutManager?.scrollToPosition(bind.roomList.adapter!!.itemCount - 1)
                        }
                    }

                }
                it.onFailure {
                    // 這裡是個workaround
                    // 理由:多人連線 sesame2 回 busy:7  notfound:5
                    // 策略:延遲網路請求等待隔壁連上的sesame2上傳完畢後拉取
                    if (it is NSError) {
                        if (it.code == 7 || it.code == 5) {
//                        hispage = 0
//                        refleshHistory()
                            lifecycleScope.launch {
//                        L.d("hcia", "🧤: -->status:" + status)
                                channel.send(true)
                            }
                        }
                    }
                }
                bind.swiperefresh.post {
                    bind.swiperefresh.isRefreshing = false
                }

            }
        }
        // 要撈幾頁資料

    }

    private val channel = Channel<Boolean>(1)

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            channel.consumeAsFlow().debounce(3500).collect {
                L.d("bbtig", "🧤: <---status:$it")
                cursor = null
                responseCursor = null
                refleshHistory()
            }
        }

        mDeviceModel.ssmosLockDelegates[mDeviceModel.ssmLockLiveData.value!!] =
            object : CHDeviceStatusDelegate {
                override fun onBleDeviceStatusChanged(
                    device: CHDevices,
                    status: CHDeviceStatus,
                    shadowStatus: CHDeviceStatus?
                ) {
                    if (device.deviceStatus == CHDeviceStatus.ReceivedAdV) {
                        device.connect {}
                    }
                    bind.ssmView?.setLockImage(mDeviceModel.ssmLockLiveData.value as CHSesame2)
                }

                override fun onMechStatus(device: CHDevices) {

                    if (mDeviceModel.ssmLockLiveData.value is CHSesame2) {
                        bind.ssmView?.setLock(mDeviceModel.ssmLockLiveData.value as CHSesame2)

                        if (device.deviceStatus.value == CHDeviceLoginStatus.UnLogin) {
                            lifecycleScope.launch {
                                channel.send(true)
                            }
                        }
                    }


                }
            }.bindLifecycle(viewLifecycleOwner)
    }

}
