package co.candyhouse.app.tabs.devices.hub3.setting

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import candyhouse.sesameos.ir.base.BaseIr.Companion.hub3Device
import candyhouse.sesameos.ir.base.IrRemote
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.IRType
import candyhouse.sesameos.ir.ext.Config
import candyhouse.sesameos.ir.ext.Ext.getParcelableCompat
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceSettingFG
import co.candyhouse.app.databinding.FgHub3SettingBinding
import co.candyhouse.app.ext.aws.AWSStatus
import co.candyhouse.app.tabs.devices.hub3.adapter.Hub3IrAdapter
import co.candyhouse.app.tabs.devices.hub3.adapter.Ssm2DevicesAdapter
import co.candyhouse.app.tabs.devices.hub3.adapter.provider.Hub3IrAdapterProvider
import co.candyhouse.app.tabs.devices.hub3.adapter.provider.Ssm2DevicesAdapterProvider
import co.candyhouse.app.tabs.devices.model.LockDeviceStatus
import co.candyhouse.app.tabs.devices.model.bindLifecycle
import co.candyhouse.sesame.open.device.CHDeviceLoginStatus
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.open.device.CHHub3Delegate
import co.candyhouse.sesame.open.device.CHWifiModule2
import co.candyhouse.sesame.open.device.CHWifiModule2MechSettings
import co.candyhouse.sesame.open.device.CHWifiModule2NetWorkStatus
import co.candyhouse.sesame.utils.L
import co.utils.safeNavigate
import co.utils.safeNavigateBack
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import kotlinx.coroutines.launch
import java.util.Locale

