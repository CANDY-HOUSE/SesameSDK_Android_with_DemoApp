package co.utils.recycle

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.databinding.SesameDevicesListLayoutBinding
import co.candyhouse.app.tabs.account.cheyKeyToUserKey
import co.candyhouse.app.tabs.devices.hub3.recycle.Hub3ItemView
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import co.candyhouse.app.tabs.devices.model.CHDeviceViewModel
import co.candyhouse.app.tabs.devices.ssm2.createOpensensorStateText
import co.candyhouse.app.tabs.devices.ssm2.getLevel
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.app.tabs.devices.ssm2.getRank
import co.candyhouse.app.tabs.devices.ssm2.setRank
import co.candyhouse.server.CHLoginAPIManager
import co.candyhouse.sesame.ble.os3.CHSesameBiometricDevice
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.device.CHDeviceLoginStatus
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.open.device.CHProductModel
import co.candyhouse.sesame.open.device.CHSesame2
import co.candyhouse.sesame.open.device.CHSesame5
import co.candyhouse.sesame.open.device.CHSesameBike
import co.candyhouse.sesame.open.device.CHSesameBike2
import co.candyhouse.sesame.open.device.CHSesameBot
import co.candyhouse.sesame.open.device.CHSesameBot2
import co.candyhouse.sesame.open.device.CHSesameConnector
import co.candyhouse.sesame.open.device.CHSesameOpenSensorMechStatus
import co.candyhouse.sesame.open.device.CHWifiModule2
import co.candyhouse.sesame.open.device.CHWifiModule2NetWorkStatus
import co.candyhouse.sesame.open.device.OpenSensorData
import co.utils.SharedPreferencesUtils
import co.utils.UserUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class DeviceListAdapter(
    private val mDeviceViewModel: CHDeviceViewModel,
    private val onDeviceClick: (CHDevices) -> Unit,
    private val callBackHub3: (CHHub3, IrRemote) -> Unit,
) : GenericAdapter<CHDevices>(mDeviceViewModel.myChDevices.value) {

    override fun onItemMoveFinished() {
        super.onItemMoveFinished()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                mDeviceViewModel.myChDevices.value.forEachIndexed { index, chDevices ->
                    chDevices.setRank(index)
                }
                CHLoginAPIManager.upLoadKeys(mDeviceViewModel.myChDevices.value.map {
                    cheyKeyToUserKey(it.getKey(), it.getLevel(), it.getNickname(), it.getRank())
                }) {}
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun getLayoutId(position: Int, obj: CHDevices): Int {
        return R.layout.sesame_devices_list_layout
    }

    override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder {
        return SesameViewHolder(view)
    }

    companion object {
        private val expandStates = mutableMapOf<String, Boolean>()

        fun getExpandState(deviceId: String): Boolean = expandStates[deviceId] ?: false
        fun setExpandState(deviceId: String, state: Boolean) {
            expandStates[deviceId] = state
        }

        var CHDevices.expanded: Boolean
            get() = getExpandState(this.deviceId.toString())
            set(value) = setExpandState(this.deviceId.toString(), value)

        fun CHDevices.setExpandState(yesOrNo: Boolean) {
            expanded = yesOrNo
        }
    }

    inner class SesameViewHolder(val view: View) : RecyclerView.ViewHolder(view), Binder<CHDevices> {
        private val binding = SesameDevicesListLayoutBinding.bind(view)

        override fun bind(device: CHDevices, pos: Int) {
            bindDevice(device)
            view.setOnClickListener { onDeviceClick(device) }
        }

        private fun bindDevice(device: CHDevices) {
            resetViews()

            binding.nickName.text = device.getNickname()

            when (device) {
                is CHHub3 -> configureHub3(device)
                is CHWifiModule2 -> configureWifiModule2(device)
                is CHSesame5, is CHSesame2 -> configureLock(device)
                else -> configureDefault(device)
            }
        }

        private fun resetViews() {
            binding.apply {
                bleStatus.visibility = View.GONE
                expandFL.visibility = View.GONE
                expandFLSubView.visibility = View.GONE
                ssmLockView.visibility = View.GONE
                ssmBikeBotView.visibility = View.GONE
                openSensorStatus.visibility = View.GONE
            }
        }

        private fun configureHub3(device: CHHub3) {
            binding.apply {
                blImg.visibility = View.GONE
                updateWifiStatus(device)

                setBatteryStatus(device.userKey?.stateInfo?.batteryPercentage ?: -1, true)

                setupExpandableView(device, 35) {
                    val param = device.deviceId.toString().uppercase(Locale.getDefault())
                    val irRemoteList = mDeviceViewModel.getIrRemoteList(param)
                    Hub3ItemView(irRemoteList) { bot2Item ->
                        callBackHub3(device, bot2Item)
                    }
                }
            }
        }

        private fun configureWifiModule2(device: CHWifiModule2) {
            binding.apply {
                blImg.visibility = View.GONE
                updateWifiStatus(device)
                batteryContain.visibility = View.GONE
                batteryPercent.visibility = View.GONE
            }
        }

        private fun configureLock(device: CHDevices) {
            binding.apply {
                updateConnectionStatus(device)

                updateBatteryStatus(device)

                ssmLockView.visibility = View.VISIBLE
                ssmLockView.setLockImage(device)
                ssmLockView.setOnClickListener {
                    CHDeviceManager.vibrateDevice(view)
                    when (device) {
                        is CHSesame5 -> device.toggle(historytag = UserUtils.getUserIdWithByte()) {
                            it.onSuccess { }
                        }

                        is CHSesame2 -> device.toggle() { it.onSuccess { } }
                    }
                }

                updateBleStatusVisibility(device)
            }
        }

        private fun configureDefault(device: CHDevices) {
            binding.apply {
                if (device.getLevel() == 2) {
                    device.deviceShadowStatus = null
                }

                updateConnectionStatus(device)

                updateBatteryStatus(device)

                updateBleStatusVisibility(device)

                if (device.productModel == CHProductModel.SesameBot2) {
                    setupExpandableView(device, 90) {
                        Bot2ItemView(getScriptList(view, device as CHSesameBot2)) { bot2Item ->
                            device.click(bot2Item.id.toUByte()) { }
                        }
                    }
                }

                if (device is CHSesameBiometricDevice) {
                    handleBiometricDevice(device)
                } else {
                    ssmBikeBotView.visibility = View.VISIBLE
                    ssmBikeBotView.setLockImage(device)
                    ssmBikeBotView.setOnClickListener {
                        CHDeviceManager.vibrateDevice(view)
                        handleBikeBotViewClick(device)
                    }
                }
            }
        }

        private fun updateWifiStatus(device: CHDevices) {
            val isWifiConnect = (device.mechStatus as? CHWifiModule2NetWorkStatus)?.isIOTWork ?: false

            binding.wifiImg.setImageResource(
                if (isWifiConnect) R.drawable.wifi_green else R.drawable.wifi_grey
            )
        }

        private fun updateConnectionStatus(device: CHDevices) {
            binding.apply {
                blImg.visibility = View.VISIBLE
                val isBleConnect = device.deviceStatus.value == CHDeviceLoginStatus.Login
                blImg.setImageResource(
                    if (isBleConnect) R.drawable.bl_green else R.drawable.bl_grey
                )

                val isWifiConnect = device.deviceShadowStatus?.let { it.value == CHDeviceLoginStatus.Login } ?: false
                wifiImg.setImageResource(
                    if (isWifiConnect) R.drawable.wifi_green else R.drawable.wifi_grey
                )
            }
        }

        private fun updateBatteryStatus(device: CHDevices) {
            val batteryLevel = device.mechStatus?.getBatteryPrecentage() ?: device.userKey?.stateInfo?.batteryPercentage ?: -1
            setBatteryStatus(batteryLevel, false)
        }

        private fun setBatteryStatus(level: Int, isHub3: Boolean) {
            binding.apply {
                val visibility = if (level == -1) View.GONE else View.VISIBLE
                batteryContain.visibility = visibility
                batteryPercent.visibility = visibility

                batteryPercent.text = "$level%"
                btnPecent.apply {
                    progressDrawable = ContextCompat.getDrawable(
                        itemView.context,
                        if (!isHub3 && level < 15) R.drawable.progress_red else R.drawable.progress_blue
                    )
                    progress = level
                }
            }
        }

        private fun updateBleStatusVisibility(device: CHDevices) {
            binding.bleStatus.text = device.deviceStatus.toString()
            binding.bleStatus.visibility = when {
                device is CHSesameConnector -> {
                    if (device.deviceStatus.value == CHDeviceLoginStatus.Login ||
                        device.deviceStatus == CHDeviceStatus.NoBleSignal ||
                        device.deviceStatus == CHDeviceStatus.ReceivedAdV
                    ) View.GONE else View.VISIBLE
                }

                else -> {
                    if (device.deviceStatus.value == CHDeviceLoginStatus.Login ||
                        device.deviceStatus == CHDeviceStatus.NoBleSignal
                    ) View.GONE else View.VISIBLE
                }
            }
        }

        private fun setupExpandableView(
            device: CHDevices,
            heightDp: Int,
            adapterProvider: () -> RecyclerView.Adapter<*>
        ) {
            binding.apply {
                expandFLSubView.adapter = adapterProvider()
                expandFL.visibility = View.VISIBLE
                expandFL.layoutParams.height = (heightDp * expandFL.resources.displayMetrics.density).toInt()
                dispatchExpanded(device)
                expandFL.setOnClickListener {
                    device.setExpandState(!device.expanded)
                    dispatchExpanded(device)
                }
            }
        }

        private fun handleBikeBotViewClick(device: CHDevices) {
            when (device) {
                is CHSesameBike -> device.unlock { it.onSuccess { } }
                is CHSesameBike2 -> device.unlock { it.onSuccess { } }
                is CHSesameBot -> device.click { it.onSuccess { } }
                is CHSesameBot2 -> {
                    val bot2ScriptCurIndexKey = "${device.deviceId}_ScriptIndex"
                    val index = SharedPreferencesUtils.preferences.getInt(bot2ScriptCurIndexKey, 0)
                    device.click(index.toUByte()) {}
                }
            }
        }

        private fun handleBiometricDevice(device: CHSesameBiometricDevice) {
            binding.apply {
                blImg.visibility = View.GONE
                ssmBikeBotView.visibility = View.GONE

                if (device.productModel == CHProductModel.SSMOpenSensor || device.productModel == CHProductModel.SSMOpenSensor2) {
                    val statusText = (device.mechStatus as? CHSesameOpenSensorMechStatus)?.data?.let { data ->
                        runCatching {
                            val sensorData = OpenSensorData.fromByteArray(data)
                            createOpensensorStateText(sensorData.Status, sensorData.TimeStamp)
                        }.getOrNull()
                    } ?: device.userKey?.stateInfo?.let { info ->
                        val status = info.CHSesame2Status
                        val timestamp = info.timestamp
                        if (status != null && timestamp != null) {
                            createOpensensorStateText(status, timestamp)
                        } else {
                            null
                        }
                    }

                    statusText?.let {
                        openSensorStatus.text = it
                        openSensorStatus.visibility = View.VISIBLE
                    }
                } else {
                    openSensorStatus.visibility = View.GONE
                }
            }
        }

        private fun getScriptList(view: View, data: CHSesameBot2): MutableList<BotItem> {
            return data.scripts.events.mapIndexed { index, event ->
                val script = "${view.resources.getString(R.string.click_script)} ${String(event.name, Charsets.UTF_8)}"
                BotItem(script, index)
            }.toMutableList()
        }

        private fun dispatchExpanded(device: CHDevices) {
            binding.apply {
                imageArrow.rotation = if (device.expanded) 90f else 0f
                expandFLSubView.visibility = if (device.expanded) View.VISIBLE else View.GONE
            }
        }
    }
}