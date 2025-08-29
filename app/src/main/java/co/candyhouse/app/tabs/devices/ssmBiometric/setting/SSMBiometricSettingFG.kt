package co.candyhouse.app.tabs.devices.ssmBiometric.setting

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceSettingFG
import co.candyhouse.app.databinding.FgSesameTouchproSettingBinding
import co.candyhouse.app.tabs.devices.model.LockDeviceStatus
import co.candyhouse.app.tabs.devices.model.bindLifecycle
import co.candyhouse.app.tabs.devices.ssm2.modelName
import co.candyhouse.app.tabs.devices.ssm2.setting.remotenanoSecondSettingValue
import co.candyhouse.sesame.ble.os3.BiometricCapability
import co.candyhouse.sesame.ble.os3.CHRemoteNanoTriggerSettings
import co.candyhouse.sesame.ble.os3.hasBiometricCapability
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHDeviceStatusDelegate
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHProductModel
import co.candyhouse.sesame.open.device.CHSesameConnector
import co.candyhouse.sesame.open.device.sesameBiometric.capability.connect.CHDeviceConnectDelegate
import co.candyhouse.sesame.open.device.sesameBiometric.capability.remoteNano.CHRemoteNanoDelegate
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBase
import co.candyhouse.sesame.server.dto.ChSubCfp
import co.candyhouse.sesame.utils.L
import co.utils.alertview.AlertView
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.alertview.objects.AlertAction
import co.utils.recycle.GenericAdapter
import co.utils.safeNavigate
import co.utils.stateInfo
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import kotlinx.coroutines.launch

class SSMBiometricSettingFG : BaseDeviceSettingFG<FgSesameTouchproSettingBinding>() {
    private val tag = "SSMBiometricSettingFG"

    var mDeviceList = ArrayList<LockDeviceStatus>()

    // 定义雷达灵敏度距离和固件值的查找表
    private val DISTANCE_TO_FIRMWARE_TABLE = listOf(
        30 to 116,
        60 to 44,
        80 to 31,
        100 to 23,
        120 to 21,
        150 to 20,
        200 to 17,
        270 to 16
    )

    override fun getViewBinder() = FgSesameTouchproSettingBinding.inflate(layoutInflater)

    private fun add_ssmTextColor() {
        val device = mDeviceModel.ssmLockLiveData.value
        var size = 3
        device?.apply {
            if (this.productModel == CHProductModel.RemoteNano) {
                size = 2
            } else if (this.productModel == CHProductModel.Remote) {
                size = 4
            }
        }
        view?.apply {
            findViewById<TextView>(R.id.add_ssm).setTextColor(
                if (mDeviceList.size >= size) Color.argb(
                    90,
                    Color.red(Color.GRAY),
                    Color.green(Color.GRAY),
                    Color.blue(Color.GRAY)
                ) else Color.BLACK
            )
        }
        bind.addLockerZone.isClickable = mDeviceList.size < size
        bind.addLockerZone.isEnabled = mDeviceList.size < size
    }

