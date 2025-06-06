package co.candyhouse.app.tabs.devices.hub3.setting

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.databinding.FgWm2ScanListBinding
import co.candyhouse.app.tabs.devices.model.bindLifecycle
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.open.device.CHHub3Delegate
import co.candyhouse.sesame.open.device.CHWifiModule2
import co.candyhouse.sesame.utils.L
import co.utils.alerts.ext.inputTextAlert
import co.utils.recycle.GenericAdapter

class Hub3ScanSSIDListFG : BaseDeviceFG<FgWm2ScanListBinding>(), CHHub3Delegate {
    var ssidList = ArrayList<WifiRssi>()
    var ssidMap: MutableMap<String, Short> = mutableMapOf()
    override fun getViewBinder()= FgWm2ScanListBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refleshPage()
       bind. swiperefresh.setOnRefreshListener { refleshPage() }
        mDeviceModel.ssmosLockDelegates[(mDeviceModel.ssmLockLiveData.value!! as CHHub3)] = object : CHHub3Delegate {
            override fun onScanWifiSID(device: CHWifiModule2, ssid: String, rssi: Short) {
                ssidMap[ssid] = ssidMap[ssid]?.let {
                    if (it < rssi) {
                        rssi
                    } else {
                        it
                    }
                } ?: rssi
                ssidList.clear()
                for ((ssid, rssi) in ssidMap) {
                    ssidList.add(WifiRssi(ssid, rssi))
                }
                ssidList.sortByDescending { it.rssi }
                bind.leaderboardList?.post {
                    bind.swiperefresh.isRefreshing = false
                    L.d("hcia", "[hub3]WiFi列表加入:" + ssid)
                    bind.leaderboardList.adapter?.notifyDataSetChanged()
                }
            }
        }.bindLifecycle(viewLifecycleOwner)

        bind.leaderboardList.apply {
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
                                    L.d("hcia", "[hub3]選到WiFi:" + wifi_rssi.ssid)
                                    (mDeviceModel.ssmLockLiveData.value!! as CHHub3).setWifiSSID(wifi_rssi.ssid) {
                                        it.onSuccess {
                                        }
                                    }
                                    view.post {
                                        val defaultPWK = if (BuildConfig.DEBUG) "55667788" else null
                                        context?.inputTextAlert(getString(R.string.wm2_pwk_hint), wifi_rssi.ssid, defaultPWK) {
                                            confirmButtonWithText("OK") { alert, name ->
                                                L.d("hcia", "[hub3]設定密碼:$name")
                                                (mDeviceModel.ssmLockLiveData.value!! as CHHub3).setWifiPassword(name) {}
                                                dismiss()
                                                if (isAdded&&!isDetached){
                                                    try {
                                                        view. findNavController().navigateUp()
                                                    }catch (e:Exception){
                                                        e.printStackTrace()
                                                    }

                                                }

                                            }
                                            cancelButton(getString(R.string.cancel))
                                        }?.show()
                                    }

                                }

                            }
                        }
            }
        }//end bind.leaderboardList.apply
    }

    private fun refleshPage() {
        bind.  swiperefresh.isRefreshing = true
        ssidList.clear() // 清空舊的數據
        L.d("hcia", "[hub3]送出開啟掃描指令")
        (mDeviceModel.ssmLockLiveData.value!! as CHHub3).scanWifiSSID {
            it.onSuccess {
                L.d("hcia", "[hub3]收到掃描開啟成功")
            }
        }
    }

    data class WifiRssi(var ssid: String, var rssi: Short)
}