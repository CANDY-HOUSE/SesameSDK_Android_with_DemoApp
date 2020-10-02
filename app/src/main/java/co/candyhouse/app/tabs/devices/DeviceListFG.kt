package co.candyhouse.app.tabs.devices

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import co.candyhouse.app.base.BaseFG
import co.candyhouse.app.base.BaseSSMFG
import co.candyhouse.app.tabs.MainActivity
import co.candyhouse.app.tabs.devices.ssm2.setting.angle.SSMCellView
import co.candyhouse.app.tabs.devices.ssm2.test.BlueSesameControlActivity
import co.candyhouse.sesame.ble.*
import co.candyhouse.sesame.ble.Sesame2.CHSesame2Delegate
import co.candyhouse.sesame2.BuildConfig
import co.candyhouse.app.R
import co.candyhouse.app.tabs.account.login.toastMSG
import co.candyhouse.sesame.ble.Sesame2.CHSesame2
import co.candyhouse.sesame.ble.Sesame2.CHSesame2Status
import co.candyhouse.sesame.ble.Sesame2.CHSesame2ShadowStatus
import co.candyhouse.sesame.deviceprotocol.CHSesame2Intention
import co.candyhouse.sesame.deviceprotocol.CHSesame2MechStatus
import co.utils.L
import co.utils.SharedPreferencesUtils
import co.utils.recycle.GenericAdapter
import kotlinx.android.synthetic.main.fg_devicelist.*
import java.util.*

class DeviceListFG : BaseFG() {
    companion object {
        var instance: DeviceListFG? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
    }

    var mDeviceList = ArrayList<CHSesame2>()
    lateinit var testSwich: Switch
    lateinit var recyclerView: RecyclerView
    lateinit var swiperefreshView: SwipeRefreshLayout

    override fun onResume() {
        super.onResume()
        if (MainActivity.nowTab == 0) {
            (activity as MainActivity).showMenu()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
//        L.d("hcia", "onCreateView:")
        val view = inflater.inflate(R.layout.fg_devicelist, container, false)
        swiperefreshView = view.findViewById<SwipeRefreshLayout>(R.id.swiperefresh).apply {
            setOnRefreshListener {
                refleshPage()
            }
        }
        refleshPage()

        recyclerView = view.findViewById<RecyclerView>(R.id.leaderboard_list).apply {
            setHasFixedSize(true)
            adapter = object : GenericAdapter<Any>(mDeviceList) {
                override fun getLayoutId(position: Int, obj: Any): Int {
                    return R.layout.sesame_layout
                }

                override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder {
                    return object : RecyclerView.ViewHolder(view), Binder<CHSesame2> {

                        var ssmView: SSMCellView = view.findViewById(R.id.ssmView)
                        var customName: TextView = view.findViewById(R.id.title)
                        var sesame2Status: TextView = view.findViewById(R.id.sub_title)
                        var shadowStatusTxt: TextView = view.findViewById(R.id.sub_title_2)
                        var testICon: View = view.findViewById(R.id.test)
                        var battery_percent: TextView = view.findViewById(R.id.battery_percent)
                        var battery: ImageView = view.findViewById(R.id.battery)

                        @SuppressLint("SetTextI18n")
                        override fun bind(data: CHSesame2, pos: Int) {
                            val sesame = data
                            sesame.delegate = object : CHSesame2Delegate {
                                override fun onMechStatusChanged(device: CHSesame2, status: CHSesame2MechStatus, intention: CHSesame2Intention) {
//                                    L.d("hcia", "status:" + status.position)
                                    ssmView.setLock(device)
                                }

                                override fun onBleDeviceStatusChanged(device: CHSesame2, status: CHSesame2Status, shadowStatus: CHSesame2ShadowStatus?) {
//                                    L.d("hcia", "UI 收到status:" + status)
                                    sesame2Status?.post {
                                        ssmView.setLock(device)
                                        ssmView.setLockImage(device)
                                        sesame2Status.text = status.toString()
                                        shadowStatusTxt.text = shadowStatus.toString()
                                        battery_percent.text = sesame.mechStatus?.getBatteryPrecentage().toString() + "%"
                                        battery_percent.visibility = if (sesame.mechStatus == null) View.GONE else View.VISIBLE
                                        battery.visibility = if (sesame.mechStatus == null) View.GONE else View.VISIBLE
                                    }

                                    if (status == CHSesame2Status.receivedBle) {
//                                        L.d("hcia","下連接----")
                                        sesame.connect() {
//                                            L.d("hcia", "it:" + it)
                                            it.onFailure {
                                                toastMSG(it.message)
                                            }
                                        }
                                    }
                                }
                            }

//                            L.d("hcia","下連接＋＋＋")
                            sesame.connect() {
                                it.onFailure {
//                                    L.d("hcia", "it.message:" + it.message)
//                                    toastMSG(it.message)
                                }
                            }
                            ssmView.setLock(sesame)
                            ssmView.setLockImage(sesame)

                            ssmView.setOnClickListener {
                                sesame.toggle() {
                                    it.onFailure {
                                        toastMSG(it.message)
                                    }
                                }
                            }

                            battery_percent.visibility = if (sesame.mechStatus == null) View.GONE else View.VISIBLE
                            battery_percent.text = sesame.mechStatus?.getBatteryPrecentage().toString() + "%"
                            battery.visibility = if (sesame.mechStatus == null) View.GONE else View.VISIBLE
                            customName.text = SharedPreferencesUtils.preferences.getString(sesame.deviceId.toString(), sesame.deviceId.toString().toUpperCase())
                            sesame2Status.text = sesame.deviceStatus.toString()
                            testICon.visibility = if (testSwich.isChecked) View.VISIBLE else View.GONE
                            testICon.setOnClickListener {
                                BlueSesameControlActivity.ssm = sesame
                                view.context.startActivity(Intent(view.context, BlueSesameControlActivity().javaClass))
                            }
                            view.setOnClickListener {
                                BaseSSMFG.mSesame = sesame
                                findNavController().navigate(R.id.action_deviceListPG_to_mainRoomFG)
                            }
                        }
                    }
                }
            }

        }
        testSwich = view.findViewById<Switch>(R.id.testSwich).apply {
            setOnCheckedChangeListener { buttonView, isChecked ->
                recyclerView.adapter?.notifyDataSetChanged()
            }
        }


        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        testAPI.visibility = if (BuildConfig.BUILD_TYPE == "debug") View.VISIBLE else View.INVISIBLE
        testSwich.visibility = if (BuildConfig.BUILD_TYPE == "debug") View.VISIBLE else View.INVISIBLE
        testSwich.isChecked = false
        testAPI.setOnClickListener {
            findNavController().navigate(R.id.action_deviceListPG_to_newWifiMD2FG)
        }

        //  todo   kill test
//        findNavController().navigate(R.id.action_deviceListPG_to_newWifiMD2FG)

    }


    fun refleshPage() {
//        runOnUiThread {
//            swiperefreshView.isRefreshing = true
//        }
        CHDeviceManager.getSesame2s {
            it.onSuccess {
                mDeviceList.clear()
                mDeviceList.addAll(it.data)
                recyclerView?.post {
                    (recyclerView.adapter as GenericAdapter<*>).notifyDataSetChanged()
                    swiperefreshView.isRefreshing = false
                }
            }
        }
    }
}


