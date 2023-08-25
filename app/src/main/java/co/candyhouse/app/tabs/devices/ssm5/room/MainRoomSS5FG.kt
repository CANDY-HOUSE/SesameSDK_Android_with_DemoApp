package co.candyhouse.app.tabs.devices.ssm5.room

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.sesame.open.CHResultState
import co.candyhouse.sesame.open.device.*
import co.utils.L
import kotlinx.android.synthetic.main.fg_room_ss5_main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.zakariya.stickyheaders.StickyHeaderLayoutManager
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainRoomSS5FG : BaseDeviceFG(R.layout.fg_room_ss5_main) {
    private var cursor: Long? = null
    private var responseCursor: Long? = null

    private var mHistorys = ArrayList<CHSesame5History>()
    private var mHistoryss = ArrayList<Pair<String, List<CHSesame5History>>>()
    private val mAdapter = SSM5HistoryAdapter(mHistoryss)
    private var isTest = false
    private var testCount = 0
    private var testStatus = "ok"
    private var delayms = 1000L

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        right_icon.setOnClickListener { findNavController().navigate(R.id.action_mainRoomSS5FG_to_SSM5SettingFG) }

        menu.setOnLongClickListener {
            isTest = true
            if (isTest) {
                delayms = delayms + 1000L;
                menu_title.text = "${mDeviceModel.ssmLockLiveData.value?.getNickname()},  ${delayms / 1000} s toggle"

            }
            true
        }

        swiperefresh.isEnabled = false
        mDeviceModel.ssmLockLiveData.observe(viewLifecycleOwner) { ssm2 ->
            // Update the UI
            ssm2 as CHSesame5
            menu_title.text = ssm2.getNickname()
            ssmView.setLockImage(ssm2)
            ssmView.setOnClickListener {
                ssm2.toggle {
//                    GlobalScope.launch {
//                        delay(6000)
//                        GlobalScope.launch(Dispatchers.Main) {
//                            room_list?.layoutManager?.scrollToPosition(room_list?.adapter!!.getItemCount() - 1)
//                        }
//                    }
                }
            }
        }
        room_list?.apply {
            layoutManager = StickyHeaderLayoutManager() // 第三方

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {}

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    if (newState == SCROLL_STATE_IDLE) {// (0：靜止, 1：手動滾動, 2：自動滾動)
                        val layoutManager = (room_list?.layoutManager as StickyHeaderLayoutManager)
                        val firstVisibleItemPosition = layoutManager.getFirstVisibleItemViewHolder(false)?.positionInSection
                        val firstAdapterPosition = layoutManager.getFirstVisibleItemViewHolder(false)?.adapterPosition
                        if (firstAdapterPosition != null) {
                            //  firstVisibleItemPosition==0  top of section ,
                            //  firstAdapterPosition  total count count form top ,
                            if (firstVisibleItemPosition == 0 && firstAdapterPosition < 20) {
//                                L.d("hcia", "[ss5] firstVisibleItemPosition == 0 && firstAdapterPosition < 20")
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


    private fun refleshHistory(isFromuser: Boolean = false) {
        swiperefresh?.post {
            swiperefresh?.isRefreshing = false
        }
        val currentDeviceUUID = (mDeviceModel.ssmLockLiveData.value as CHSesame5).deviceId

        // 要撈幾頁資料
//        (mDeviceModel.ssmLockLiveData.value as CHSesame5).history(cursor) {
        (mDeviceModel.ssmLockLiveData.value as CHSesame5).history(cursor, currentDeviceUUID!!) {
            it.onSuccess {
//                L.d("hcia", "onSuccess!! :"+it )
                var sesameHistorys = it.data.first
//                L.d("hcia", "sesameHistorys:" + sesameHistorys)
                responseCursor = it.data.second

                if (sesameHistorys.isEmpty()) {
                    return@onSuccess
                }
                room_list?.post {
                    mHistorys.addAll(sesameHistorys)
                    mHistorys = mHistorys.distinctBy { (it.recordID) } as ArrayList<CHSesame5History> // 去時間重複
                    mHistorys.sortBy { it.recordID }

                    mAdapter.mGroupHistData.clear()
                    mAdapter.mGroupHistData.addAll(mHistorys.groupBy { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(it.date) }.toList())
                    if (room_list?.adapter == null) {
//                        L.d("hcia", "room_list?.adapter:")
                        room_list?.adapter = mAdapter
                        if (sesameHistorys.size != 0) {
                            room_list?.layoutManager?.scrollToPosition(room_list?.adapter!!.getItemCount() - 1)
                        }
                    }
                    mAdapter.notifyAllSectionsDataSetChanged()

                    if (it is CHResultState.CHResultStateBLE) {
                        room_list?.layoutManager?.scrollToPosition(room_list?.adapter!!.getItemCount() - 1)
                    }
                    if (isFromuser) {
                        room_list?.layoutManager?.scrollToPosition(room_list?.adapter!!.getItemCount() - 1)
                    }

                }
                swiperefresh?.post {
                    swiperefresh?.isRefreshing = false
                }
            }


        }
    }


    override fun onResume() {
        super.onResume()


        mDeviceModel.ssmosLockDelegates.put(mDeviceModel.ssmLockLiveData.value!!, object : CHDeviceStatusDelegate {

            override fun onBleDeviceStatusChanged(device: CHDevices, status: CHDeviceStatus, shadowStatus: CHDeviceStatus?) {
//                L.d("hcia", "[ui][ss5][his] onBleDeviceStatusChanged:")

                if (device.deviceStatus == CHDeviceStatus.ReceivedAdV) {
                    device.connect {}
                }
                ssmView?.setLock(mDeviceModel.ssmLockLiveData.value as CHSesame5)
                ssmView?.setLockImage(mDeviceModel.ssmLockLiveData.value as CHSesame5)

                if (isTest) {
                    if (device.deviceStatus == CHDeviceStatus.NoBleSignal) {
                        testStatus = "斷線"
                    }
                    if (device.deviceStatus == CHDeviceStatus.Locked) {
                        GlobalScope.launch {
                            delay(delayms)
                            device as CHSesame5
                            device.unlock((testStatus + " " + testCount.toString() + " B:" + device.mechStatus?.getBatteryVoltage() + "V").toByteArray()) { }
                            testCount++
                            testStatus = "ok"
                        }
                    }
                    if (device.deviceStatus == CHDeviceStatus.Unlocked) {

                        GlobalScope.launch {
                            delay(delayms)
                            device as CHSesame5

                            device.lock((testStatus + " " + testCount.toString() + " " + device.mechStatus?.getBatteryVoltage() + "V").toByteArray()) { }
                            testCount++
                            testStatus = "ok"

                        }
                    }
                }


            }

            override fun onMechStatus(device: CHDevices) {
//                L.d("hcia", "[ui][ss5][his] onMechStatusChanged:")

                ssmView?.setLock(mDeviceModel.ssmLockLiveData.value as CHSesame5)
                ssmView?.setLockImage(mDeviceModel.ssmLockLiveData.value as CHSesame5)
//                ssmView.setLock(mDeviceModel.ssmLockLiveData.value as CHSesame5)
//                L.d("hcia", "[ss5] status.isStop:" + status.isStop)

                if (device.mechStatus?.isStop == true) {
//                    L.d("hcia", "mDeviceModel.ssmLockLiveData.value?.deviceStatus?.value:" + mDeviceModel.ssmLockLiveData.value?.deviceStatus?.value)
                    if (mDeviceModel.ssmLockLiveData.value?.deviceStatus?.value == CHDeviceLoginStatus.UnLogin) {
                        GlobalScope.launch {
                            delay(3000)
                            refleshHistory(true)
                        }
                    }

                }
            }
        })
    }

}
