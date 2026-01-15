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
import co.candyhouse.sesame.open.device.CHSesame2
import co.candyhouse.sesame.open.device.CHSesame5
import co.utils.UserUtils

class SSM2SetAngleFG : BaseDeviceFG<FgSetAngleBinding>() {

    override fun getViewBinder() = FgSetAngleBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mDeviceModel.ssmLockLiveData.value.apply {
            (mDeviceModel.ssmLockLiveData.value as? CHSesame2)?.let {
                updateLockView(it)
                mDeviceModel.ssmosLockDelegates[it] = object : CHDeviceStatusDelegate {
                    override fun onMechStatus(device: CHDevices) {
                        updateLockView(it)
                    }
                }.bindLifecycle(viewLifecycleOwner)
                bind.magnetZone.visibility = View.GONE
            }
            (mDeviceModel.ssmLockLiveData.value as? CHSesame5)?.let {
                updateLockView(it)
                mDeviceModel.ssmosLockDelegates[it] = object : CHDeviceStatusDelegate {
                    override fun onMechStatus(device: CHDevices) {
                        updateLockView(it)
                    }
                }.bindLifecycle(viewLifecycleOwner)
            }

            bind.ssmView.setOnClickListener {
                (mDeviceModel.ssmLockLiveData.value as? CHSesame2)?.toggle() {}
                (mDeviceModel.ssmLockLiveData.value as? CHSesame5)?.toggle(historytag = UserUtils.getUserIdWithByte()) {}
            }
            bind.setunlockZone.setOnClickListener {
                if ((mDeviceModel.ssmLockLiveData.value as CHDevices).deviceStatus.value == CHDeviceLoginStatus.unlogined) {
                    return@setOnClickListener
                }

                (mDeviceModel.ssmLockLiveData.value as? CHSesame2)?.let { device ->
                    device.configureLockPosition(
                        device.mechSetting!!.lockPosition,
                        device.mechStatus!!.position
                    ) {
                        bind.ssmView.setLock(device)
                    }
                }
                (mDeviceModel.ssmLockLiveData.value as? CHSesame5)?.let { device ->
                    device.configureLockPosition(
                        device.mechSetting!!.lockPosition,
                        device.mechStatus!!.position
                    ) {
                        bind.ssmView.setLock(device)
                    }
                }
            }
            bind.setlockZone.setOnClickListener {
                if ((mDeviceModel.ssmLockLiveData.value as CHDevices).deviceStatus.value == CHDeviceLoginStatus.unlogined) {
                    return@setOnClickListener
                }
                (mDeviceModel.ssmLockLiveData.value as? CHSesame2)?.let { device ->
                    device.configureLockPosition(
                        device.mechStatus!!.position,
                        device.mechSetting!!.unlockPosition
                    ) {
                        bind.ssmView.setLock(device)
                    }
                }
                (mDeviceModel.ssmLockLiveData.value as? CHSesame5)?.let { device ->
                    device.configureLockPosition(
                        device.mechStatus!!.position,
                        device.mechSetting!!.unlockPosition
                    ) {
                        bind.ssmView.setLock(device)
                    }
                }
            }
            bind.magnetZone.setOnClickListener {
                (mDeviceModel.ssmLockLiveData.value as? CHSesame5)?.magnet {}
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun updateLockView(device: CHDevices) {
        bind.angleTv.text = device.mechStatus?.position.toString() + "°"
        when (device) {
            is CHSesame2 -> bind.ssmView.setLock(device)
            is CHSesame5 -> bind.ssmView.setLock(device)
        }
    }

}

