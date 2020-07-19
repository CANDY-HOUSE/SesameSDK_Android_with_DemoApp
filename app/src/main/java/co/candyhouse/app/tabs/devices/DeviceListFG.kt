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
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread
import co.candyhouse.app.tabs.devices.ssm2.setting.angle.SSMCellView
import co.candyhouse.app.tabs.devices.ssm2.test.BlueSesameControlActivity
import co.candyhouse.sesame.ble.*
import co.candyhouse.sesame.ble.Sesame2.CHSesame2Delegate
import co.candyhouse.sesame2.BuildConfig
import co.candyhouse.app.R
import co.candyhouse.app.tabs.account.login.toastMSG
import co.candyhouse.app.tabs.devices.ssm2.ssmUIParcer
import co.candyhouse.sesame.deviceprotocol.NSError
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
                        var toggle: Button = view.findViewById(R.id.toggle)
                        var customName: TextView = view.findViewById(R.id.title)
                        var ownerName: TextView = view.findViewById(R.id.sub_title)
                        var testICon: View = view.findViewById(R.id.test)
                        var battery_percent: TextView = view.findViewById(R.id.battery_percent)
                        var battery: ImageView = view.findViewById(R.id.battery)


                        @SuppressLint("SetTextI18n")
                        override fun bind(data: CHSesame2, pos: Int) {
                            val sesame = data
                            sesame.delegate = object : CHSesame2Delegate {
                                override fun onBleDeviceStatusChanged(device: CHSesame2, status: CHSesame2Status) {
//                                    L.d("hcia", device.deviceId.toString() + " UI chDeviceStatus:" + device.deviceStatus)
//                                    battery.setBackgroundResource(ssmBatteryParcer(device))
                                    toggle?.post {
                                        toggle.setBackgroundResource(ssmUIParcer(device))
                                        ownerName.text = sesame.deviceStatus.toString()
                                        battery_percent.text = sesame.mechStatus?.batteryPrecentage().toString() + "%"
                                        battery_percent.visibility = if (sesame.mechStatus == null) View.GONE else View.VISIBLE
                                        battery.visibility = if (sesame.mechStatus == null) View.GONE else View.VISIBLE
                                        ssmView.setLock(device)
                                    }

                                    if (sesame.deviceStatus == CHSesame2Status.receiveBle) {
                                        sesame.connnect() {}
                                    }
                                }
                            }
                            sesame.connnect() {}
                            ssmView.setLock(sesame)
                            ssmView.setOnClickListener {
                                sesame.toggle() {
                                    it.onFailure {
                                        L.d("hcia", "it!!!!!:" + it)
                                        L.d("hcia", "it!!!!!:" + (it as NSError).code)
                                        L.d("hcia", "it!!!!!:" + (it as NSError).domaon)
                                        toastMSG(it.message)
                                    }
                                }
                            }

//                            battery.setBackgroundResource(ssmBatteryParcer(sesame))
                            battery_percent.visibility = if (sesame.mechStatus == null) View.GONE else View.VISIBLE
                            battery_percent.text = sesame.mechStatus?.batteryPrecentage().toString() + "%"

                            battery.visibility = if (sesame.mechStatus == null) View.GONE else View.VISIBLE
                            customName.text = SharedPreferencesUtils.preferences.getString(sesame.deviceId.toString(), sesame.deviceId.toString().toUpperCase())
                            ownerName.text = sesame.deviceStatus.toString()
                            toggle.setOnClickListener { sesame.toggle() {} }
                            toggle.setBackgroundResource(ssmUIParcer(sesame))
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

        if (BuildConfig.BUILD_TYPE == "debug") {
            testSwich.isChecked = false
            testSwich.visibility = View.VISIBLE
            testAPI.visibility = View.VISIBLE

        } else {
            testSwich.isChecked = false
            testSwich.visibility = View.INVISIBLE
            testAPI.visibility = View.INVISIBLE
        }

    }


    fun refleshPage() {
        runOnUiThread {
            swiperefreshView.isRefreshing = true
        }
        CHDeviceManager.getSesame2s {

//            it.forEach {
//                L.d("hcia", "UI 拿到it.deviceId:" + it.deviceId + " " + it.deviceStatus)
//            }
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


