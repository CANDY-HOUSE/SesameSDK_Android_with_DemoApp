package co.candyhouse.app.tabs.devices.hub3.setting

import android.annotation.SuppressLint
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

    private val tag = "Hub3ScanSSIDListFG"

    private val ssidMap: MutableMap<String, Short> = mutableMapOf()
    private val ssidList: MutableList<WifiRssi> = mutableListOf()

    private val adapter by lazy { WifiListAdapter(ssidList, ::onWifiSelected) }

    override fun getViewBinder() = FgWm2ScanListBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupList()
        setupRefresh()
        bindHub3Delegate()

        refreshPage()
    }

    private fun setupList() {
        bind.leaderboardList.adapter = adapter
    }

    private fun setupRefresh() {
        bind.swiperefresh.setOnRefreshListener { refreshPage() }
    }

    private fun bindHub3Delegate() {
        val hub3 = currentHub3OrNull() ?: return

        mDeviceModel.ssmosLockDelegates[hub3] = object : CHHub3Delegate {
            override fun onScanWifiSID(device: CHWifiModule2, ssid: String, rssi: Short) {
                onWifiScanResult(ssid, rssi)
            }
        }.bindLifecycle(viewLifecycleOwner)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshPage() {
        val hub3 = currentHub3OrNull() ?: run {
            bind.swiperefresh.isRefreshing = false
            return
        }

        bind.swiperefresh.isRefreshing = true
        ssidMap.clear()
        ssidList.clear()
        adapter.notifyDataSetChanged()

        L.d(tag, "[hub3]送出開啟掃描指令")
        hub3.scanWifiSSID {
            it.onSuccess {
                L.d(tag, "[hub3]收到掃描開啟成功")
            }
            it.onFailure { e ->
                bind.swiperefresh.isRefreshing = false
                L.d(tag, "[hub3]掃描開啟失敗: ${e.message}")
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun onWifiScanResult(ssid: String, rssi: Short) {
        val old = ssidMap[ssid]
        if (old == null || old < rssi) ssidMap[ssid] = rssi

        ssidList.clear()
        ssidMap.forEach { (k, v) -> ssidList.add(WifiRssi(k, v)) }
        ssidList.sortByDescending { it.rssi }

        bind.leaderboardList.post {
            bind.swiperefresh.isRefreshing = false
            L.d(tag, "[hub3]WiFi列表加入:$ssid")
            adapter.notifyDataSetChanged()
        }
    }

    private fun onWifiSelected(itemView: View, wifi: WifiRssi) {
        val hub3 = currentHub3OrNull() ?: return

        L.d(tag, "[hub3]選到WiFi:${wifi.ssid}")
        hub3.setWifiSSID(wifi.ssid) { }

        itemView.post {
            val defaultPWK = if (BuildConfig.DEBUG) "55667788" else null
            context?.inputTextAlert(
                getString(R.string.wm2_pwk_hint),
                wifi.ssid,
                defaultPWK
            ) {
                confirmButtonWithText("OK") { _, pw ->
                    L.d(tag, "[hub3]設定密碼:$pw")
                    hub3.setWifiPassword(pw) { }
                    dismiss()
                    if (isAdded && !isDetached) {
                        itemView.findNavController().navigateUp()
                    }
                }
                cancelButton(getString(R.string.cancel))
            }?.show()
        }
    }

    private fun currentHub3OrNull(): CHHub3? {
        return (mDeviceModel.ssmLockLiveData.value as? CHHub3)
    }

    data class WifiRssi(val ssid: String, val rssi: Short)

    private class WifiListAdapter(
        private val data: List<WifiRssi>,
        private val onClick: (View, WifiRssi) -> Unit
    ) : GenericAdapter<WifiRssi>(data as MutableList<WifiRssi>) {

        override fun getLayoutId(position: Int, obj: WifiRssi): Int = R.layout.key_cell

        override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder {
            return object : RecyclerView.ViewHolder(view), Binder<WifiRssi> {

                private val wifiImg = itemView.findViewById<ImageView>(R.id.wifi_img)
                private val title = itemView.findViewById<TextView>(R.id.title)
                private val subTitle = itemView.findViewById<TextView>(R.id.sub_title)

                override fun bind(obj: WifiRssi, pos: Int) {
                    title.text = obj.ssid
                    subTitle.text = obj.rssi.toString()

                    val rssi = obj.rssi.toInt()
                    wifiImg.setImageResource(
                        when {
                            rssi > -50 -> R.drawable.ic_wifi_blue
                            rssi > -70 -> R.drawable.ic_wifi_blue_middle
                            else -> R.drawable.ic_wifi_blue_weak
                        }
                    )

                    itemView.setOnClickListener { onClick(itemView, obj) }
                }
            }
        }
    }
}