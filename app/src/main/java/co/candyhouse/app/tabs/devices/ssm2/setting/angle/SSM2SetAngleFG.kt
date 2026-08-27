package co.candyhouse.app.tabs.devices.ssm2.setting.angle

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceSettingFG
import co.candyhouse.app.databinding.FgSetAngleBinding
import co.candyhouse.app.ext.userKey
import co.candyhouse.app.tabs.devices.model.bindLifecycle
import co.candyhouse.app.tabs.devices.ssm2.getLevel
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.devices.CHSesame2
import co.candyhouse.sesame.open.devices.CHSesame5
import co.candyhouse.sesame.open.devices.base.CHDeviceLoginStatus
import co.candyhouse.sesame.open.devices.base.CHDeviceStatus
import co.candyhouse.sesame.open.devices.base.CHDeviceStatusDelegate
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.open.devices.base.CHProductModel
import co.candyhouse.sesame.server.CHAPIClientBiz
import co.candyhouse.sesame.server.dto.cheyKeyToUserKey
import co.candyhouse.sesame.utils.L
import co.utils.UserUtils
import co.utils.vibrateDevice
import java.text.NumberFormat
import kotlin.math.abs

class SSM2SetAngleFG : BaseDeviceSettingFG<FgSetAngleBinding>() {

    private val logTag = "SSM2SetAngleFG"
    private var useSlidingDoorUi: Boolean = false
    private val sensorDetectIntervalValues = (0..1000 step 50).map { it.toShort() }
    private var currentSensorDetectIntervalMs = CHDevices.UNSET_SENSOR_DETECT_INTERVAL_MS
    private var isUpdatingSensorDetectIntervalSwitch = false

