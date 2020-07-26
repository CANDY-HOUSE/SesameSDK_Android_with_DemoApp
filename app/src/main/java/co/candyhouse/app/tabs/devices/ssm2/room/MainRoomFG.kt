package co.candyhouse.app.tabs.devices.ssm2.room

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseSSMFG
import co.candyhouse.sesame.ble.CHSesame2Status
import co.candyhouse.sesame.ble.Sesame2.CHSesame2
import co.candyhouse.sesame.ble.Sesame2.CHSesame2Delegate
import co.candyhouse.sesame.ble.Sesame2.CHSesame2History
import co.candyhouse.sesame.deviceprotocol.CHSesame2Intention
import co.candyhouse.sesame.deviceprotocol.CHSesame2MechStatus
import co.candyhouse.sesame.server.CHResultState
import co.utils.L
import co.utils.SharedPreferencesUtils
import kotlinx.android.synthetic.main.back_sub.*
import kotlinx.android.synthetic.main.fg_room_main.*
import org.zakariya.stickyheaders.StickyHeaderLayoutManager


@ExperimentalUnsignedTypes
class MainRoomFG : BaseSSMFG() {
    private var hispage = 0
    private var mRecyclerView: RecyclerView? = null

    //    private var histoetGroupData: ArrayList<Pair<String, List<CHSesame2History>>> = arrayListOf<Pair<String, List<CHSesame2History>>>()
    var historys = ArrayList<CHSesame2History>()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fg_room_main, container, false)

        mRecyclerView = view.findViewById<RecyclerView>(R.id.room_list)
        return view
    }

    val mAdapter = SSMHistoryAdapter(arrayListOf<Pair<String, List<CHSesame2History>>>())


    @SuppressLint("SimpleDateFormat")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        backicon.setOnClickListener { findNavController().navigateUp() }
        backTitle.text = SharedPreferencesUtils.preferences.getString(mSesame?.deviceId.toString(), mSesame?.deviceId.toString().toUpperCase())
        backTitle.visibility = View.VISIBLE
        mRecyclerView!!.layoutManager = StickyHeaderLayoutManager()
        ssmView.setLock(mSesame!!)
        ssmView.setOnClickListener { mSesame?.toggle() {} }
        right_icon.setOnClickListener {
            findNavController().navigate(R.id.action_mainRoomFG_to_SSM2SettingFG)
        }
        mRecyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == 0) {
                    val firstVisibleItemPosition = (mRecyclerView!!.layoutManager as StickyHeaderLayoutManager)
                            .getFirstVisibleItemViewHolder(false)?.positionInSection
                    if (firstVisibleItemPosition == 0) {
                        L.d("hcia", "滑到頂")
                        hispage = hispage + 1
                        refleshHistory()
                    }
                }
            }
        })

        refleshHistory()
    }

    @SuppressLint("SimpleDateFormat")
    private fun refleshHistory() {
        mSesame?.getHistories(hispage) {
//            L.d("hcia", "UI收到 歷史it:" + it.javaClass.simpleName)
            val his = it.getOrNull()
//            if (his is CHResultState.CHResultStateBLE) {
//                L.d("hcia", "資料來源:CHResultStateBLE")
//            }
//            if (his is CHResultState.CHResultStateNetworks) {
//                L.d("hcia", "資料來源:CHResultStateNetworks")
//            }

//            L.d("hcia", "資料來源:" + his?.javaClass?.simpleName + " 數據量:" + his?.data!!)

//            his?.data?.forEach {
//                L.d("hcia", "UI收到歷史:" + it.recordID + " type:"  + " " + it.date)
//            }

            mRecyclerView?.post {

//        L.d("hcia", "lists:" + lists.size)

                if (his?.data!!.size == 0) {
                    return@post
                }

                historys.addAll(his.data)
                historys = historys.distinctBy {
                    it.recordID
                } as ArrayList<CHSesame2History>

                historys.sortBy {
                    it.recordID
                }
//                L.d("hcia", "總數據量:" + historys.size)


                val mTestGoupHistory = historys.groupBy {
                    groupTZ().format(it.date)
                }
//                L.d("hcia", "分組:")


                mAdapter.mGroupHistData.clear()
                mAdapter.mGroupHistData.addAll(mTestGoupHistory.toList())
//                L.d("hcia", "添加完畢")
                if (mRecyclerView?.adapter == null) {
//                    L.d("hcia", "初始化")

                    mRecyclerView!!.adapter = mAdapter
                    if (his?.data.size != 0) {
                        mRecyclerView?.layoutManager?.scrollToPosition(mRecyclerView!!.adapter!!.getItemCount() - 1)
                    }
                }
                L.d("hcia", "mRecyclerView?.adapter:" + mAdapter)
                mAdapter.notifyAllSectionsDataSetChanged()
                if (his is CHResultState.CHResultStateBLE) {
//                    L.d("hcia", "資料來源:CHResultStateBLE")
                    mRecyclerView?.layoutManager?.scrollToPosition(mRecyclerView!!.adapter!!.getItemCount() - 1)
                }
//                L.d("hcia", "刷新")
            }
        }
    }


    override fun onResume() {
        super.onResume()
        mSesame?.delegate = object : CHSesame2Delegate {
            override fun onBleDeviceStatusChanged(device: CHSesame2, status: CHSesame2Status) {
                if (device.deviceStatus == CHSesame2Status.receiveBle) {
                    device.connnect() {}
                }
            }

            override fun onMechStatusChanged(device: CHSesame2, status: CHSesame2MechStatus, intention: CHSesame2Intention) {
                ssmView?.setLock(mSesame!!)
            }
        }
    }


    companion object {
        @JvmField
        var instance: MainRoomFG? = null
    }

}


