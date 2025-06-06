package co.utils.recycle

import android.annotation.SuppressLint
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import candyhouse.sesameos.ir.base.IrRemote
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.R
import co.candyhouse.app.tabs.account.cheyKeyToUserKey
import co.candyhouse.app.tabs.devices.hub3.recycle.Hub3ItemView
import co.candyhouse.app.tabs.devices.model.CHDeviceViewModel
import co.candyhouse.app.tabs.devices.ssm2.getLevel
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.app.tabs.devices.ssm2.getRank
import co.candyhouse.app.tabs.devices.ssm2.getTestLoginCount
import co.candyhouse.app.tabs.devices.ssm2.parseOpensensorState
import co.candyhouse.app.tabs.devices.ssm2.setRank
import co.candyhouse.app.tabs.devices.ssm2.setting.angle.SSMBikeCellView
import co.candyhouse.app.tabs.devices.ssm2.setting.angle.SSMCellView
import co.candyhouse.server.CHLoginAPIManager
import co.candyhouse.sesame.ble.os3.biometric.face.CHSesameFace
import co.candyhouse.sesame.ble.os3.biometric.facePro.CHSesameFacePro
import co.candyhouse.sesame.ble.os3.biometric.touch.CHSesameTouch
import co.candyhouse.sesame.ble.os3.biometric.touchPro.CHSesameTouchPro
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
import co.candyhouse.sesame.utils.L
import co.utils.SharedPreferencesUtils
import co.utils.UserUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
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
        return when (obj) {
            is CHHub3 -> R.layout.hub3_layout
            is CHWifiModule2 -> R.layout.wm2_layout
            is CHSesame5, is CHSesame2 -> R.layout.sesame_layout
            else -> R.layout.ssmbike_layout
        }
    }

    override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.wm2_layout -> Wm2ViewHolder(view)
            R.layout.sesame_layout -> SesameViewHolder(view)
            R.layout.hub3_layout -> Hub3ViewHolder(view)
            else -> SsmBikeViewHolder(view)
        }
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

    inner class Hub3ViewHolder(val view: View) : RecyclerView.ViewHolder(view), Binder<CHHub3> {
        private val customName: TextView = view.findViewById(R.id.title)
        private val wifiImg: ImageView = view.findViewById(R.id.wifi_img)
        private val imageArrow: ImageView = view.findViewById(R.id.imageArrow)
        private val flView1: FrameLayout = view.findViewById(R.id.flView1)
        private val hub3_ll_title: LinearLayout = view.findViewById(R.id.hub3_ll_title)
        private val hub3_rv: RecyclerView = view.findViewById(R.id.hub3_rv)

        override fun bind(data: CHHub3, pos: Int) {
            setViewData(data)
        }

        private fun setViewData(hub3Device: CHHub3) {
            customName.text = hub3Device.getNickname()
            hub3_ll_title.setOnClickListener { onDeviceClick(hub3Device) }
            val isWifiConnect =
                (hub3Device.mechStatus as? CHWifiModule2NetWorkStatus)?.isIOTWork == true
            wifiImg.setImageResource(if (isWifiConnect) R.drawable.wifi_green else R.drawable.wifi_grey)

            val param = hub3Device.deviceId.toString().uppercase(Locale.getDefault())
            val irRemoteList = mDeviceViewModel.getIrRemoteList(param)

            if (irRemoteList.isNotEmpty()) {
                flView1.visibility = VISIBLE
                hub3_rv.adapter = Hub3ItemView(irRemoteList) { bot2Item ->
                    L.d("sf", "首页红外线列表子控件：" + bot2Item.alias + " " + bot2Item.uuid)
                    callBackHub3(hub3Device, bot2Item)
                }

                dispatchExpanded(hub3Device)

                flView1.setOnClickListener {
                    hub3Device.let { device ->
                        device.setExpandState(!device.expanded)
                        dispatchExpanded(device)
                    }
                }
            } else {
                flView1.visibility = GONE
            }
        }

        private fun dispatchExpanded(hub3Device: CHHub3) {
            if (hub3Device.expanded) {
                imageArrow.rotation = 90f
                hub3_rv.visibility = VISIBLE
            } else {
                imageArrow.rotation = 0f
                hub3_rv.visibility = GONE
            }
        }
    }

    inner class Wm2ViewHolder(val view: View) : RecyclerView.ViewHolder(view),
        Binder<CHWifiModule2> {
        private val customName: TextView = view.findViewById(R.id.title)
        private val wifiImg: ImageView = view.findViewById(R.id.wifi_img)

        override fun bind(data: CHWifiModule2, pos: Int) {
            setupWm2(data)
            view.setOnClickListener { onDeviceClick(data) }
        }

        private fun setupWm2(wm2: CHWifiModule2) {
            val ls = (wm2.mechStatus as? CHWifiModule2NetWorkStatus)?.isIOTWork == true
            L.d("updateHub3", wm2.getNickname() + "---" + this.hashCode() + "---" + ls)
            customName.text = wm2.getNickname()
            wifiImg.setImageResource(
                if ((wm2.mechStatus as? CHWifiModule2NetWorkStatus)?.isIOTWork == true)
                    R.drawable.wifi_green else R.drawable.wifi_grey
            )
        }
    }

    inner class SesameViewHolder(val view: View) : RecyclerView.ViewHolder(view),
        Binder<CHDevices> {
        private val ssmView: SSMCellView = view.findViewById(R.id.ssmView)
        private val customName: TextView = view.findViewById(R.id.title)
        private val sesame2Status: TextView = view.findViewById(R.id.sub_title)
        private val shadowStatusTxt: TextView = view.findViewById(R.id.sub_title_2)
        private val batteryPercent: TextView = view.findViewById(R.id.battery_percent)
        private val battery: ImageView = view.findViewById(R.id.battery)
        private val blImg: ImageView = view.findViewById(R.id.bl_img)
        private val wifiImg: ImageView = view.findViewById(R.id.wifi_img)
        private val btnPercent: ProgressBar = view.findViewById(R.id.btn_pecent)
        private val battery_contain: View = view.findViewById(R.id.battery_contain)

        override fun bind(data: CHDevices, pos: Int) {
            setupSSMCell(data)
            view.setOnClickListener { onDeviceClick(data) }

        }

        @SuppressLint("SetTextI18n")
        private fun setupSSMCell(sesame: CHDevices) {
            ssmView.visibility = View.VISIBLE
            ssmView.setOnClickListener {
                CHDeviceManager.vibrateDevice(view)
                (sesame as? CHSesame5)?.toggle(historytag = UserUtils.getUserIdWithByte()) {
                    L.d("sf", "CHSesame5")
                    it.onSuccess { }
                }
                (sesame as? CHSesame2)?.toggle() {
                    it.onSuccess { }
                    L.d("sf", "CHSesame2")
                }
            }
            ssmView.setLockImage(sesame)
            val isBleConnect = sesame.deviceStatus.value == CHDeviceLoginStatus.Login
            val isWifiConnect = sesame.deviceShadowStatus?.value == CHDeviceLoginStatus.Login
            blImg.setImageResource(if (isBleConnect) R.drawable.bl_green else R.drawable.bl_grey)
            wifiImg.setImageResource(if (isWifiConnect) R.drawable.wifi_green else R.drawable.wifi_grey)
            batteryPercent.visibility = if (sesame.mechStatus == null) View.GONE else View.VISIBLE
            batteryPercent.text = sesame.mechStatus?.getBatteryPrecentage().toString() + "%"
            battery.visibility = if (sesame.mechStatus == null) View.GONE else View.VISIBLE
            btnPercent.progressDrawable = ContextCompat.getDrawable(
                itemView.context,
                if ((sesame.mechStatus?.getBatteryPrecentage()
                        ?: 0) < 15
                ) R.drawable.progress_red else R.drawable.progress_blue
            )
            btnPercent.progress = sesame.mechStatus?.getBatteryPrecentage() ?: 0
            if (!isBleConnect && !isWifiConnect) {
                battery_contain.visibility = View.GONE
                batteryPercent.visibility = View.GONE
            } else {
                battery_contain.visibility = View.VISIBLE
                batteryPercent.visibility = View.VISIBLE
            }
            customName.text = sesame.getNickname()
            sesame2Status.text = sesame.deviceStatus.toString()
            sesame2Status.visibility =
                if (sesame.deviceStatus.value == CHDeviceLoginStatus.Login || sesame.deviceStatus == CHDeviceStatus.NoBleSignal) View.GONE else View.VISIBLE
            shadowStatusTxt.text = sesame.deviceShadowStatus.toString()
            shadowStatusTxt.setTextColor(
                ContextCompat.getColor(
                    view.context,
                    if (sesame.deviceShadowStatus?.value == CHDeviceLoginStatus.Login) R.color.unlock_blue else R.color.lock_red
                )
            )
            if (BuildConfig.DEBUG) {
                sesame2Status.visibility = View.VISIBLE
                if (sesame.loginTimestamp != null) {
                    val testct = sesame.getTestLoginCount()
                    val timeMinus = sesame.loginTimestamp!!.minus(sesame.deviceTimestamp!!)
                    sesame2Status.text =
                        "[login:${testct}][gap:${timeMinus}]" + Date(sesame.deviceTimestamp!! * 1000).toLocaleString() + "  ${sesame.deviceStatus}"
                }
            }
        }
    }

    inner class SsmBikeViewHolder(val view: View) : RecyclerView.ViewHolder(view),
        Binder<CHDevices> {
        private val ssmView: SSMBikeCellView = view.findViewById(R.id.ssmView)
        private val customName: TextView = view.findViewById(R.id.title)
        private val sesame2Status: TextView = view.findViewById(R.id.sub_title)
        private val shadowStatusTxt: TextView = view.findViewById(R.id.sub_title_2)
        private val batteryPercent: TextView = view.findViewById(R.id.battery_percent)
        private val blImg: ImageView = view.findViewById(R.id.bl_img)
        private val imageArrow: ImageView = view.findViewById(R.id.imageArrow)
        private val wifiImg: ImageView = view.findViewById(R.id.wifi_img)
        private val btnPercent: ProgressBar = view.findViewById(R.id.btn_pecent)
        private val battery_contain: View = view.findViewById(R.id.battery_contain)
        private val llview1: RecyclerView = view.findViewById(R.id.llview1)
        private val flView1: FrameLayout = view.findViewById(R.id.flView1)
        private val centerText: TextView = view.findViewById(R.id.centerText)

        private fun getScriptList(view: View, data: CHSesameBot2): MutableList<BotItem> {
            return data.scripts.events.mapIndexed { index, chSesamebot2Event ->
                val script = "${view.resources.getString(R.string.click_script)} ${
                    String(
                        chSesamebot2Event.name,
                        Charsets.UTF_8
                    )
                }"
                BotItem(script, index)
            }.toMutableList()
        }

        override fun bind(data: CHDevices, pos: Int) {
            setDevice(data)
            view.setOnClickListener {
                onDeviceClick(data)
                if (data.productModel === CHProductModel.SesameBot2) {
                    data.setExpandState(false)
                }
            }
        }

        private fun setDevice(chDevice: CHDevices) {
            imageArrow.visibility =
                if (chDevice.productModel == CHProductModel.SesameBot2) View.VISIBLE else View.GONE
            llview1.visibility = View.GONE
            if (chDevice.productModel === CHProductModel.SesameBot2) {
                imageArrow.rotation = if (chDevice.expanded) 90f else 0f
                llview1.visibility = if (imageArrow.rotation == 90f) View.VISIBLE else View.GONE
                if (chDevice is CHSesameBot2) {
                    llview1.adapter =
                        Bot2ItemView(getScriptList(view, chDevice)) { bot2Item ->
                            chDevice.click(bot2Item.id.toUByte()) { }
                        }
                    imageArrow.visibility = VISIBLE
                } else {
                    L.d("ssmBike", "ssmBike is not CHSesameBot2")
                    imageArrow.visibility = GONE
                }
                flView1.setOnClickListener {
                    (chDevice as? CHSesameBot2)?.let {
                        it.setExpandState(!it.expanded)
                        imageArrow.rotation = if (chDevice.expanded) 90f else 0f
                        llview1.visibility =
                            if (imageArrow.rotation == 90f) View.VISIBLE else View.GONE
                    }
                }
            }
            ssmView.visibility = View.VISIBLE
            ssmView.setOnClickListener {
                CHDeviceManager.vibrateDevice(view)
                (chDevice as? CHSesameBike)?.unlock { it.onSuccess { } }
                (chDevice as? CHSesameBike2)?.unlock { it.onSuccess { } }
                (chDevice as? CHSesameBot)?.click { it.onSuccess { } }
                (chDevice as? CHSesame5)?.toggle(historytag = UserUtils.getUserIdWithByte()) { it.onSuccess { } }
                (chDevice as? CHSesame2)?.toggle() { it.onSuccess { } }
                (chDevice as? CHSesameBot2)?.let { sesameBot2 ->
                    val bot2ScriptCurIndexKey = sesameBot2.deviceId.toString() + "_ScriptIndex"
                    sesameBot2.click(
                        SharedPreferencesUtils.preferences.getInt(
                            bot2ScriptCurIndexKey,
                            0
                        ).toUByte()
                    ) {}
                }
            }
            (chDevice.mechStatus as? CHSesameOpenSensorMechStatus)?.let { it ->
                val fullText = parseOpensensorState(it)
                fullText?.let {
                    centerText.text = it
                }
            }
            sesame2Status.text = chDevice.deviceStatus.toString()
            val isBleConnect = chDevice.deviceStatus.value == CHDeviceLoginStatus.Login
            blImg.setImageResource(if (isBleConnect) R.drawable.bl_green else R.drawable.bl_grey)
            customName.text = chDevice.getNickname()
            ssmView.setLockImage(chDevice)
            if (chDevice.productModel != CHProductModel.Hub3) {
                batteryPercent.text = chDevice.mechStatus?.getBatteryPrecentage().toString() + "%"
                btnPercent.progressDrawable = ContextCompat.getDrawable(
                    itemView.context,
                    if ((chDevice.mechStatus?.getBatteryPrecentage()
                            ?: 0) < 15
                    ) R.drawable.progress_red else R.drawable.progress_blue
                )
                btnPercent.progress = chDevice.mechStatus?.getBatteryPrecentage() ?: 0
            }
            batteryPercent.visibility = if (chDevice.mechStatus == null) View.GONE else View.VISIBLE
            battery_contain.visibility =
                if (chDevice.mechStatus == null) View.GONE else View.VISIBLE
            sesame2Status.text = chDevice.deviceStatus.toString()
            if (chDevice is CHSesameConnector) {
                sesame2Status.visibility =
                    if (chDevice.deviceStatus.value == CHDeviceLoginStatus.Login || chDevice.deviceStatus == CHDeviceStatus.NoBleSignal || chDevice.deviceStatus == CHDeviceStatus.ReceivedAdV) View.GONE else View.VISIBLE
            } else {
                sesame2Status.visibility =
                    if (chDevice.deviceStatus.value == CHDeviceLoginStatus.Login || chDevice.deviceStatus == CHDeviceStatus.NoBleSignal) View.GONE else View.VISIBLE
            }
            shadowStatusTxt.text = chDevice.deviceShadowStatus.toString()
            if (chDevice.getLevel() == 2) {
                chDevice.deviceShadowStatus = null
            }
            val isWifiConnect = if (chDevice.productModel == CHProductModel.Hub3) {
                (chDevice.mechStatus as? CHWifiModule2NetWorkStatus)?.isIOTWork == true
            } else {
                chDevice.deviceShadowStatus?.value == CHDeviceLoginStatus.Login
            }
            wifiImg.setImageResource(if (isWifiConnect) R.drawable.wifi_green else R.drawable.wifi_grey)
            //customName.text = ssmBike.getNickname()
            if (chDevice.productModel != CHProductModel.Hub3) {
                btnPercent.progressDrawable = ContextCompat.getDrawable(
                    itemView.context,
                    if ((chDevice.mechStatus?.getBatteryPrecentage()
                            ?: 0) < 15
                    ) R.drawable.progress_red else R.drawable.progress_blue
                )
                btnPercent.progress = chDevice.mechStatus?.getBatteryPrecentage() ?: 0
            }
            blImg.visibility = View.VISIBLE
            batteryPercent.visibility = View.VISIBLE
            btnPercent.visibility = View.VISIBLE
            battery_contain.visibility = View.VISIBLE
            if (!isBleConnect && !isWifiConnect) {
                battery_contain.visibility = View.GONE
                batteryPercent.visibility = View.GONE
            } else {
                battery_contain.visibility = View.VISIBLE
                batteryPercent.visibility = View.VISIBLE
            }
            centerText.visibility = View.GONE
            when (chDevice) {
                is CHSesameTouchPro, is CHHub3 , is CHSesameTouch, is CHSesameTouchPro, is CHSesameFacePro, is CHSesameFace-> {
                    blImg.visibility = View.GONE
                    val opensensorDecide =
                        if (centerText.text.isNullOrEmpty()) View.GONE else View.VISIBLE
                    val shouldShow =
                        if (chDevice.productModel == CHProductModel.SSMOpenSensor) opensensorDecide else View.GONE
                    batteryPercent.visibility = shouldShow
                    btnPercent.visibility = shouldShow
                    battery_contain.visibility = shouldShow
                    centerText.visibility = shouldShow
                }
            }
        }
    }
}
