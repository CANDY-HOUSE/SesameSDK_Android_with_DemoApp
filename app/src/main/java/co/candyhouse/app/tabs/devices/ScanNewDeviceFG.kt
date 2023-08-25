package co.candyhouse.app.tabs.devices

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG

import co.candyhouse.app.tabs.devices.model.CHDeviceViewModel
import co.candyhouse.app.tabs.devices.ssm2.*
import co.candyhouse.sesame.open.device.CHWifiModule2
import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.open.CHBleManagerDelegate
import co.candyhouse.sesame.open.device.*
import co.utils.recycle.GenericAdapter
import kotlinx.android.synthetic.main.fg_rg_device.*
import kotlinx.android.synthetic.main.activity_main.*
import android.bluetooth.BluetoothManager
import android.content.Context
import co.candyhouse.app.base.setPage
import co.utils.L

import co.utils.alertview.fragments.toastMSG


@SuppressLint("SetTextI18n") class ScanNewDeviceFG : BaseDeviceFG(R.layout.fg_rg_device) {

    private var mDeviceList = ArrayList<CHDevices>()
    private val mDeviceViewModel: CHDeviceViewModel by activityViewModels()

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter.enable()

        top_back_img.setOnClickListener { findNavController().navigateUp() }
        leaderboard_list.setEmptyView(empty_view)
        leaderboard_list.adapter = object : GenericAdapter<CHDevices>(mDeviceList) {
            override fun getLayoutId(position: Int, obj: CHDevices): Int {
                return R.layout.cell_device_unregist
            }

            override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder {
                return object : RecyclerView.ViewHolder(view), Binder<CHDevices> {
                    override fun bind(data: CHDevices, pos: Int) {
                        (itemView.findViewById(R.id.title) as TextView).text = "${data.getDistance()} cm"
                        (itemView.findViewById(R.id.title_txt) as TextView).text = data.deviceId.toString().uppercase()
                        (itemView.findViewById(R.id.subtitle_txt) as TextView).text = data.deviceStatus.toString()
                        (itemView.findViewById(R.id.sub_title) as TextView).text = data.getNickname()
                        itemView.setOnClickListener {
                            L.d("device click",data.productModel.deviceModel()+"---"+data.productModel.deviceModelName()+"-name:"+data::class.java.simpleName)
                            data.connect {
                                it.onSuccess {
                                    L.d("connect","onsuccess")
                                    data.deviceStatus=CHDeviceStatus.ReadyToRegister

                                }
                                it.onFailure {res->
                                    L.d("connect","onFailure${res.message}")
                                }
                            }
                            doRegisterDevice(data)
                            data.delegate = object : CHDeviceStatusDelegate {
                                override fun onBleDeviceStatusChanged(device: CHDevices, status: CHDeviceStatus, shadowStatus: CHDeviceStatus?) {
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

          //     L.d("devices size",devices.size.toString())
                mDeviceList.clear()
                mDeviceList.addAll(devices.filter { it.rssi != null }
//                    .filter { it.rssi!! > -65 }///註冊列表只顯示距離近的
                )
                mDeviceList.sortBy { it.getDistance() }
                mDeviceList.firstOrNull()?.connect { }
                leaderboard_list.post((leaderboard_list.adapter as GenericAdapter<*>)::notifyDataSetChanged)
            }
        }
    }


    private fun doRegisterDevice(device: CHDevices) {
        device.register {
            it.onSuccess {
                device.setHistoryTag(getHistoryTag()) {}
                device.setLevel(0)
                device.setIsJustRegister(true)
                L.d("doRegisterDevice","onSuccess")
                mDeviceViewModel.updateDevices()
                activity?.runOnUiThread {
                    mDeviceViewModel.ssmLockLiveData.value = device
                    findNavController().navigateUp()

                    // 注册成功后依照业务跳转
                    (device as? CHWifiModule2)?.let {
                        findNavController().navigate(R.id.to_WM2SettingFG)
                    }
                    (device as? CHSesame2)?.let {
                        device.configureLockPosition(0, 90) {}
                        findNavController().navigate(R.id.action_to_SSM2SetAngleFG)
                    }
                    (device as? CHSesame5)?.let {
                        findNavController().navigate(R.id.action_to_SSM2SetAngleFG)
                    }
                    (device as? CHSesameTouchPro)?.let {
                        findNavController().navigate(R.id.to_SesameTouchProSettingFG)
                    }
                }
            }
            it.onFailure { it ->
                L.d("doRegisterDevice","fail${it.message}")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        CHBleManager.delegate = null
    }
}