class Hub3SettingFG : BaseDeviceSettingFG<FgHub3SettingBinding>(), CHHub3Delegate,
    Ssm2DevicesAdapterProvider, Hub3IrAdapterProvider {

    private val tag = "Hub3SettingFG"

    private var versionTag = ""
    private var netVersionTag = ""

    private var currentProgress = 0
    private var irRemote: IrRemote? = null

    private var mDeviceList = ArrayList<LockDeviceStatus>()

    override fun getViewBinder() = FgHub3SettingBinding.inflate(layoutInflater)

    @SuppressLint("SimpleDateFormat", "UnsafeRepeatOnLifecycleDetector")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val device = mDeviceViewModel.ssmLockLiveData.value
        if (device !is CHHub3) {
            safeNavigateBack()
            return
        }
        device.connect { }

        setupUI(device)
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        L.d(tag, "onStart")
        observeViewModelData()
    }

    override fun onResume() {
        super.onResume()
        L.d(tag, "onResume")
        dispatchOnResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 若蓝牙固件升级中不断开连接，避免离开再进入进度消失
        if (mDeviceViewModel.ssmLockLiveData.value?.deviceStatus?.value == CHDeviceLoginStatus.Login
            && (currentProgress in 1..99)
        ) {
            L.d("sf", "固件-蓝牙升级中……")
        } else {
            L.d("sf", "断开蓝牙连接……")
            mDeviceViewModel.ssmLockLiveData.value?.disconnect { }
            mDeviceModel.ssmLockLiveData.value?.disconnect { }
        }
    }

    private fun setupUI(device: CHHub3) {
        // 加载芝麻设备
        addSsm2Devices(device)

        // 加载红外线数据
        addHub3Devices(device)

        // 设置 LED 亮度
        if (device.deviceStatus == CHDeviceStatus.NoSettings) {
            setHub3Brightness(device)
        }
    }

    private fun setupListeners() {
        // 添加芝麻设备
        bind.addLockerZone.setOnClickListener {
            if (mDeviceList.isNotEmpty()) {
                val ids = ArrayList<String>()
                mDeviceList.forEach {
                    ids.add(it.id)
                }
                val bundle = Bundle()

                bundle.putStringArrayList("data", ids)
                findNavController().navigate(R.id.to_HUB3SelectLockerListFG, bundle)
            } else {
                findNavController().navigate(R.id.to_HUB3SelectLockerListFG)
            }
        }

        // WiFi SSID
        bind.apScanZone.setOnClickListener {
            safeNavigate(R.id.to_HUB3ScanSSIDListFG)
        }

        // matter协议
        bind.matterZone.setOnClickListener {
            val device = mDeviceViewModel.ssmLockLiveData.value!! as CHHub3
            device.apply {
                safeNavigate(R.id.to_Hub3MatterFG)
            }
        }

        // 添加红外线数据
        bind.tvIr.setOnClickListener {
            mDeviceViewModel.ssmLockLiveData.value?.apply {
                if (this is CHHub3) {
                    hub3Device.value = this
                    val bundle = Bundle()
                    if (irRemote != null) {
                        bundle.apply {
                            putParcelable(Config.irDevice, irRemote)
                        }
                    }
                    safeNavigate(R.id.action_to_irfg, bundle)
                }
            }
        }

        setupBackViewListener()
    }

    private fun observeViewModelData() {
        mDeviceViewModel.ssmLockLiveData.observe(viewLifecycleOwner) { hub3 ->
            if (hub3.deviceStatus == CHDeviceStatus.ReceivedAdV) {
                hub3.connect { }
            }
            if (hub3.mechStatus is CHWifiModule2NetWorkStatus) {
                L.d(tag, "observe mechStatus")
                val status: CHWifiModule2NetWorkStatus =
                    hub3.mechStatus as CHWifiModule2NetWorkStatus

                checkIrStatus(status)

                view?.findViewById<TextView>(R.id.wm2_id_txt)?.text =
                    hub3.deviceId.toString().uppercase()

                if (hub3 is CHHub3) {
                    bind.wifiSsidTxt.text = hub3.mechSetting?.wifiSSID
                    bind.wifiPassTxt.text = hub3.mechSetting?.wifiPassWord

                    if (hub3.deviceStatus.value == CHDeviceLoginStatus.UnLogin) {
                        if (status.isIOTWork == true) { // IOT 状态是从AWS拿的。 其他状态的显示需要等到蓝牙连上之后才会刷新， 在没连上蓝牙之前，需要保持UI显示的一致性。
                            true.also { bind.apIcon.isSelected = it }
                            true.also { bind.netIcon.isSelected = it }
                            true.also { bind.iotIcon.isSelected = it }
                            VISIBLE.also { bind.netOkLine.visibility = it }
                            VISIBLE.also { bind.iotOkLine.visibility = it }
                            GONE.also { bind.ssidLogo.visibility = it }
                        } else {
                            false.also { bind.apIcon.isSelected = it }
                            false.also { bind.netIcon.isSelected = it }
                            false.also { bind.iotIcon.isSelected = it }
                            bind.netOkLine.visibility = GONE
                            bind.iotOkLine.visibility = GONE
                            bind.ssidLogo.visibility = VISIBLE
                        }
                    } else {
                        ((hub3.mechStatus as? CHWifiModule2NetWorkStatus)?.isAPWork == true).also {
                            bind.apIcon.isSelected = it
                        }
                        ((hub3.mechStatus as? CHWifiModule2NetWorkStatus)?.isNetWork == true).also {
                            bind.netIcon.isSelected = it
                        }
                        ((hub3.mechStatus as? CHWifiModule2NetWorkStatus)?.isIOTWork == true).also {
                            bind.iotIcon.isSelected = it
                        }
                        (if ((hub3.mechStatus as? CHWifiModule2NetWorkStatus)?.isAPCheck == false) VISIBLE else GONE).also {
                            bind.ssidLogo.visibility = it
                        }
                        (if (bind.iotIcon.isSelected) VISIBLE else GONE).also {
                            bind.iotOkLine.visibility = it
                        }
                        (if (bind.netIcon.isSelected) VISIBLE else GONE).also {
                            bind.netOkLine.visibility = it
                        }
                    }
                }
            }
        }

        // 接收成功删除IR设备信息
        lifecycleScope.launch {
            mDeviceViewModel.channel.collect { type ->
                // 处理消息
                L.d("sf", "type= $type")

                if (type == IRType.DEVICE_REMOTE_CUSTOM) {
                    // 删除"自学习"遥控临时数据
                    L.d("sf", "clear DEVICE_REMOTE_CUSTOM data...")
                    irRemote = null
                }
                // 加载红外线数据
                val device = mDeviceViewModel.ssmLockLiveData.value
                if (device is CHHub3) {
                    addHub3Devices(device)
                }
            }
        }
    }

    private fun dispatchOnResume() {
        handleHub3Status()
        checkIsCompany()
        // 从Iot获取hub3数据，并回调刷新版本号
        L.d("harry", "request hub3 status")
        val currentDevice = (mDeviceViewModel.ssmLockLiveData.value!! as CHHub3)
        currentDevice.getHub3StatusFromIot(currentDevice.deviceId.toString()) {}
    }

    private fun handleHub3Status() {
        mDeviceViewModel.ssmosLockDelegates[(mDeviceViewModel.ssmLockLiveData.value!! as CHHub3)] =
            object : CHHub3Delegate {
                override fun onBleDeviceStatusChanged(
                    device: CHDevices,
                    status: CHDeviceStatus,
                    shadowStatus: CHDeviceStatus?
                ) {
                    onChange()
                    onUIDeviceStatus(status)
                    if (device.deviceStatus == CHDeviceStatus.ReceivedAdV) {
                        device.connect { }
                    }
                }

                @SuppressLint("SetTextI18n")
                override fun onOTAProgress(device: CHWifiModule2, percent: Byte) {
                    val targetPercent = percent.toInt()

                    if (targetPercent > currentProgress) {
                        // 使用 ValueAnimator 来平滑递增到目标百分比
                        ValueAnimator.ofInt(currentProgress, targetPercent).apply {
                            duration = 2500 // 动画时长，OTA实测数据
                            addUpdateListener { animation ->
                                val animatedValue = animation.animatedValue as Int
                                bind.deviceVersionTxt.text = "$animatedValue%"
                            }
                            start()
                        }
                        currentProgress = targetPercent
                    }

                    // 升级完成后，归0
                    if (currentProgress >= 100) {
                        currentProgress = 0
                    }
                }

                override fun onMechStatus(device: CHDevices) {
                    L.d(tag, "onMechStatus")
                    verTag()
                    val status: CHWifiModule2NetWorkStatus? =
                        (device.mechStatus as? CHWifiModule2NetWorkStatus)

                    if (device.deviceStatus.value == CHDeviceLoginStatus.UnLogin) {
                        if (status?.isIOTWork == true) { // IOT 状态是从AWS拿的。 其他状态的显示需要等到蓝牙连上之后才会刷新， 在没连上蓝牙之前，需要保持UI显示的一致性。
                            bind.apIcon.isSelected = true
                            bind.netIcon.isSelected = true
                            bind.iotIcon.isSelected = true
                            bind.netOkLine.visibility = VISIBLE
                            bind.iotOkLine.visibility = VISIBLE
                            bind.ssidLogo.visibility = GONE
                        } else {
                            bind.apIcon.isSelected = false
                            bind.netIcon.isSelected = false
                            bind.iotIcon.isSelected = false
                            bind.netOkLine.visibility = GONE
                            bind.iotOkLine.visibility = GONE
                            bind.ssidLogo.visibility = VISIBLE
                        }
                    } else {
                        bind.apIcon.isSelected = status?.isAPWork == true
                        bind.iotOkLine.visibility =
                            if (status?.isIOTWork == true) VISIBLE else GONE
                        bind.netOkLine.visibility =
                            if (status?.isNetWork == true) VISIBLE else GONE

                        bind.apIcon.isSelected = (status?.isAPWork == true)
                        bind.netIcon.isSelected = (status?.isNetWork == true)
                        bind.iotIcon.isSelected = (status?.isIOTWork == true)

                        bind.apingIcon.visibility =
                            if (status?.isAPConnecting == true) VISIBLE else View.GONE
                        bind.netingIcon.visibility =
                            if (status?.isConnectingNet == true) VISIBLE else View.GONE
                        bind.iotIngIcon.visibility =
                            if (status?.isConnectingIOT == true) VISIBLE else View.GONE

                        status?.isAPCheck = (status?.isAPWork == true)
                        bind.ssidLogo.visibility =
                            if (status?.isAPCheck == false) VISIBLE else View.GONE
                    }

                    status?.let { checkIrStatus(it) }
                }

                @SuppressLint("NotifyDataSetChanged")
                override fun onSSM2KeysChanged(
                    device: CHWifiModule2,
                    ssm2keys: Map<String, String>
                ) {
                    if (!isAdded) return
                    bind.recy.post {
                        mDeviceList.clear()
                        mDeviceList.addAll(ssm2keys.map {
                            LockDeviceStatus(it.key, it.value.toByte(), it.value.toByte())
                        })
                        bind.recy.adapter?.notifyDataSetChanged()

                        checkIrStatus(device.mechStatus as CHWifiModule2NetWorkStatus)
                    }
                }

                override fun onAPSettingChanged(
                    device: CHWifiModule2,
                    settings: CHWifiModule2MechSettings
                ) {
                    bind.wifiSsidTxt.text = settings.wifiSSID
                    bind.wifiPassTxt.text = settings.wifiPassWord
                    verTag()
                }

                override fun onHub3BrightnessReceive(device: CHHub3, brightness: Int) {
                    L.d("duty", "hub3连接后返回的亮度 $brightness")

                    // 设置 LED 亮度
                    setHub3Brightness(device)
                }

            }.bindLifecycle(viewLifecycleOwner)
    }

    private fun checkIrStatus(status: CHWifiModule2NetWorkStatus) {
        if (status.isAPWork == true) {
            bind.devicesEmptyLogo.visibility = if (mDeviceList.size == 0) VISIBLE else GONE
            bind.addLockerZone.visibility = if (mDeviceList.size < 5) VISIBLE else GONE

            bind.addLockerZone.isEnabled = true
            bind.addSsm.setTextColor(resources.getColor(R.color.black))

            bind.tvIr.isEnabled = true
            bind.tvIr.setTextColor(resources.getColor(R.color.black))
        } else {
            // 添加芝麻设备置为灰色
            bind.addLockerZone.isEnabled = false
            bind.addSsm.setTextColor(resources.getColor(R.color.gray1))
            bind.devicesEmptyLogo.visibility = GONE

            // 添加红外线设备置为灰色
            bind.tvIr.isEnabled = true
            bind.tvIr.setTextColor(resources.getColor(R.color.gray1))
        }
    }

    private fun setHub3Brightness(device: CHHub3) {
        val brightness = device.hub3Brightness
        L.d("duty", "duty=$brightness")
        val progressValue = calculateBrightnessPercentage(brightness)
        L.d("duty", "progressValue=$progressValue")
        bind.ledSeekbar.setProgress(progressValue)
        bind.ledSeekbar.setIndicatorTextFormat(getString(R.string.brightness) + " \${PROGRESS}%")

        bind.ledSeekbar.onSeekChangeListener = object : OnSeekChangeListener {
            override fun onSeeking(seekParams: SeekParams) {}

            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar) {}

            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {
                val progressTouch = (seekBar.progress / 100.0 * 255).toInt().toByte()
                L.d("duty", "progressTouch=$progressTouch")
                device.setHub3Brightness(progressTouch) { res ->
                    res.onSuccess {
                        L.d("duty", "Hub 3 setting success...")
                    }
                }
            }
        }

        bind.ledDutyZone.visibility = VISIBLE
    }

    private fun calculateBrightnessPercentage(byteValue: Byte): Float {
        val unsignedValue = byteValue.toInt() and 0xFF

        return (unsignedValue / 255.0 * 100).toFloat()
    }

    private fun addSsm2Devices(device: CHHub3) {
        bind.recy.apply {
            mDeviceList.clear()
            val ssm2KeysMapCopy = device.ssm2KeysMap

            if (ssm2KeysMapCopy.isNotEmpty()) {
                mDeviceList.addAll(ssm2KeysMapCopy.map { (key, value) ->
                    LockDeviceStatus(key, value.toByte(), value.toByte())
                })
            }

            bind.devicesEmptyLogo.visibility = if (mDeviceList.size == 0) VISIBLE else GONE
            adapter = Ssm2DevicesAdapter(context, mDeviceList, this@Hub3SettingFG)
        }
    }

    private fun addHub3Devices(device: CHHub3) {
        val uuid = device.deviceId.toString().uppercase(Locale.getDefault())
        lifecycleScope.launch {
            val result = mDeviceViewModel.getHub3Data(uuid)
            result.fold(
                onSuccess = { data -> updateHub3List(data) },
                onFailure = { error -> handleError(error) }
            )
        }
    }

    private fun updateHub3List(data: List<IrRemote>) {
        L.d("sf", "更新列表……")
        val mutableList: MutableList<IrRemote> = data as java.util.ArrayList
        bind.recyIR.apply {
            adapter = Hub3IrAdapter(context, mutableList, this@Hub3SettingFG)
        }
    }

    private fun handleError(exception: Throwable) {
        L.d("sf", "刷新红外线列表数据异常：" + exception.message)
    }

    private fun checkIsCompany() {
        // 红外内测放开逻辑前置判断：非公司内部用户，true 放开；false 关闭。
        val flag = true
        if (AWSStatus.checkIsCompany() || flag) {
            bind.tvIrText.visibility = VISIBLE
            bind.tvIr.visibility = VISIBLE
            bind.recyIR.visibility = VISIBLE
        } else {
            bind.tvIrText.visibility = GONE
            bind.tvIr.visibility = VISIBLE
            bind.recyIR.visibility = GONE
        }
    }

    @SuppressLint("SetTextI18n")
    private fun compareIfNewest() {
        view?.findViewById<View>(R.id.device_version_txt)?.post {
            val tailTag = netVersionTag
            val cheddd = tailTag.isNotEmpty() && versionTag.contains(tailTag)
            view?.findViewById<TextView>(R.id.device_version_txt)?.text =
                versionTag + (if (cheddd) getString(R.string.latest) else "")
            if (tailTag.isNotEmpty()) {
                view?.findViewById<View>(R.id.alert_logo)?.visibility =
                    if (cheddd) View.GONE else View.VISIBLE
            }
        }
    }

    private fun verTag() {
        val currentDevice = (mDeviceViewModel.ssmLockLiveData.value!! as CHHub3)
        if (!(currentDevice.versionTagFromIoT.isNullOrEmpty()) && !(currentDevice.hub3LastFirmwareVer.isNullOrEmpty())) { // IoT 有数据
            versionTag = currentDevice.versionTagFromIoT!!
            netVersionTag = (currentDevice.hub3LastFirmwareVer!!).split("-").last()
            compareIfNewest()
        } else { // IoT 没数据, 从蓝牙获取
            currentDevice.getVersionTag {
                it.onSuccess { va ->
                    if (isAdded && !isDetached) {
                        if (va.data.startsWith("B")) {
                            versionTag = va.data.split(":").last()
                        }
                        if (va.data.startsWith("N")) {
                            netVersionTag = va.data.split(":").last()
                        }
                        compareIfNewest()
                    }
                }
            }
        }
    }

    private fun setupBackViewListener() {
        setFragmentResultListener(Config.controlIrDeviceResult) { _, bundle ->
            var irRemote: IrRemote? = null
            var chDeviceId: String? = null
            if (bundle.containsKey(Config.irDevice)) {
                irRemote = bundle.getParcelableCompat<IrRemote>(Config.irDevice)
            }
            if (bundle.containsKey(Config.chDeviceId)) {
                chDeviceId = bundle.getString(Config.chDeviceId)
            }
            if (irRemote != null && !chDeviceId.isNullOrEmpty()) {
                mDeviceViewModel.updateHub3IrDevice(irRemote, chDeviceId)
            }
        }
    }

    override fun getDeviceNameByIdNew(id: String): String? {
        return getDeviceNameById(id)
    }

    override fun removeSesame(id: String) {
        (mDeviceViewModel.ssmLockLiveData.value!! as CHHub3).removeSesame(id) {}
    }

    override fun setIrRemote(data: IrRemote) {
        L.d("sf", "add DEVICE_REMOTE_CUSTOM IrRemote")
        irRemote = data
    }

    override fun performStudy(data: IrRemote) {
        mDeviceViewModel.ssmLockLiveData.value?.apply {
            if (this is CHHub3) {
                hub3Device.value = this
                val bundle = Bundle().apply {
                    putParcelable(Config.irDevice, data)
                }
                safeNavigate(
                    R.id.action_to_irdiy3,
                    bundle
                )
            }
        }
    }

    override fun performAirControl(data: IrRemote) {
        mDeviceViewModel.ssmLockLiveData.value?.apply {
            if (this is CHHub3) {
                hub3Device.value = this
                safeNavigate(
                    R.id.action_to_irgridefg,
                    Bundle().apply {
                        this.putParcelable(
                            Config.irDevice,
                            data
                        )
                        this.putBoolean(
                            Config.isNewDevice,
                            false
                        )
                    })
            }
        }
    }

    override fun deleteIRDevice(data: IrRemote) {
        mDeviceViewModel.ssmLockLiveData.value?.apply {
            if (this is CHHub3) {
                val uuid = this.deviceId.toString()
                    .uppercase(Locale.getDefault())
                mDeviceViewModel.deleteIRDevice(
                    uuid,
                    data.uuid,
                    data.type
                )
            }
        }
    }

}
