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
import co.candyhouse.sesame.ble.os3.CHRemoteNanoTriggerSettings
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHDeviceStatusDelegate
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHProductModel
import co.candyhouse.sesame.open.device.CHSesameConnector
import co.candyhouse.sesame.open.device.sesameBiometric.capability.card.CHCardCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.connect.CHDeviceConnectDelegate
import co.candyhouse.sesame.open.device.sesameBiometric.capability.face.CHFaceCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.fingerPrint.CHFingerPrintCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.palm.CHPalmCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.passcode.CHPassCodeCapable
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
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import kotlinx.coroutines.launch

class SSMBiometricSettingFG : BaseDeviceSettingFG<FgSesameTouchproSettingBinding>() {
    var mDeviceList = ArrayList<LockDeviceStatus>()
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
                view?.findViewById<TextView>(R.id.battery)?.post {
                    view?.findViewById<TextView>(R.id.battery)?.text =
                        "${device.mechStatus?.getBatteryPrecentage() ?: 0} %"
                }
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
                        bind.devicesEmptyLogo.visibility =
                            if (mDeviceList.size == 0) View.VISIBLE else View.GONE
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
        L.d("sf", "setRadarUI..." + payload[0] + " " + payload[1])

        val sensitivityValue = payload[1].toInt() and 0xFF
        val percentage = calculateRadarPercentage(sensitivityValue)
        val distance = calculateDistance(percentage)

        L.d("sf", "设备返回的雷达灵敏度：$percentage%, 距离：${distance}cm")

        bind.radarSeekbar.setMin(40f)
        bind.radarSeekbar.setMax(400f)
        bind.radarSeekbar.setProgress(distance.toFloat())
        bind.radarSeekbar.setIndicatorTextFormat(getString(R.string.distance) + " \${PROGRESS}cm")

        bind.radarSeekbar.onSeekChangeListener = object : OnSeekChangeListener {
            override fun onSeeking(seekParams: SeekParams) {}

            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar) {}

            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {
                val distance1 = seekBar.progress
                val percentage1 = (distance1 - 40) * 100 / 360
                val sensitivityValue2 = calculateSensitivityValue(percentage1)

                L.d(
                    "sf",
                    "设置雷达灵敏度距离: ${distance1}cm, 百分比: $percentage1%, 值: $sensitivityValue2"
                )

                setRadarSensitivity(device, sensitivityValue2)
            }
        }

        activity?.runOnUiThread {
            bind.raderSensZone.visibility = View.VISIBLE
        }
    }

    private fun calculateRadarPercentage(sensitivityValue: Int): Int {
        return ((116 - sensitivityValue) * 100) / (116 - 16)
    }

    private fun calculateSensitivityValue(percentage: Int): Byte {
        val value = 116 - (percentage * (116 - 16)) / 100
        return value.toByte()
    }

    private fun calculateDistance(percentage: Int): Int {
        // 0% -> 40cm, 100% -> 400cm
        return 40 + (percentage * 360) / 100
    }

    private fun setRadarSensitivity(device: CHSesameConnector, sensitivityValue: Byte) {
        val payload = byteArrayOf(0x33, sensitivityValue, 0, 0, 0)

        device.setRadarSensitivity(payload) { res ->
            res.onSuccess {
                L.d("sf", "雷达灵敏度设置成功")
            }

            res.onFailure { error ->
                L.e("sf", "雷达灵敏度设置失败: $error")
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

        if (device.productModel === CHProductModel.SSMFace || device.productModel === CHProductModel.SSMFacePro || device.productModel === CHProductModel.SSMFaceProAI) {
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

        view.findViewById<TextView>(R.id.battery)?.post {
            view.findViewById<TextView>(R.id.battery)?.text =
                (mDeviceModel.ssmLockLiveData.value as CHSesameConnector).mechStatus?.getBatteryPrecentage()
                    ?.let { "$it%" } ?: ""
        }
    }

    private fun checkBiometricDeviceView() {
        if (mDeviceModel.ssmLockLiveData.value!!.productModel == CHProductModel.SSMOpenSensor
            || mDeviceModel.ssmLockLiveData.value!!.productModel == CHProductModel.BLEConnector
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
    }

    private fun setupCardZoneView() {
        if (mDeviceModel.ssmLockLiveData.value is CHCardCapable) {
            bind.cardsZone.visibility = View.VISIBLE
            bind.cardsZone.setOnClickListener {
                safeNavigate(R.id.to_SesameKeyboardCards)
            }
        } else {
            bind.cardsZone.visibility = View.GONE
        }
    }

    private fun setupFingerprintZoneView() {
        if (mDeviceModel.ssmLockLiveData.value is CHFingerPrintCapable) {
            bind.fpZone.visibility = View.VISIBLE
            bind.fpZone.setOnClickListener {
                safeNavigate(R.id.to_SesameKeyboardFingerprint)
            }
        } else {
            bind.fpZone.visibility = View.GONE
        }
    }

    private fun setupPasswordZoneView() {
        if (mDeviceModel.ssmLockLiveData.value is CHPassCodeCapable) {
            bind.passwordZone.visibility = View.VISIBLE
            bind.passwordZone.setOnClickListener {
                safeNavigate(R.id.to_SesameKeyboardPassword)
            }
        } else {
            bind.passwordZone.visibility = View.GONE
        }
    }

    private fun setupFaceZoneView() {
        if (mDeviceModel.ssmLockLiveData.value is CHFaceCapable) {
            bind.faceZone.visibility = View.VISIBLE
            bind.faceZone.setOnClickListener {
                safeNavigate(R.id.to_SesameFaceProFaces)
            }
        } else {
            bind.faceZone.visibility = View.GONE
        }
    }

    private fun setupPalmZoneView() {
        if (mDeviceModel.ssmLockLiveData.value is CHPalmCapable) {
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