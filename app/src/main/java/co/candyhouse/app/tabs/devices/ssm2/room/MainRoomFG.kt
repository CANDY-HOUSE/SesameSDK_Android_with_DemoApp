package co.candyhouse.app.tabs.devices.ssm2.room

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import co.candyhouse.app.base.BaseSSMFG
import co.candyhouse.sesame.ble.CHSesame2Status
import co.candyhouse.sesame.ble.CHSesame2
import co.candyhouse.sesame.ble.Sesame2.CHSesame2Delegate
import co.candyhouse.app.R
import co.utils.SharedPreferencesUtils
import kotlinx.android.synthetic.main.back_sub.*
import kotlinx.android.synthetic.main.fg_room_main.*
import org.zakariya.stickyheaders.StickyHeaderLayoutManager


class MainRoomFG : BaseSSMFG() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fg_room_main, container, false)
        instance = this
        return view
    }

    @SuppressLint("SimpleDateFormat")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        backicon.setOnClickListener { findNavController().navigateUp() }
        backTitle.text = SharedPreferencesUtils.preferences.getString(mSesame?.deviceId.toString(), mSesame?.deviceId.toString().toUpperCase())
        backTitle.visibility = View.VISIBLE
        room_list.layoutManager = StickyHeaderLayoutManager()
        ssmView.setLock(mSesame!!)
        ssmView.setOnClickListener { mSesame?.toggle() {} }
        right_icon.setOnClickListener {
            findNavController().navigate(R.id.action_mainRoomFG_to_SSM2SettingFG)
        }

        refleshHistory()
    }

    @SuppressLint("SimpleDateFormat")
    private fun refleshHistory() {
//        mSesame?.getHistories { res ->
//            if (res!!.isCache) {
//                refleshUIList(res.data!!)
//            } else {
//                val hisRes: CHResState.net.historyRes<List<HistoryAndOperater>> = res as CHResState.net.historyRes
//                if (!hisRes.isEnd) {
//                    refleshHistory()
//                }
//                refleshUIList(res.data!!)
//            }
//        }
    }

//    private fun refleshUIList(lists: List<HistoryAndOperater>) {
//        if (lists.size == 0) {
//            return
//        }
//        val mTestGoupHistory = lists.groupBy {
//            groupTZ().format(showTZ().parse(it.history.timestamp))
//        }
//        val testGList = mTestGoupHistory.toList()
//        val tmpList = arrayListOf<Pair<String, List<HistoryAndOperater>>>()
//        tmpList.addAll(testGList)
//        tmpList.sortBy {
//            groupTZ().parse(it.first)
//        }.apply {
//            runOnUiThread(Runnable {
//                room_list?.adapter = SSMHistoryAdapter(tmpList)
//                room_list?.adapter?.notifyDataSetChanged()
//                if (lists.size != 0) {
//                    room_list?.layoutManager?.scrollToPosition(room_list.adapter!!.getItemCount() - 1)
//                }
//            })
//        }
//    }

    override fun onResume() {
        super.onResume()
        mSesame?.delegate = object : CHSesame2Delegate {
            override fun onBleDeviceStatusChanged(device: CHSesame2, status: CHSesame2Status) {
                ssmView?.setLock(mSesame!!)
                if (device.deviceStatus == CHSesame2Status.receiveBle) {
                    device.connnect() {}
                }
            }
        }
    }


    companion object {
        @JvmField
        var instance: MainRoomFG? = null
    }

}


