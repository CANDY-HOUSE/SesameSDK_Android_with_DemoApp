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
import co.candyhouse.sesame.ble.Sesame2.*
import co.candyhouse.sesame.deviceprotocol.CHSesame2Intention
import co.candyhouse.sesame.deviceprotocol.CHSesame2MechStatus
import co.candyhouse.sesame.deviceprotocol.NSError
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
    var mHistorys = ArrayList<CHSesame2History>()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fg_room_main, container, false)
        mRecyclerView = view.findViewById<RecyclerView>(R.id.room_list)
        return view
    }

    val mAdapter = SSMHistoryAdapter(ArrayList())

    @SuppressLint("SimpleDateFormat")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        backicon.setOnClickListener { findNavController().navigateUp() }
        backTitle.text = SharedPreferencesUtils.preferences.getString(mSesame?.deviceId.toString(), mSesame?.deviceId.toString().toUpperCase())
        backTitle.visibility = View.VISIBLE

        mRecyclerView!!.layoutManager = StickyHeaderLayoutManager()
        ssmView.setLock(mSesame!!)
        ssmView.setLockImage(mSesame!!)
        ssmView.setOnClickListener { mSesame?.toggle {} }
        right_icon.setOnClickListener {
            findNavController().navigate(R.id.action_mainRoomFG_to_SSM2SettingFG)
        }

        mRecyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {}

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == 0) {
                    val firstVisibleItemPosition = (mRecyclerView!!.layoutManager as StickyHeaderLayoutManager)
                            .getFirstVisibleItemViewHolder(false)?.positionInSection
                    if (firstVisibleItemPosition == 0) {
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

            it.onSuccess {
                L.d("hcia", "UI收到 歷史it:" + it.javaClass.simpleName)
                val sesameHistorys = it.data


                mRecyclerView?.post {

                    if (sesameHistorys.size == 0) {
                        return@post
                    }
                    /**
                    leftPower its a workaround
                    a. recordID not correct after reboot
                    b. timestamp without ms
                    workaround!!  binding  timestamp with  recordID
                     */
                    val leftPower = 10000000000

                    mHistorys.addAll(sesameHistorys)
                    mHistorys = mHistorys.distinctBy {
                        (it.date.getTime() * leftPower + it.recordID)
                    } as ArrayList<CHSesame2History>

                    mHistorys.sortBy {
                        (it.date.getTime() * leftPower + it.recordID)
                    }
//                L.d("hcia", "總數據量:" + mHistorys.size)
                    val mTestGoupHistory = mHistorys.groupBy {
                        groupTZ().format(it.date)
                    }
//                L.d("hcia", "分組:")
                    mAdapter.mGroupHistData.clear()
                    mAdapter.mGroupHistData.addAll(mTestGoupHistory.toList())
//                L.d("hcia", "添加完畢")
                    if (mRecyclerView?.adapter == null) {
//                    L.d("hcia", "初始化")
                        mRecyclerView!!.adapter = mAdapter
                        if (sesameHistorys.size != 0) {
                            mRecyclerView?.layoutManager?.scrollToPosition(mRecyclerView!!.adapter!!.getItemCount() - 1)

                        }
                    }
//                L.d("hcia", "mRecyclerView?.adapter:" + mAdapter)
                    mAdapter.notifyAllSectionsDataSetChanged()
                    if (it is CHResultState.CHResultStateBLE) {
//                    L.d("hcia", "資料來源:CHResultStateBLE")
                        mRecyclerView?.layoutManager?.scrollToPosition(mRecyclerView!!.adapter!!.getItemCount() - 1)
                    }
//                L.d("hcia", "刷新")
                }
            }
            it.onFailure {
                // todo kill the hint  if you got!!!
                // 這裡是個workaround
                // 理由:多人連線 sesame2 回 busy
                // 策略:延遲網路請求等待隔壁連上的sesame2上傳完畢後拉取

                if (it is NSError) {
                    if (it.code == 7) {
                        hispage = 0
                        refleshHistory()
                    }
                }
//                L.d("hcia", "UI it!!!!!!!!!!!!!!!!!!:" + it.localizedMessage)//it!!!!!!!!!!!!!!!!!!:BUSY
            }
        }
    }


    override fun onResume() {
        super.onResume()
        mSesame?.delegate = object : CHSesame2Delegate {
            override fun onBleDeviceStatusChanged(device: CHSesame2, status: CHSesame2Status,shadowStatus: CHSesame2ShadowStatus?) {
                if (device.deviceStatus == CHSesame2Status.receivedBle) {
                    device.connect() {}
                }
                ssmView?.setLockImage(mSesame!!)
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