    override fun onResume() {
        super.onResume()
        if (mDeviceModel.ssmLockLiveData.value !is CHSesameBiometricBase) {
            return
        }
        val device = mDeviceModel.ssmLockLiveData.value as CHSesameBiometricBase
//        device.registerEventDelegate(device, mDeviceModel.ssmosLockDelegates[device]!!)
        val delegate = object : CHDeviceStatusDelegate, CHRemoteNanoDelegate, CHDeviceConnectDelegate {
            override fun onBleDeviceStatusChanged(
                device: CHDevices,
                status: CHDeviceStatus,
                shadowStatus: CHDeviceStatus?
            ) {
                onChange()
                onUIDeviceStatus(status)
                checkVersionTag(status, device)
                if (device.deviceStatus == CHDeviceStatus.ReceivedAdV && isAdded) {
                    device.connect { }
                }
                L.d("onSSM2KeysChanged", "onBleDeviceStatusChanged" + "---")
            }

            override fun onMechStatus(device: CHDevices) {
                setBattery(view, device)
            }

            override fun onRadarReceive(device: CHSesameConnector, payload: ByteArray) {
                setRadarUI(device, payload)
            }

            override fun onSSM2KeysChanged(
                device: CHSesameConnector,
                ssm2keys: Map<String, ByteArray>
            ) {
                if (!isAdded) return
                L.d("onSSM2KeysChanged", "onSSM2KeysChanged" + ssm2keys.size)
                viewLifecycleOwner.lifecycleScope.launch {
                    if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        mDeviceList.clear()
                        mDeviceList.addAll(ssm2keys.map {
                            LockDeviceStatus(it.key, it.value[0], it.value[1])
                        })
                        add_ssmTextColor()
                        bind.recy.adapter?.notifyDataSetChanged()
                        bind.devicesEmptyLogo.visibility = if (mDeviceList.size == 0) View.VISIBLE else View.GONE
                        bind.tvAddSsmLogo.visibility = if (mDeviceList.size == 0) View.GONE else View.VISIBLE
                    }
                }
                val list = arrayListOf<ChSubCfp>()
                ssm2keys.map {
                    list.add(ChSubCfp(it.key, ""))
                }
                isUpload = true
            }

            override fun onTriggerDelaySecondReceived(
                device: CHSesameConnector,
                setting: CHRemoteNanoTriggerSettings
            ) {
                if (isAdded && !isDetached) {
                    bind.triggerWheelview.apply {
                        post {
                            setCurrentPosition(setting.triggerDelaySecond.toInt())
                            bind.triggerStatus.text = String.format(
                                "%.1f %s",
                                setting.triggerDelaySecond.toInt() * 0.3,
                                context.getString(R.string.second2)
                            )
                            bind.triggerWheelview.visibility = View.GONE
                            isWheelViewVisible = false
                        }
                    }
                }
            }
        }
        delegate.bindLifecycle(viewLifecycleOwner)
        mDeviceModel.ssmosLockDelegates[device] = delegate
        device.registerEventDelegate(device, delegate)
    }

    private fun setRadarUI(device: CHSesameConnector, payload: ByteArray) {
        L.d(tag, "setRadarUI..." + payload[0] + " " + payload[1])

        val sensitivityValue = payload[1].toInt() and 0xFF
        val distance = calculateDistanceFromFirmwareValue(sensitivityValue)

        L.d(tag, "设备返回的雷达灵敏度值：$sensitivityValue, 对应标准距离：${distance}cm")

        bind.radarSeekbar.setMin(30f)
        bind.radarSeekbar.setMax(270f)
        bind.radarSeekbar.setProgress(distance.toFloat())
        bind.radarSeekbar.setIndicatorTextFormat(getString(R.string.distance) + " \${PROGRESS}cm")

        bind.radarSeekbar.onSeekChangeListener = object : OnSeekChangeListener {
            override fun onSeeking(seekParams: SeekParams) {}

            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar) {}

            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {
                val distance1 = seekBar.progress
                val sensitivityValue2 = calculateFirmwareValueFromDistance(distance1)

                L.d(tag, "设置雷达灵敏度距离: ${distance1}cm, 固件值: $sensitivityValue2")

                setRadarSensitivity(device, sensitivityValue2)
            }
        }

        activity?.runOnUiThread {
            bind.raderSensZone.visibility = View.VISIBLE
        }
    }

    // 根据固件值计算距离（使用线性插值）
    private fun calculateDistanceFromFirmwareValue(firmwareValue: Int): Int {
        if (firmwareValue >= 116) return 30
        if (firmwareValue <= 16) return 270

        // 找到相邻的两个点进行插值
        for (i in 0 until DISTANCE_TO_FIRMWARE_TABLE.size - 1) {
            val (dist1, fw1) = DISTANCE_TO_FIRMWARE_TABLE[i]
            val (dist2, fw2) = DISTANCE_TO_FIRMWARE_TABLE[i + 1]

            if (firmwareValue in fw2..fw1) {
                val ratio = (firmwareValue - fw2).toFloat() / (fw1 - fw2)
                return (dist2 + ratio * (dist1 - dist2)).toInt()
            }
        }

        return 30
    }

    // 根据距离计算固件值（使用线性插值）
    private fun calculateFirmwareValueFromDistance(distance: Int): Byte {
        if (distance <= 30) return 116
        if (distance >= 270) return 16

        // 找到相邻的两个点进行插值
        for (i in 0 until DISTANCE_TO_FIRMWARE_TABLE.size - 1) {
            val (dist1, fw1) = DISTANCE_TO_FIRMWARE_TABLE[i]
            val (dist2, fw2) = DISTANCE_TO_FIRMWARE_TABLE[i + 1]

            if (distance in dist1..dist2) {
                val ratio = (distance - dist1).toFloat() / (dist2 - dist1)
                val firmwareValue = fw1 + ratio * (fw2 - fw1)
                return firmwareValue.toInt().toByte()
            }
        }

        return 116
    }

    private fun setRadarSensitivity(device: CHSesameConnector, sensitivityValue: Byte) {
        val payload = byteArrayOf(0x33, sensitivityValue, 0, 0, 0)

        device.setRadarSensitivity(payload) { res ->
            res.onSuccess {
                L.d(tag, "雷达灵敏度设置成功")
            }

            res.onFailure { error ->
                L.e(tag, "雷达灵敏度设置失败: $error")
            }
        }
    }

    var isWheelViewVisible = false

    @SuppressLint("SimpleDateFormat", "DefaultLocale")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (mDeviceModel.ssmLockLiveData.value !is CHSesameBiometricBase) {
            return
        }
        val device = mDeviceModel.ssmLockLiveData.value as CHSesameBiometricBase
        device.connect { }

        bind.recy.isNestedScrollingEnabled = false
        bind.recy.apply {
            mDeviceList.clear()
            mDeviceList.addAll((mDeviceModel.ssmLockLiveData.value as CHSesameConnector).ssm2KeysMap.map {
                LockDeviceStatus(it.key, it.value[0], it.value[1])
            })
            add_ssmTextColor()
            bind.devicesEmptyLogo.visibility =
                if (mDeviceList.size == 0) View.VISIBLE else View.GONE
            bind.tvAddSsmLogo.visibility = if (mDeviceList.size == 0) View.GONE else View.VISIBLE
            adapter = object : GenericAdapter<LockDeviceStatus>(mDeviceList) {
                override fun getLayoutId(position: Int, obj: LockDeviceStatus): Int =
                    R.layout.wm2_key_cell

                override fun getViewHolder(
                    view: View,
                    viewType: Int
                ): RecyclerView.ViewHolder =
                    object : RecyclerView.ViewHolder(view), Binder<LockDeviceStatus> {
                        @SuppressLint("SetTextI18n")
                        override fun bind(data: LockDeviceStatus, pos: Int) {

                            L.d("LockDeviceStatus", data.toString())


                            val title = itemView.findViewById<TextView>(R.id.title)
                            title.text = data.id
                            getDeviceNameById(data.id)?.apply {
                                L.d("deviceNameById", this)
                                title.text = this
                            }
                            itemView.setOnClickListener {
                                AlertView(title.text.toString(), "", AlertStyle.IOS).apply {
                                    addAction(
                                        AlertAction(
                                            getString(R.string.ssm_delete),
                                            AlertActionStyle.NEGATIVE
                                        ) { action ->
                                            (mDeviceModel.ssmLockLiveData.value!! as CHSesameBiometricBase).removeSesame(
                                                data.id
                                            ) {}
                                        })
                                    show(activity as AppCompatActivity)
                                }
                            }
                        }
                    }
            }
        }
        if (device.productModel == CHProductModel.SSMOpenSensor || device.productModel == CHProductModel.RemoteNano) {
            bind.friendRecy.visibility = View.GONE
        }
        if (device.productModel == CHProductModel.RemoteNano) {
            bind.triggertimeZone.apply {
                visibility = View.VISIBLE
                setOnClickListener {
                    if (isWheelViewVisible) {
                        bind.triggerWheelview.visibility = View.GONE
                    } else {
                        bind.triggerWheelview.visibility = View.VISIBLE
                    }
                    isWheelViewVisible = !isWheelViewVisible
                }
            }
            bind.triggerSecond.visibility = View.VISIBLE
            bind.triggerWheelview.setNotLoop()
            bind.triggerWheelview.apply {
                setItems(remotenanoSecondSettingValue.map {
                    String.format(
                        "%.1f %s",
                        it * 0.3,
                        context.getString(R.string.second2)
                    )
                })
                setInitPosition(0)
                bind.triggerStatus.text = String.format(
                    "%.1f %s",
                    0.0,
                    context.getString(R.string.second2)
                )
                device.triggerDelaySetting?.triggerDelaySecond?.let {
                    setCurrentPosition(it.toInt())
                    bind.triggerStatus.text = String.format(
                        "%.1f %s",
                        it.toInt() * 0.3,
                        context.getString(R.string.second2)
                    )
                }
                setListener { selected ->
                    val opsLockSecond = remotenanoSecondSettingValue[selected]
                    device.setTriggerDelayTime(opsLockSecond.toUByte()) {}
                }
            }
        }
        bind.cardsZone.setOnClickListener { safeNavigate(R.id.to_SesameKeyboardCards) }
        bind.fpZone.setOnClickListener { safeNavigate(R.id.to_SesameKeyboardFingerprint) }
        bind.passwordZone.setOnClickListener { safeNavigate(R.id.to_SesameKeyboardPassword) }
        bind.faceZone.setOnClickListener { safeNavigate(R.id.to_SesameFaceProFaces) }
        bind.facePalm.setOnClickListener { safeNavigate(R.id.to_FacePalm) }

        setupPasswordZoneView()
        setupCardZoneView()
        setupFingerprintZoneView()
        setupFaceZoneView()
        setupPalmZoneView()
        checkBiometricDeviceView()

        if (device.productModel === CHProductModel.SSMFace || device.productModel === CHProductModel.SSMFacePro || device.productModel === CHProductModel.SSMFaceProAI || device.productModel === CHProductModel.SSMFaceAI) {
            bind.facePalm.visibility = View.VISIBLE
            if (device.deviceStatus == CHDeviceStatus.Unlocked) {
                // 只有Face刷卡机才显示雷达灵敏度
                setRadarUI(
                    (mDeviceModel.ssmLockLiveData.value as CHSesameConnector),
                    device.radarPayload
                )
            }
        } else {
            bind.raderSensZoneRl.visibility = View.GONE
            bind.radarDistanceSet.visibility = View.GONE
            bind.facePalm.visibility = View.GONE
        }
        view.findViewById<View>(R.id.share_zone)?.visibility =
            if (device.productModel == CHProductModel.SSMOpenSensor || device.productModel == CHProductModel.RemoteNano) View.GONE else View.VISIBLE

        val name = device.productModel.modelName()
        bind.addSsmHintByTouchTxt.text = getString(R.string.add_ssm_hint_by_touch, name)
        bind.trashDeviceKeyTxt.text = getString(R.string.trash_device_key, name)
        bind.addLockerZone.setOnClickListener {
            navigateNext(mDeviceList, R.id.to_SesameKeyboardSelectLockerListFG)
        }

        setBattery(view, device)
    }

    private fun setBattery(view: View?, device: CHDevices) {
        val batteryLevel = device.mechStatus?.getBatteryPrecentage() ?: device.stateInfo?.batteryPercentage
        view?.findViewById<TextView>(R.id.battery)?.post {
            view.findViewById<TextView>(R.id.battery)?.text = batteryLevel?.let { "$it%" } ?: ""
        }
    }

    private fun checkBiometricDeviceView() {
        if (mDeviceModel.ssmLockLiveData.value!!.productModel == CHProductModel.SSMOpenSensor
            || mDeviceModel.ssmLockLiveData.value!!.productModel == CHProductModel.Remote
            || mDeviceModel.ssmLockLiveData.value!!.productModel == CHProductModel.RemoteNano

        ) {
            bind.cardsZone.visibility = View.GONE
            bind.fpZone.visibility = View.GONE
            bind.passwordZone.visibility = View.GONE
            bind.faceZone.visibility = View.GONE
            bind.facePalm.visibility = View.GONE
        }
        if (mDeviceModel.ssmLockLiveData.value!!.productModel == CHProductModel.RemoteNano) {
            bind.batteryZone.visibility = View.GONE
        }
        if (mDeviceModel.ssmLockLiveData.value!!.productModel == CHProductModel.SSMFaceProAI) {
            bind.cardsZone.visibility = View.GONE
            bind.fpZone.visibility = View.GONE
        }
        if (mDeviceModel.ssmLockLiveData.value!!.productModel == CHProductModel.SSMFaceAI) {
            bind.cardsZone.visibility = View.GONE
            bind.fpZone.visibility = View.GONE
            bind.passwordZone.visibility = View.GONE
        }
    }

    private fun setupCardZoneView() {
        if (mDeviceModel.ssmLockLiveData.value?.hasBiometricCapability(BiometricCapability.CARD) == true) {
            bind.cardsZone.visibility = View.VISIBLE
            bind.cardsZone.setOnClickListener {
                safeNavigate(R.id.to_SesameKeyboardCards)
            }
        } else {
            bind.cardsZone.visibility = View.GONE
        }
    }

    private fun setupFingerprintZoneView() {
        if (mDeviceModel.ssmLockLiveData.value?.hasBiometricCapability(BiometricCapability.FINGERPRINT) == true) {
            bind.fpZone.visibility = View.VISIBLE
            bind.fpZone.setOnClickListener {
                safeNavigate(R.id.to_SesameKeyboardFingerprint)
            }
        } else {
            bind.fpZone.visibility = View.GONE
        }
    }

    private fun setupPasswordZoneView() {
        if (mDeviceModel.ssmLockLiveData.value?.hasBiometricCapability(BiometricCapability.PASSCODE) == true) {
            bind.passwordZone.visibility = View.VISIBLE
            bind.passwordZone.setOnClickListener {
                safeNavigate(R.id.to_SesameKeyboardPassword)
            }
        } else {
            bind.passwordZone.visibility = View.GONE
        }
    }

    private fun setupFaceZoneView() {
        if (mDeviceModel.ssmLockLiveData.value?.hasBiometricCapability(BiometricCapability.FACE) == true) {
            bind.faceZone.visibility = View.VISIBLE
            bind.faceZone.setOnClickListener {
                safeNavigate(R.id.to_SesameFaceProFaces)
            }
        } else {
            bind.faceZone.visibility = View.GONE
        }
    }

    private fun setupPalmZoneView() {
        if (mDeviceModel.ssmLockLiveData.value?.hasBiometricCapability(BiometricCapability.PALM) == true) {
            bind.facePalm.visibility = View.VISIBLE
            bind.facePalm.setOnClickListener {
                safeNavigate(R.id.to_FacePalm)
            }
        } else {
            bind.facePalm.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mDeviceModel.ssmLockLiveData.value?.disconnect { }
    }

    private fun navigateNext(list: ArrayList<LockDeviceStatus>, res: Int) {
        var bundle: Bundle? = null
        if (list.isNotEmpty()) {
            val strs = ArrayList<String>()
            list.forEach {
                strs.add(it.id)
            }
            bundle = Bundle()

            bundle.putStringArrayList("data", strs)
        }
        safeNavigate(res, bundle)
    }

}