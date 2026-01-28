package co.candyhouse.app.tabs.devices.hub3.setting

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.R
import co.candyhouse.app.databinding.FgWm2ScanListBinding
import co.candyhouse.app.tabs.devices.model.CHDeviceViewModel
import co.candyhouse.app.tabs.devices.model.bindLifecycle
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.open.device.CHHub3Delegate
import co.candyhouse.sesame.open.device.CHWifiModule2
import co.candyhouse.sesame.utils.L
import co.utils.alerts.ext.inputTextAlert
import co.utils.recycle.GenericAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * WiFi SSID 列表
 *
 * @author frey on 2026/1/24
 */
class Hub3ScanSSIDDialogFragment : BottomSheetDialogFragment(), CHHub3Delegate {

    private val tag = "Hub3ScanSSIDDialogFragment"
    private var _binding: FgWm2ScanListBinding? = null
    private val binding get() = _binding!!

    private val ssidMap: MutableMap<String, Short> = mutableMapOf()
    private val ssidList: MutableList<WifiRssi> = mutableListOf()
    private val adapter by lazy { WifiListAdapter(ssidList, ::onWifiSelected) }

    private val mDeviceModel: CHDeviceViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FgWm2ScanListBinding.inflate(inflater, container, false)
        binding.root.setBackgroundResource(R.drawable.bg_bottom_sheet_rounded)
        binding.root.clipToOutline = true
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ssidDialogClose.setOnClickListener {
            dismiss()
        }
        setupList()
        setupRefresh()
        bindHub3Delegate()
        refreshPage()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setOnShowListener { dialogInterface ->
                val d = dialogInterface as BottomSheetDialog
                val sheet = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                    ?: return@setOnShowListener

                sheet.setBackgroundColor(Color.TRANSPARENT)
                sheet.elevation = 0f
                sheet.clipToOutline = false

                val topGapPx = (40f * resources.displayMetrics.density).toInt()
                val screenHeight = resources.displayMetrics.heightPixels
                val targetHeight = screenHeight - topGapPx
                sheet.layoutParams = sheet.layoutParams.apply { height = targetHeight }
                sheet.requestLayout()

                val behavior = BottomSheetBehavior.from(sheet)
                behavior.isDraggable = false
                behavior.isFitToContents = true
                behavior.skipCollapsed = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED

                ViewCompat.setOnApplyWindowInsetsListener(sheet) { _, insets ->
                    val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                    binding.foot.setPadding(
                        binding.foot.paddingLeft,
                        binding.foot.paddingTop,
                        binding.foot.paddingRight,
                        bottom
                    )
                    insets
                }
                ViewCompat.requestApplyInsets(sheet)
            }
        }
    }

    override fun getTheme(): Int = R.style.Theme_App_WifiScanBottomSheet

    private fun setupList() {
        binding.leaderboardList.adapter = adapter
    }

    private fun setupRefresh() {
        binding.swiperefresh.setOnRefreshListener { refreshPage() }
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
            binding.swiperefresh.isRefreshing = false
            return
        }

        binding.swiperefresh.isRefreshing = true
        ssidMap.clear()
        ssidList.clear()
        adapter.notifyDataSetChanged()

        hub3.scanWifiSSID {
            it.onSuccess {
                L.d(tag, "[hub3]收到掃描開啟成功")
            }
            it.onFailure { e ->
                binding.swiperefresh.isRefreshing = false
                L.d(tag, "[hub3]掃描開啟失敗: ${e.message}")
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun onWifiScanResult(ssid: String, rssi: Short) {
        if (!isAdded) return

        val old = ssidMap[ssid]
        if (old == null || old < rssi) ssidMap[ssid] = rssi

        ssidList.clear()
        ssidMap.forEach { (k, v) -> ssidList.add(WifiRssi(k, v)) }
        ssidList.sortByDescending { it.rssi }

        binding.leaderboardList.post {
            binding.swiperefresh.isRefreshing = false
            L.d(tag, "[hub3]WiFi列表加入:$ssid")
            adapter.notifyDataSetChanged()
        }
    }

    private fun onWifiSelected(itemView: View, wifi: WifiRssi) {
        val hub3 = currentHub3OrNull() ?: return
        L.d(tag, "[hub3]選到WiFi:${wifi.ssid}")
        hub3.setWifiSSID(wifi.ssid) { }

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
                this@Hub3ScanSSIDDialogFragment.dismiss()
            }
            cancelButton(getString(R.string.cancel))
        }?.show()
    }

    private fun currentHub3OrNull(): CHHub3? {
        return (mDeviceModel.ssmLockLiveData.value as? CHHub3)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class WifiRssi(val ssid: String, val rssi: Short)

    private class WifiListAdapter(
        data: List<WifiRssi>,
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

    companion object {
        const val TAG = "Hub3ScanSSIDDialog"
        fun newInstance(): Hub3ScanSSIDDialogFragment = Hub3ScanSSIDDialogFragment()
    }
}