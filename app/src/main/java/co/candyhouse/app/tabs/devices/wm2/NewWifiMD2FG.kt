package co.candyhouse.app.tabs.devices.wm2

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.tabs.MainActivity
import co.candyhouse.sesame.ble.CHBleManager
import co.candyhouse.sesame.ble.CHBleManagerDelegate
import co.candyhouse.sesame.ble.Sesame2.CHSesame2
import co.candyhouse.sesame.ble.Sesame2.CHSesame2Delegate
import co.candyhouse.app.R
import co.candyhouse.app.tabs.devices.DeviceListFG
import co.candyhouse.app.tabs.devices.ssm2.setting.DfuService
import co.candyhouse.app.tabs.devices.wm2.test.Wm2TestActivity
import co.candyhouse.sesame.ble.Sesame2.CHSesame2Status
import co.candyhouse.sesame.ble.Sesame2.CHSesame2ShadowStatus
import co.candyhouse.sesame.ble.wm2.CHWifiModule2
import co.utils.L
import co.utils.recycle.EmptyRecyclerView
import co.utils.recycle.GenericAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fg_rg_device.*
import no.nordicsemi.android.dfu.DfuProgressListener
import no.nordicsemi.android.dfu.DfuServiceInitiator
import no.nordicsemi.android.dfu.DfuServiceListenerHelper
import java.lang.Math.pow
import java.util.*

class NewWifiMD2FG : Fragment() {

    private lateinit var recyclerView: EmptyRecyclerView
    var mDeviceList = ArrayList<CHWifiModule2>()
    private var lastClickTime = 0L

    override fun onResume() {
        super.onResume()

        (activity as MainActivity).hideMenu()
        CHBleManager.delegate = object : CHBleManagerDelegate {
            override fun didDiscoverUnRegisteredWifiModule2s(wifiModules: List<CHWifiModule2>) {
                mDeviceList.clear()
                mDeviceList.addAll(wifiModules.sortedByDescending { it.rssi })
//                mDeviceList.firstOrNull()?.connect { }
                mDeviceList.let {
                    recyclerView.post {
//                    L.d("hcia", "sesames:" + sesames.first().rssi)
                        if (System.currentTimeMillis() - lastClickTime >= 500) {//FAST_CLICK_DELAY_TIME = 500
                            (recyclerView.adapter as GenericAdapter<*>).notifyDataSetChanged()
                            lastClickTime = System.currentTimeMillis()
                        }
                    }
                }
            }

        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fg_new_wm2, container, false)
        recyclerView = view.findViewById<EmptyRecyclerView>(R.id.leaderboard_list).apply {
            setHasFixedSize(true)
            adapter = object : GenericAdapter<Any>(mDeviceList) {

                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                    return super.onCreateViewHolder(parent, viewType)

                }

                override fun getLayoutId(position: Int, obj: Any): Int {
                    return R.layout.cell_wm2_unregist
                }

                override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder {
                    return object : RecyclerView.ViewHolder(view),
                            Binder<CHWifiModule2> {
                        var customName: TextView = itemView.findViewById(R.id.title)
                        var uuidTxt: TextView = itemView.findViewById(R.id.title_txt)

                        //                        var update_img: ImageView = itemView.findViewById(R.id.update_fw)
                        var statusTxt: TextView = itemView.findViewById(R.id.subtitle_txt)

                        @SuppressLint("SetTextI18n")
                        override fun bind(data: CHWifiModule2, pos: Int) {
                            val carpet = data

                            itemView.setOnClickListener {
//                                carpet.connect {}
                                Wm2TestActivity.wm2 = carpet
                                view.context.startActivity(Intent(view.context, Wm2TestActivity().javaClass))
                            }

                            val distance: Int = (pow(10.0, ((carpet.txPowerLevel!! - carpet.rssi!!.toDouble() - 62.0) / 20.0)) * 100).toInt()
                            customName.text = "" + (if (carpet.rssi == null) "-" else distance.toString() + " cm")
                            uuidTxt.text = carpet.deviceId.toString().toUpperCase()
                            statusTxt.text = "WM2:" + data.deviceStatus
                        }
                    }
                }
            }
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.setEmptyView(empty_view)
        backicon.setOnClickListener { findNavController().navigateUp() }
        L.d("hcia", "搜尋 wm2頁面 :")
    }


}
