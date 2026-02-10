package co.candyhouse.app.tabs.devices.ssm2.setting.angle

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.databinding.FgSetAngleBinding
import co.candyhouse.app.tabs.devices.model.bindLifecycle
import co.candyhouse.sesame.open.device.CHDeviceLoginStatus
import co.candyhouse.sesame.open.device.CHDeviceStatusDelegate
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHProductModel
import co.candyhouse.sesame.open.device.CHSesame2
import co.candyhouse.sesame.open.device.CHSesame5
import co.utils.UserUtils

class SSM2SetAngleFG : BaseDeviceFG<FgSetAngleBinding>() {

    private var useLidingDoorUi: Boolean = false

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
                }.bindLifecycle(viewLifecycleOwner)
            }
            bind.ssmView.setOnClickListener {
                (this as? CHSesame2)?.toggle() {}
                (this as? CHSesame5)?.toggle(historytag = UserUtils.getUserIdWithByte()) {}
            }
            bind.slidingDoorView.setOnClickListener {
                (this as? CHSesame5)?.toggle(historytag = UserUtils.getUserIdWithByte()) {}
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
                        setLockFromDevice(device, useLidingDoorUi)
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
                        setLockFromDevice(device, useLidingDoorUi)
                    }
                }
            }
            bind.magnetZone.setOnClickListener {
                (this as? CHSesame5)?.magnet {}
            }
            bind.magnetZone.setOnLongClickListener {
                val dev = this ?: return@setOnLongClickListener true

                if (dev.productModel == CHProductModel.SS6Pro) {
                    useLidingDoorUi = !useLidingDoorUi
                    updateLockView(dev)
                    true
                } else {
                    false
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun updateLockView(device: CHDevices) {
        val canToggle = device.productModel == CHProductModel.SS6Pro
        val showLiding = canToggle && useLidingDoorUi

        bind.angleTv.text = (device.mechStatus?.position ?: 0).toString() + "°"
        bind.ssmView.visibility = if (showLiding) View.GONE else View.VISIBLE
        bind.slidingDoorView.visibility = if (showLiding) View.VISIBLE else View.GONE
        bind.magnetZone.visibility = if (device is CHSesame5) View.VISIBLE else View.GONE

        setLockFromDevice(device, showLiding)
    }

    private fun setLockFromDevice(device: CHDevices, showLiding: Boolean = false) {
        when (device) {
            is CHSesame2 -> {
                bind.ssmView.setLock(device)
            }

            is CHSesame5 -> {
                if (showLiding) {
                    bind.slidingDoorView.setLock(
                        pos = (device.mechStatus?.position ?: 0).toInt(),
                        lockPos = (device.mechSetting?.lockPosition ?: 0).toInt(),
                        unlockPos = (device.mechSetting?.unlockPosition ?: 0).toInt()
                    )
                } else {
                    bind.ssmView.setLock(device)
                }
            }
        }
    }

    override fun onDestroyView() {
        useLidingDoorUi = false
        super.onDestroyView()
    }
}

