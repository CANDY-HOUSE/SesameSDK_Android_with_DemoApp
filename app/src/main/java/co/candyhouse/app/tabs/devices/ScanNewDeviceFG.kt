package co.candyhouse.app.tabs.devices

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.base.setPage
import co.candyhouse.app.databinding.FgRgDeviceBinding
import co.candyhouse.app.ext.webview.data.WebViewConfig
import co.candyhouse.app.tabs.devices.ssm2.getDistance
import co.candyhouse.app.tabs.devices.ssm2.getLevel
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.app.tabs.devices.ssm2.setIsJustRegister
import co.candyhouse.app.tabs.devices.ssm2.setLevel
import co.candyhouse.sesame.ble.os3.CHSesameBiometricDevice
import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.open.CHBleManagerDelegate
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHDeviceStatusDelegate
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.open.device.CHSesame2
import co.candyhouse.sesame.open.device.CHSesame5
import co.candyhouse.sesame.open.device.CHWifiModule2
import co.candyhouse.sesame.server.CHAPIClientBiz
import co.candyhouse.sesame.server.dto.cheyKeyToUserKey
import co.candyhouse.sesame.utils.L
import co.utils.alertview.fragments.toastMSG
import co.utils.getHistoryTag
import co.utils.recycle.GenericAdapter
import co.utils.safeNavigate
import com.amazonaws.mobileconnectors.apigateway.ApiClientException
import com.google.android.material.bottomnavigation.BottomNavigationView
import pub.devrel.easypermissions.EasyPermissions

@SuppressLint("SetTextI18n")
class ScanNewDeviceFG : BaseDeviceFG<FgRgDeviceBinding>() {

    private var mDeviceList = ArrayList<CHDevices>()
    override fun getViewBinder() = FgRgDeviceBinding.inflate(layoutInflater)

    private var isDestory = false

    override fun onDestroy() {
        super.onDestroy()
        isDestory = true
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getPermissions()
        isDestory = false
        (context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter.enable()

        bind.topBackImg.setOnClickListener { findNavController().navigateUp() }
        bind.leaderboardList.setEmptyView(bind.emptyView)
        bind.leaderboardList.adapter = object : GenericAdapter<CHDevices>(mDeviceList) {
            override fun getLayoutId(position: Int, obj: CHDevices): Int {
                return R.layout.cell_device_unregist
            }

            override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder {
                return object : RecyclerView.ViewHolder(view), Binder<CHDevices> {
                    override fun bind(data: CHDevices, pos: Int) {
                        (itemView.findViewById<TextView>(R.id.title)!!).text =
                            "${data.getDistance()} cm"
                        (itemView.findViewById<TextView>(R.id.title_txt)!!).text =
                            data.deviceId.toString().uppercase()
                        (itemView.findViewById<TextView>(R.id.subtitle_txt)!!).text =
                            data.deviceStatus.toString()
                        (itemView.findViewById<TextView>(R.id.sub_title)!!).text =
                            data.getNickname()
                        itemView.setOnClickListener {
                            data.connect { }
                            doRegisterDevice(data)
                            data.delegate = object : CHDeviceStatusDelegate {
                                override fun onBleDeviceStatusChanged(
                                    device: CHDevices,
                                    status: CHDeviceStatus,
                                    shadowStatus: CHDeviceStatus?
                                ) {
                                    if (status == CHDeviceStatus.ReadyToRegister) {
                                        doRegisterDevice(device)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        CHBleManager.delegate = object : CHBleManagerDelegate {
            override fun didDiscoverUnRegisteredCHDevices(devices: List<CHDevices>) {
                mDeviceList.clear()
                mDeviceList.addAll(devices.filter { it.rssi != null })
                try {
                    mDeviceList.sortBy { it.getDistance() }
                } catch (e: IllegalArgumentException) {
                    L.d("didDiscoverUnRegisteredCHDevices", "Sorting error: ${e.message}")
                }

                mDeviceList.firstOrNull()?.connect { }
                bind.leaderboardList.post((bind.leaderboardList.adapter as GenericAdapter<*>)::notifyDataSetChanged)
            }
        }
    }

    private fun getPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (EasyPermissions.hasPermissions(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            ) {
                CHBleManager.enableScan {}
            } else {
                L.d("hcia", "Build.VERSION.SDK_INT:" + Build.VERSION.SDK_INT)
                L.d("hcia", "Build.VERSION_CODES.S:" + Build.VERSION_CODES.S)
                L.d(
                    "hcia",
                    "(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S):" + (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                )
                EasyPermissions.requestPermissions(
                    this,
                    getString(R.string.launching_why_need_location_permission),
                    0,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            }
        } else {
            if (EasyPermissions.hasPermissions(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            ) {
                CHBleManager.enableScan {}
            } else {
                EasyPermissions.requestPermissions(
                    this,
                    getString(R.string.launching_why_need_location_permission),
                    0,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_ADMIN
                )

            }
        }

    }

    private fun doRegisterDevice(device: CHDevices) {
        if (!isAdded || isDetached) return // 检查 Fragment 是否已附加到 Activity

        device.register {
            it.onSuccess {
                if (isAdded && !isDetached) {
                    device.setHistoryTag(getHistoryTag()) {}
                    device.setLevel(0)
                    device.setIsJustRegister(true)
                    mDeviceViewModel.updateDevices()
                    CHAPIClientBiz.putKey(
                        cheyKeyToUserKey(
                            device.getKey(),
                            device.getLevel(),
                            device.getNickname()
                        )
                    ) {}
                    activity?.runOnUiThread {
                        mDeviceViewModel.ssmLockLiveData.value = device
                        findNavController().navigateUp()
                        activity?.findViewById<BottomNavigationView>(R.id.bottom_nav)?.setPage(0)
                        navigateToDeviceSettings(device)
                    }
                }
            }
            it.onFailure {
                if (it is ApiClientException) {
                    if (it.statusCode == 0) {
                        it.errorMessage?.let { message ->
                            toastMSG(message)
                        }
                    }
                }
            }
        }
    }

    private fun navigateToDeviceSettings(device: CHDevices) {
        if (isAdded && !isDetached) {
            when (device) {
                is CHSesame2 -> {
                    device.configureLockPosition(0, 90) {}
                    safeNavigate(R.id.action_to_SSM2SetAngleFG)
                }
                is CHSesame5 -> safeNavigate(R.id.action_to_SSM2SetAngleFG)
                is CHHub3 -> {
                    val config = WebViewConfig(
                        scene = "wifi-module",
                        params = mapOf(
                            "deviceUUID" to device.deviceId.toString().uppercase(),
                            "keyLevel" to device.getLevel().toString()
                        )
                    )
                    safeNavigate(R.id.action_to_webViewFragment, config.toBundle())
                }
                is CHWifiModule2 -> safeNavigate(R.id.to_WM2SettingFG)
                is CHSesameBiometricDevice -> safeNavigate(actionId = R.id.to_SesameTouchProSettingFG)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        CHBleManager.delegate = null
    }
}