    override fun getViewBinder() = FgSetAngleBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mDeviceModel.ssmLockLiveData.value.apply {
            (this as? CHSesame2)?.let {
                updateLockView(it)
                mDeviceModel.ssmosLockDelegates[it] = object : CHDeviceStatusDelegate {
                    override fun onMechStatus(device: CHDevices) {
                        updateLockView(it)
                    }
                }.bindLifecycle(viewLifecycleOwner)
            }
            (this as? CHSesame5)?.let {
                updateLockView(it)
                mDeviceModel.ssmosLockDelegates[it] = object : CHDeviceStatusDelegate {
                    override fun onMechStatus(device: CHDevices) {
                        updateLockView(it)
                    }

                    override fun onLockUnlockSwitchPointReceive(device: CHDevices, point: Short) {
                        updateSwitchPointUI(device)
                    }

                    override fun onSensorDetectIntervalReceive(device: CHDevices, intervalMs: Short) {
                        showSensorDetectIntervalUI(device, intervalMs)
                    }
                }.bindLifecycle(viewLifecycleOwner)
                // 若固件已上报过切换点，进入页面时即显示按钮与角度标记
                updateSwitchPointUI(it)
                showSensorDetectIntervalUI(it, it.sensorDetectIntervalMs)
            }
            bind.ssmView.setOnClickListener {
                (this as? CHSesame2)?.toggle() {}
                (this as? CHSesame5)?.toggle(historytag = UserUtils.getEnvironmentIdWithByte()) {}
            }
            bind.slidingDoorView.setOnClickListener {
                (this as? CHSesame5)?.toggle(historytag = UserUtils.getEnvironmentIdWithByte()) {}
            }
            bind.setunlockZone.setOnClickListener {
                if ((this as CHDevices).deviceStatus.value == CHDeviceLoginStatus.unlogined) {
                    return@setOnClickListener
                }

                (this as? CHSesame2)?.let { device ->
                    device.configureLockPosition(
                        device.mechSetting!!.lockPosition,
                        device.mechStatus!!.position
                    ) {
                        setLockFromDevice(device)
                    }
                }
                (this as? CHSesame5)?.let { device ->
                    device.configureLockPosition(
                        device.mechSetting!!.lockPosition,
                        device.mechStatus!!.position
                    ) {
                        setLockFromDevice(device, useSlidingDoorUi)
                    }
                }
            }
            bind.setlockZone.setOnClickListener {
                if ((this as CHDevices).deviceStatus.value == CHDeviceLoginStatus.unlogined) {
                    return@setOnClickListener
                }
                (this as? CHSesame2)?.let { device ->
                    device.configureLockPosition(
                        device.mechStatus!!.position,
                        device.mechSetting!!.unlockPosition
                    ) {
                        setLockFromDevice(device)
                    }
                }
                (this as? CHSesame5)?.let { device ->
                    device.configureLockPosition(
                        device.mechStatus!!.position,
                        device.mechSetting!!.unlockPosition
                    ) {
                        setLockFromDevice(device, useSlidingDoorUi)
                    }
                }
            }
            bind.magnetZone.setOnClickListener {
                (this as? CHSesame5)?.magnet {}
            }
            bind.switchPointZone.setOnClickListener {
                if ((this as CHDevices).deviceStatus.value == CHDeviceLoginStatus.unlogined) {
                    return@setOnClickListener
                }
                (this as? CHSesame5)?.let { device ->
                    val point = device.mechStatus?.position ?: return@let
                    device.setLockUnlockSwitchPoint(point) {
                        it.onSuccess {
                            L.d(logTag, "[setLockUnlockSwitchPoint] success point=$point")
                        }
                        it.onFailure { err ->
                            L.e(logTag, "[setLockUnlockSwitchPoint] failed message=${err.message}")
                        }
                    }
                }
            }
            bind.magnetZone.setOnLongClickListener {
                val dev = this
                val ssm5 = this as? CHSesame5 ?: return@setOnLongClickListener false
                val (targetModel, advType) = when (dev.productModel) {
                    CHProductModel.SS6ProSlidingDoor -> CHProductModel.SS6Pro to 21.toByte()
                    CHProductModel.SS6Pro -> CHProductModel.SS6ProSlidingDoor to 32.toByte()
                    else -> return@setOnLongClickListener true
                }

                view.context.vibrateDevice(100)
                ssm5.sendAdvProductTypeCommand(data = byteArrayOf(advType)) { res ->
                    res.onSuccess {
                        L.d(logTag, "sendAdvProductTypeCommand success advType=$advType")
                        activity?.runOnUiThread {
                            dev.productModel = targetModel
                            mDeviceModel.ssmLockLiveData.value = dev
                            useSlidingDoorUi = targetModel == CHProductModel.SS6ProSlidingDoor
                            bind.ssmView.visibility = if (useSlidingDoorUi) View.GONE else View.VISIBLE
                            bind.slidingDoorView.visibility = if (useSlidingDoorUi) View.VISIBLE else View.GONE
                            setLockFromDevice(dev, useSlidingDoorUi)
                            syncProductModel(dev, targetModel)
                        }
                    }
                    res.onFailure { err ->
                        L.e(logTag, "切换失败（message=${err.message}）")
                    }
                }
                true
            }
        }
    }

    private fun showSensorDetectIntervalUI(targetDevice: CHDevices, intervalMs: Short) {
        currentSensorDetectIntervalMs = intervalMs
        activity?.runOnUiThread {
            if (!isAdded) return@runOnUiThread

            if (intervalMs == CHDevices.UNSET_SENSOR_DETECT_INTERVAL_MS) {
                bind.sensorDetectIntervalWheelview.visibility = View.GONE
                bind.sensorDetectIntervalZone.visibility = View.GONE
                return@runOnUiThread
            }

            val selectedIndex = sensorDetectIntervalValues.indexOf(intervalMs).takeIf { it >= 0 }
                ?: sensorDetectIntervalValues.indices.minByOrNull {
                    abs(sensorDetectIntervalValues[it].toInt() - intervalMs.toInt())
                }
                ?: 0

            bind.sensorDetectIntervalWheelview.apply {
                setItems(sensorDetectIntervalValues.map(::sensorDetectFrequencyText))
                setInitPosition(selectedIndex)
                setCurrentPosition(selectedIndex)
                setListener { selected ->
                    val selectedInterval = sensorDetectIntervalValues.getOrNull(selected) ?: return@setListener
                    targetDevice.setSensorDetectInterval(selectedInterval) { result ->
                        result.onSuccess {
                            L.d(logTag, "设置传感器检测间隔成功：${sensorDetectIntervalSecondsText(selectedInterval)} 秒")
                            bind.sensorDetectIntervalWheelview.post {
                                updateSensorDetectIntervalState(targetDevice, selectedInterval)
                                setSensorDetectIntervalWheelVisible(false)
                            }
                        }
                        result.onFailure { error ->
                            L.e(logTag, "setSensorDetectInterval failed message=${error.message}")
                            bind.sensorDetectIntervalWheelview.post {
                                updateSensorDetectIntervalState(targetDevice, currentSensorDetectIntervalMs)
                            }
                        }
                    }
                }
            }

            bind.sensorDetectIntervalSwitch.setOnCheckedChangeListener(null)
            updateSensorDetectIntervalState(targetDevice, intervalMs)
            bind.sensorDetectIntervalSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (isUpdatingSensorDetectIntervalSwitch) return@setOnCheckedChangeListener
                setSensorDetectIntervalWheelVisible(isChecked)
            }
            bind.sensorDetectIntervalStatus.setOnClickListener {
                if (bind.sensorDetectIntervalSwitch.isEnabled &&
                    bind.sensorDetectIntervalSwitch.isChecked
                ) {
                    setSensorDetectIntervalWheelVisible(
                        bind.sensorDetectIntervalWheelview.visibility != View.VISIBLE
                    )
                }
            }

            bind.sensorDetectIntervalZone.visibility = View.VISIBLE
        }
    }

    private fun setSensorDetectIntervalWheelVisible(visible: Boolean) {
        bind.sensorDetectIntervalWheelview.visibility = if (visible) View.VISIBLE else View.GONE
        setSensorDetectIntervalSwitchChecked(visible)
        if (visible) {
            bind.scrollView.post {
                bind.scrollView.smoothScrollTo(0, bind.content.height)
            }
        }
    }

    private fun updateSensorDetectIntervalState(targetDevice: CHDevices, intervalMs: Short) {
        currentSensorDetectIntervalMs = intervalMs
        bind.sensorDetectIntervalSwitch.isEnabled =
            targetDevice.deviceStatus.value == CHDeviceLoginStatus.logined
        bind.sensorDetectIntervalStatus.text = sensorDetectFrequencyText(intervalMs)
        bind.sensorDetectIntervalStatus.visibility =
            if (intervalMs == 0.toShort()) View.INVISIBLE else View.VISIBLE
    }

    private fun setSensorDetectIntervalSwitchChecked(checked: Boolean) {
        isUpdatingSensorDetectIntervalSwitch = true
        bind.sensorDetectIntervalSwitch.isChecked = checked
        isUpdatingSensorDetectIntervalSwitch = false
    }

    override fun onUIDeviceStatus(status: CHDeviceStatus) {
        bind.sensorDetectIntervalSwitch.isEnabled =
            status.value == CHDeviceLoginStatus.logined &&
                    currentSensorDetectIntervalMs != CHDevices.UNSET_SENSOR_DETECT_INTERVAL_MS
        if (status.value != CHDeviceLoginStatus.logined) {
            setSensorDetectIntervalWheelVisible(false)
        }
    }

    @SuppressLint("StringFormatMatches")
    private fun sensorDetectFrequencyText(intervalMs: Short): String {
        if (intervalMs == 0.toShort()) return getString(R.string.stop_sensor_detection)

        return getString(R.string.times_per_second, formatDecimal(1000.0 / intervalMs.toInt()))
    }

    private fun sensorDetectIntervalSecondsText(intervalMs: Short): String =
        formatDecimal(intervalMs.toInt() / 1000.0)

    private fun formatDecimal(value: Double): String =
        NumberFormat.getNumberInstance().apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }.format(value)

    private fun syncProductModel(device: CHDevices, productModel: CHProductModel) {
        val deviceModel = productModel.deviceModel()
        val updatedKey = device.getKey().copy(deviceModel = deviceModel)

        CHDeviceManager.receiveCHDeviceKeys(updatedKey) { result ->
            result.onFailure { err ->
                L.e(logTag, "save product model locally failed model=$deviceModel message=${err.message}")
            }
        }

        val updatedUserKey = device.userKey?.copy(deviceModel = deviceModel)
            ?: cheyKeyToUserKey(updatedKey, device.getLevel(), device.getNickname())
        CHAPIClientBiz.putKey(updatedUserKey) { result ->
            result.onSuccess {
                L.d(logTag, "sync product model success model=$deviceModel")
                mDeviceModel.refreshDevices()
            }
            result.onFailure { err ->
                L.e(logTag, "sync product model failed model=$deviceModel message=${err.message}")
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun updateLockView(device: CHDevices) {
        useSlidingDoorUi = device.productModel == CHProductModel.SS6ProSlidingDoor

        bind.angleTv.text = (device.mechStatus?.position ?: 0).toString() + "°"
        bind.ssmView.visibility = if (useSlidingDoorUi) View.GONE else View.VISIBLE
        bind.slidingDoorView.visibility = if (useSlidingDoorUi) View.VISIBLE else View.GONE
        bind.magnetZone.visibility = if (device is CHSesame5) View.VISIBLE else View.GONE

        setLockFromDevice(device, useSlidingDoorUi)
    }

    private fun setLockFromDevice(device: CHDevices, showSliding: Boolean = false) {
        when (device) {
            is CHSesame2 -> {
                bind.ssmView.setLock(device)
            }

            is CHSesame5 -> {
                if (showSliding) {
                    bind.slidingDoorView.setLock(
                        pos = (device.mechStatus?.position ?: 0).toInt(),
                        lockPos = (device.mechSetting?.lockPosition ?: 0).toInt(),
                        unlockPos = (device.mechSetting?.unlockPosition ?: 0).toInt()
                    )
                } else {
                    bind.ssmView.setLock(device)
                }
                updateSwitchPointUI(device)
            }
        }
    }

    /** 根据固件是否上报过切换点（hasLockUnlockSwitchPointSetting），显示/隐藏切换点按钮，并在角度视图上标记对应位置。 */
    private fun updateSwitchPointUI(device: CHDevices) {
        if (device.hasLockUnlockSwitchPointSetting) {
            bind.switchPointZone.visibility = View.VISIBLE
            bind.ssmView.setSwitchPoint(device.lockUnlockSwitchPoint.toInt())
        } else {
            bind.switchPointZone.visibility = View.GONE
            bind.ssmView.clearSwitchPoint()
        }
    }

}
