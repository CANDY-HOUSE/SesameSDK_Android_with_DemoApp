package co.candyhouse.app.tabs.devices

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseFG

import co.candyhouse.app.tabs.devices.model.CHDeviceViewModel
import co.candyhouse.app.tabs.devices.ssm2.*
import co.candyhouse.app.tabs.devices.ssm2.setting.angle.*

import co.candyhouse.sesame.open.device.*
import co.utils.SharedPreferencesUtils
import co.utils.recycle.GenericAdapter
import co.utils.recycle.ItemTouchHelperAdapter
import co.utils.recycle.SimpleItemTouchHelperCallback
import kotlinx.android.synthetic.main.fg_devicelist.*
import java.util.*

@SuppressLint("SetTextI18n") class DeviceListFG : BaseFG(R.layout.fg_devicelist) {
    private val mDeviceViewModel: CHDeviceViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        L.d("hcia", "onCreate:")
        mDeviceViewModel.updateDevices()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        L.d("hcia", "onViewCreated:")
        swiperefresh.setOnRefreshListener { mDeviceViewModel.refleshDevices() }
        leaderboard_list.setEmptyView(empty_view)
        leaderboard_list.adapter = object : GenericAdapter<Any>(mDeviceViewModel.myChDevices.value) {
            override fun onItemMoveFinished() {
                super.onItemMoveFinished()
                mDeviceViewModel.myChDevices.value.forEachIndexed { index, device ->
                    device.setRank(index)
                }

            }

            override fun getLayoutId(position: Int, obj: Any): Int {
                return when (obj) {
                    is CHWifiModule2 -> R.layout.wm2_layout
                    is CHSesame5, is CHSesame2 -> R.layout.sesame_layout
                    else /*Bike,bot,touch*/ -> R.layout.ssmbike_layout
                }
            }

            override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder {
                return when (viewType) {
                    R.layout.wm2_layout -> object : RecyclerView.ViewHolder(view), Binder<CHWifiModule2> {
                        val customName: TextView = view.findViewById(R.id.title)
                        val wifiImg: ImageView = view.findViewById(R.id.wifi_img)
                        override fun bind(data: CHWifiModule2, pos: Int) {
                            mDeviceViewModel.ssmosLockDelegates[data] = object : CHWifiModule2Delegate {
                                override fun onMechStatus(device: CHDevices) {
                                    setupWm2(data)
                                }


                                override fun onBleDeviceStatusChanged(device: CHDevices, status: CHDeviceStatus, shadowStatus: CHDeviceStatus?) {
                                    setupWm2(data)
                                }
                            }
                            setupWm2(data)
                            view.setOnClickListener {
                                mDeviceViewModel.ssmLockLiveData.value = data
                                findNavController().navigate(R.id.to_WM2SettingFG)
                            }
                        }

                        private fun setupWm2(wm2: CHWifiModule2) {
                            customName.text = wm2.getNickname()
                            wifiImg.setImageResource(if ((wm2.mechStatus as? CHWifiModule2NetWorkStatus) ?.isIOTWork == true) R.drawable.wifi_green else R.drawable.wifi_grey)
                        }
                    }
                    R.layout.sesame_layout -> /*CHSesame2*/ object : RecyclerView.ViewHolder(view), Binder<CHDevices> {
                        val ssmView: SSMCellView = view.findViewById(R.id.ssmView)
                        val customName: TextView = view.findViewById(R.id.title)
                        val sesame2Status: TextView = view.findViewById(R.id.sub_title)
                        val shadowStatusTxt: TextView = view.findViewById(R.id.sub_title_2)
                        val batteryPercent: TextView = view.findViewById(R.id.battery_percent)
                        val battery: ImageView = view.findViewById(R.id.battery)
                        val blImg: ImageView = view.findViewById(R.id.bl_img)
                        val wifiImg: ImageView = view.findViewById(R.id.wifi_img)
                        val btnPercent: ProgressBar = view.findViewById(R.id.btn_pecent)
                        override fun bind(data: CHDevices, pos: Int) {
                            mDeviceViewModel.ssmosLockDelegates[data] = object : CHDeviceStatusDelegate {
                                override fun onMechStatus(device: CHDevices) {
                                    setupSSMCell(data)
                                }

                                override fun onBleDeviceStatusChanged(device: CHDevices, status: CHDeviceStatus, shadowStatus: CHDeviceStatus?) {
                                    setupSSMCell(data)
                                    if (SharedPreferencesUtils.nickname?.contains(BuildConfig.testname) == true) {
                                        if(device.deviceStatus  == CHDeviceStatus.BleLogining){
                                            device.setTestLoginCount(device.getTestLoginCount()+1)
                                        }
                                    }
                                }
                            }
                            setupSSMCell(data)
                            view.setOnClickListener {
                                mDeviceViewModel.ssmLockLiveData.value = data
                                when (data.productModel) {
                                    CHProductModel.WM2 -> TODO()
                                    CHProductModel.SS2, CHProductModel.SS4 -> findNavController().navigate(if (data.getLevel() == 2) R.id.action_deviceListPG_to_SSM2SettingFG else R.id.action_deviceListPG_to_mainRoomFG)
                                    CHProductModel.SesameBot1 -> findNavController().navigate(R.id.action_deviceListPG_to_SesameBot2SettingFG)
                                    CHProductModel.BiKeLock -> findNavController().navigate(R.id.action_deviceListPG_to_sesameBikeSettingFG)
                                    CHProductModel.BiKeLock2 -> findNavController().navigate(R.id.action_deviceListPG_to_sesameBikeSettingFG)
                                    CHProductModel.SS5, CHProductModel.SS5PRO -> findNavController().navigate(if (data.getLevel() == 2) R.id.to_Sesame5SettingFG else R.id.action_deviceListPG_to_mainRoomSS5FG)
                                    CHProductModel.SSMOpenSensor -> findNavController().navigate(R.id.to_SesameOpenSensorSettingFG)
                                    CHProductModel.SSMTouchPro -> findNavController().navigate(R.id.to_SesameTouchProSettingFG)
                                    CHProductModel.SSMTouch -> findNavController().navigate(R.id.to_SesameTouchProSettingFG)
                                    CHProductModel.BLEConnector -> findNavController().navigate(R.id.to_SesameTouchProSettingFG)
                                }
                            }
                        }

                        private fun setupSSMCell(sesame: CHDevices) {
                            ssmView.setOnClickListener {
                                (sesame as? CHSesame5)?.toggle { }
                                (sesame as? CHSesame2)?.toggle { }
//                                sesame.toggle {
//                                    it.onSuccess {
//                                        getLastKnownLocation(ssmView.context) {
//                                            it.onSuccess {
//                                                CHLoginAPIManager.putKeyInfor(CHDeviceInfor(sesame.deviceId.toString().uppercase(), sesame.productModel.productType().toString(), it.data?.longitude.toString(), it.data?.latitude.toString())) {}
//                                            }
//                                        }
//                                    }
//                                }
                            }
                            ssmView.setLockImage(sesame)
                            blImg.setImageResource(if (sesame.deviceStatus.value == CHDeviceLoginStatus.Login) R.drawable.bl_green else R.drawable.bl_grey)
//                            L.d("hcia", "${sesame.getNickname()} deviceShadowStatus:" + sesame.deviceShadowStatus)
                            wifiImg.setImageResource(if (sesame.deviceShadowStatus?.value == CHDeviceLoginStatus.Login) R.drawable.wifi_green else R.drawable.wifi_grey)
                            batteryPercent.visibility = if (sesame.mechStatus == null) View.GONE else View.VISIBLE
                            batteryPercent.text = sesame.mechStatus?.getBatteryPrecentage().toString() + "%"
                            battery.visibility = if (sesame.mechStatus == null) View.GONE else View.VISIBLE
                            btnPercent.progressDrawable = ContextCompat.getDrawable(itemView.context, if ((sesame.mechStatus?.getBatteryPrecentage() ?: 0) < 15) R.drawable.progress_red else R.drawable.progress_blue)
                            btnPercent.progress = sesame.mechStatus?.getBatteryPrecentage() ?: 0
                            customName.text = sesame.getNickname()
                            sesame2Status.text = sesame.deviceStatus.toString()
                            sesame2Status.visibility = if (sesame.deviceStatus.value == CHDeviceLoginStatus.Login || sesame.deviceStatus == CHDeviceStatus.NoBleSignal) View.GONE else View.VISIBLE
                            shadowStatusTxt.text = sesame.deviceShadowStatus.toString()
                            shadowStatusTxt.setTextColor(ContextCompat.getColor(view.context, if (sesame.deviceShadowStatus?.value == CHDeviceLoginStatus.Login) R.color.unlock_blue else R.color.lock_red))


                            if (SharedPreferencesUtils.nickname?.contains(BuildConfig.testname) == true) {
                                sesame2Status.visibility =  View.VISIBLE
                                if(sesame.loginTimestamp != null){
                                    val testct =sesame.getTestLoginCount()
                                    val timeMinus = sesame.loginTimestamp!!.minus(sesame.deviceTimestamp!!)
                                    sesame2Status.text ="[login:${testct}][gap:${timeMinus}]"+ Date(sesame.deviceTimestamp!!*1000).toLocaleString() + "  ${sesame.deviceStatus}"
                                    sesame.getNickname()
                                }
                            }


                        }

                    }
                    else /*R.layout.ssmbike_layout*/ -> object : RecyclerView.ViewHolder(view), Binder<CHDevices> {
                        val ssmView: SSMBikeCellView = view.findViewById(R.id.ssmView)
                        val customName: TextView = view.findViewById(R.id.title)
                        val sesame2Status: TextView = view.findViewById(R.id.sub_title)
                        val shadowStatusTxt: TextView = view.findViewById(R.id.sub_title_2)
                        val battery_percent: TextView = view.findViewById(R.id.battery_percent)
                        val bl_img: ImageView = view.findViewById(R.id.bl_img)
                        val wifi_img: ImageView = view.findViewById(R.id.wifi_img)
                        val btn_pecent: ProgressBar = view.findViewById(R.id.btn_pecent)
                        val battery_contain: View = view.findViewById(R.id.battery_contain)
                        override fun bind(data: CHDevices, pos: Int) {

                            view.setOnClickListener {
                                mDeviceViewModel.ssmLockLiveData.value = data
                                when (data.productModel) {
                                    CHProductModel.WM2 -> TODO()
                                    CHProductModel.SS2, CHProductModel.SS4 -> findNavController().navigate(if (data.getLevel() == 2) R.id.action_deviceListPG_to_SSM2SettingFG else R.id.action_deviceListPG_to_mainRoomFG)
                                    CHProductModel.SesameBot1 -> findNavController().navigate(R.id.action_deviceListPG_to_SesameBot2SettingFG)
                                    CHProductModel.BiKeLock -> findNavController().navigate(R.id.action_deviceListPG_to_sesameBikeSettingFG)
                                    CHProductModel.BiKeLock2 -> findNavController().navigate(R.id.action_deviceListPG_to_sesameBikeSettingFG)
                                    CHProductModel.SS5, CHProductModel.SS5PRO -> findNavController().navigate(if (data.getLevel() == 2) R.id.to_Sesame5SettingFG else R.id.action_deviceListPG_to_mainRoomSS5FG)
                                    CHProductModel.SSMOpenSensor -> findNavController().navigate(R.id.to_SesameOpenSensorSettingFG)
                                    CHProductModel.SSMTouchPro -> findNavController().navigate(R.id.to_SesameTouchProSettingFG)
                                    CHProductModel.SSMTouch -> findNavController().navigate(R.id.to_SesameTouchProSettingFG)
                                    CHProductModel.BLEConnector -> findNavController().navigate(R.id.to_SesameTouchProSettingFG)
                                }
                            }
                            mDeviceViewModel.ssmosLockDelegates.put(data, object : CHDeviceStatusDelegate {
                                override fun onBleDeviceStatusChanged(device: CHDevices, status: CHDeviceStatus, shadowStatus: CHDeviceStatus?) {
                                    setDevice(data)
                                }

                                override fun onMechStatus(device: CHDevices) {
                                    setDevice(data)
                                }
                            })
                            setDevice(data)
                        }

                        private fun setDevice(ssmBike: CHDevices) {
                            ssmView.setOnClickListener {
                                (ssmBike as? CHSesameBike)?.unlock { }
                                (ssmBike as? CHSesameBike2)?.unlock { }
                                (ssmBike as? CHSesameBot)?.click { }
                                (ssmBike as? CHSesame5)?.toggle { }
                                (ssmBike as? CHSesame2)?.toggle { }
                            }
                            sesame2Status.text = ssmBike.deviceStatus.toString()
                            bl_img.setImageResource(if (ssmBike.deviceStatus.value == CHDeviceLoginStatus.Login) R.drawable.bl_green else R.drawable.bl_grey)
                            customName.text = ssmBike.getNickname()
                            ssmView.setLockImage(ssmBike)
                            battery_percent.text = ssmBike.mechStatus?.getBatteryPrecentage().toString() + "%"
                            battery_percent.visibility = if (ssmBike.mechStatus == null) View.GONE else View.VISIBLE
                            battery_contain.visibility = if (ssmBike.mechStatus == null) View.GONE else View.VISIBLE
                            sesame2Status.text = ssmBike.deviceStatus.toString()
                            if(ssmBike is CHSesameConnector){
                                sesame2Status.visibility = if (ssmBike.deviceStatus.value == CHDeviceLoginStatus.Login || ssmBike.deviceStatus == CHDeviceStatus.NoBleSignal|| ssmBike.deviceStatus == CHDeviceStatus.ReceivedAdV) View.GONE else View.VISIBLE
                            }else{
                                sesame2Status.visibility = if (ssmBike.deviceStatus.value == CHDeviceLoginStatus.Login || ssmBike.deviceStatus == CHDeviceStatus.NoBleSignal) View.GONE else View.VISIBLE
                            }

                            shadowStatusTxt.text = ssmBike.deviceShadowStatus.toString()
                            bl_img.setImageResource(if (ssmBike.deviceStatus.value == CHDeviceLoginStatus.Login) R.drawable.bl_green else R.drawable.bl_grey)
//                            L.d("hcia", "${ssmBike.getNickname()} deviceShadowStatus:" + ssmBike.deviceShadowStatus)
                            wifi_img.setImageResource(if (ssmBike.deviceShadowStatus?.value == CHDeviceLoginStatus.Login) R.drawable.wifi_green else R.drawable.wifi_grey)
                            customName.text = ssmBike.getNickname()
                            btn_pecent.progressDrawable = ContextCompat.getDrawable(itemView.context, if ((ssmBike.mechStatus?.getBatteryPrecentage() ?: 0) < 15) R.drawable.progress_red else R.drawable.progress_blue)
                            btn_pecent.progress = ssmBike.mechStatus?.getBatteryPrecentage() ?: 0



                            when (ssmBike) {
                                is CHSesameTouchPro -> {
                                    bl_img.visibility = View.GONE
                                    battery_percent.visibility = View.GONE
                                    btn_pecent.visibility = View.GONE
                                    battery_contain.visibility = View.GONE
                                }
                                is CHSesameSensor -> {
                                    bl_img.visibility = View.GONE
//                                    wifi_img.visibility = View.GONE
                                    battery_percent.visibility = View.GONE
                                    btn_pecent.visibility = View.GONE
                                    battery_contain.visibility = View.GONE
                                }
                                else -> {
//                                    bl_img.visibility = View.VISIBLE
//                                    btn_pecent.visibility = View.VISIBLE
//                                    battery_percent.visibility = View.VISIBLE
//                                    battery_contain.visibility = View.VISIBLE
                                }
                            }

                        }
                    }
                }
            }
        }

        ItemTouchHelper(SimpleItemTouchHelperCallback(leaderboard_list.adapter as ItemTouchHelperAdapter)).attachToRecyclerView(leaderboard_list)

        mDeviceViewModel.neeReflesh.observe(viewLifecycleOwner) { isR ->
            swiperefresh?.isRefreshing = isR
            if (!isR) {
                leaderboard_list?.adapter?.notifyDataSetChanged()
            }
        }
    }
}
