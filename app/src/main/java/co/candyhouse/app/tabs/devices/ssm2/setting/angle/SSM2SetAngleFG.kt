package co.candyhouse.app.tabs.devices.ssm2.setting.angle

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
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
import co.candyhouse.sesame.open.devices.base.CHDeviceStatusDelegate
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.open.devices.base.CHProductModel
import co.candyhouse.sesame.server.CHAPIClientBiz
import co.candyhouse.sesame.server.dto.cheyKeyToUserKey
import co.candyhouse.sesame.utils.L
import co.utils.UserUtils
import co.utils.vibrateDevice

class SSM2SetAngleFG : BaseDeviceSettingFG<FgSetAngleBinding>() {

    private val tag = "SSM2SetAngleFG"
    private var useSlidingDoorUi: Boolean = false

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
                }.bindLifecycle(viewLifecycleOwner)
                // 若固件已上报过切换点，进入页面时即显示按钮与角度标记
                updateSwitchPointUI(it)
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
                    val point = (device.mechStatus?.position ?: 0).toShort()
                    device.setLockUnlockSwitchPoint(point) {
                        it.onSuccess {
                            L.d(tag, "[setLockUnlockSwitchPoint] success point=$point")
                        }
                        it.onFailure { err ->
                            L.e(tag, "[setLockUnlockSwitchPoint] failed message=${err.message}")
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
                        L.d(tag, "sendAdvProductTypeCommand success advType=$advType")
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
                        L.e(tag, "切换失败（message=${err.message}）")
                    }
                }
                true
            }
        }
    }

    private fun syncProductModel(device: CHDevices, productModel: CHProductModel) {
        val deviceModel = productModel.deviceModel()
        val updatedKey = device.getKey().copy(deviceModel = deviceModel)

        CHDeviceManager.receiveCHDeviceKeys(updatedKey) { result ->
            result.onFailure { err ->
                L.e(tag, "save product model locally failed model=$deviceModel message=${err.message}")
            }
        }

        val updatedUserKey = device.userKey?.copy(deviceModel = deviceModel)
            ?: cheyKeyToUserKey(updatedKey, device.getLevel(), device.getNickname())
        CHAPIClientBiz.putKey(updatedUserKey) { result ->
            result.onSuccess {
                L.d(tag, "sync product model success model=$deviceModel")
                mDeviceModel.refreshDevices()
            }
            result.onFailure { err ->
                L.e(tag, "sync product model failed model=$deviceModel message=${err.message}")
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
