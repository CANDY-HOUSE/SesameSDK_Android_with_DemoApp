package co.candyhouse.app.tabs.devices.wm2.setting

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.sesame.open.device.CHWifiModule2
import co.candyhouse.sesame.open.device.CHWifiModule2Delegate
import co.utils.L
import co.utils.alerts.ext.*
import co.utils.recycle.GenericAdapter
import kotlinx.android.synthetic.main.fg_wm2_scan_list.*


class WM2ScanSSIDListFG : BaseDeviceFG(R.layout.fg_wm2_scan_list), CHWifiModule2Delegate {

    var ssidList = ArrayList<WifiRssi>()
    var ssidMap: MutableMap<String, Short> = mutableMapOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        refleshPage()

        swiperefresh.setOnRefreshListener { refleshPage() }

        mDeviceModel.ssmosLockDelegates.put((mDeviceModel.ssmLockLiveData.value!! as CHWifiModule2), object : CHWifiModule2Delegate {
            override fun onScanWifiSID(device: CHWifiModule2, ssid: String, rssi: Short) {
//                L.d("hcia", "ssid 添加:" + ssid)
                ssidMap[ssid] = ssidMap[ssid]?.let {
                    if (it < rssi) {
                        rssi
                    } else {
                        it
                    }
                } ?: rssi
            }

        })
        leaderboard_list.apply {
            adapter = object : GenericAdapter<WifiRssi>(ssidList) {
                override fun getLayoutId(position: Int, obj: WifiRssi): Int = R.layout.key_cell
                override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder =
                    object : RecyclerView.ViewHolder(view), Binder<WifiRssi> {
                        var wifiImg = itemView.findViewById<ImageView>(R.id.wifi_img)
                        var title = itemView.findViewById<TextView>(R.id.title)
                        var sub_title = itemView.findViewById<TextView>(R.id.sub_title)
                        override fun bind(wifi_rssi: WifiRssi, pos: Int) {
                            title.text = wifi_rssi.ssid
                            sub_title.text = wifi_rssi.rssi.toString()
                            wifi_rssi.rssi > -50
                            wifiImg.setImageResource(if (wifi_rssi.rssi > -50) R.drawable.ic_wifi_blue else if (wifi_rssi.rssi > -70) R.drawable.ic_wifi_blue_middle else R.drawable.ic_wifi_blue_weak)

                            itemView.setOnClickListener {
                                (mDeviceModel.ssmLockLiveData.value!! as CHWifiModule2).setWifiSSID(wifi_rssi.ssid) {
                                    it.onSuccess {
                                        view.post {

//                                                var defaultPWK = if (BuildConfig.DEBUG) "jt232kvuds0ny" else null
                                            val defaultPWK = if (BuildConfig.DEBUG) "55667788" else null
                                            context?.inputTextAlert(getString(R.string.wm2_pwk_hint), wifi_rssi.ssid, defaultPWK) {
                                                confirmButtonWithText("OK") { alert, name ->
                                                    (mDeviceModel.ssmLockLiveData.value!! as CHWifiModule2).setWifiPassword(name) {}
                                                    (mDeviceModel.ssmLockLiveData.value!! as CHWifiModule2).connectWifi {
                                                        it.onSuccess {
                                                            L.d("hcia", "連線成功it.data:" + it.data)
                                                            activity!!.runOnUiThread {
                                                                Toast.makeText(activity!!, activity!!.getString(R.string.Wm2WifiOK), Toast.LENGTH_LONG).show()
                                                            }

                                                        }
                                                        it.onFailure {
                                                            activity!!.runOnUiThread {
                                                                activity!!.alert(activity!!.getString(R.string.wm2_ap_setting_error), null) {
                                                                    L.d("hcia", " this:" + this)
                                                                }.show()
//                                                                    Toast.makeText(activity!!, "error:" + it, Toast.LENGTH_LONG).show()
                                                            }
                                                        }
                                                    }
                                                    dismiss()
                                                    findNavController().navigateUp()
                                                }
                                                cancelButton(getString(R.string.cancel))
                                            }?.show()
                                        }

                                    }
                                }
                            }
                        }
                    }
            }
        }//end leaderboard_list.apply
    }

    private fun refleshPage() {
        swiperefresh.isRefreshing = true
        L.d("hcia", "ssidList 清空:" + ssidList)
        ssidList.clear()
        leaderboard_list.adapter?.notifyDataSetChanged()
        (mDeviceModel.ssmLockLiveData.value!! as CHWifiModule2).scanWifiSSID {
            it.onSuccess {
                L.d("hcia", "UI 掃描完畢")
                ssidMap.forEach { ssid_rssi ->
                    ssidList.add(WifiRssi(ssid_rssi.key, ssid_rssi.value))
                }
                ssidList.sortByDescending { it.rssi }
                leaderboard_list?.post {
                    swiperefresh.isRefreshing = false
                    leaderboard_list.adapter?.notifyDataSetChanged()
                }
            }
        }
    }

    data class WifiRssi(var ssid: String, var rssi: Short)
}