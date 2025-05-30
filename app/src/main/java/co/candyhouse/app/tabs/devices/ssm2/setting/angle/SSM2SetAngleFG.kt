package co.candyhouse.app.tabs.devices.ssm2.setting.angle

import android.os.Bundle
import android.view.View
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.R
import co.candyhouse.app.databinding.FgNoHandBinding
import co.candyhouse.app.databinding.FgSetAngleBinding
import co.candyhouse.app.tabs.devices.model.bindLifecycle
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.sesame.open.device.*
import co.utils.UserUtils


class SSM2SetAngleFG : BaseDeviceFG<FgSetAngleBinding>() {
    override fun getViewBinder() = FgSetAngleBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        L.d("hcia", "SSM2SetAngleFG onViewCreated:")
        mDeviceModel.ssmLockLiveData.value.apply {
            this?.apply {
                bind.titlec.text = mDeviceModel.ssmLockLiveData.value!!.getNickname()
            }

            (mDeviceModel.ssmLockLiveData.value as? CHSesame2)?.let {
                bind.ssmView.setLock(it)
                mDeviceModel.ssmosLockDelegates[it] = object : CHDeviceStatusDelegate {
                    override fun onMechStatus(device: CHDevices) {
                        bind.ssmView.setLock(it)
                    }
                }.bindLifecycle(viewLifecycleOwner)
                bind.magnetZone.visibility = View.GONE
            }
            (mDeviceModel.ssmLockLiveData.value as? CHSesame5)?.let {
                bind.ssmView.setLock(it)
                mDeviceModel.ssmosLockDelegates[it] = object : CHDeviceStatusDelegate {
                    override fun onMechStatus(device: CHDevices) {
                        bind.ssmView.setLock(it)
                    }
                }.bindLifecycle(viewLifecycleOwner)
            }

            bind.ssmView.setOnClickListener {
                (mDeviceModel.ssmLockLiveData.value as? CHSesame2)?.toggle() {}
                (mDeviceModel.ssmLockLiveData.value as? CHSesame5)?.toggle(historytag = UserUtils.getUserIdWithByte()) {}
            }

            bind.setunlockZone.setOnClickListener {

                if ((mDeviceModel.ssmLockLiveData.value as CHDevices).deviceStatus.value == CHDeviceLoginStatus.UnLogin) {
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
                if ((mDeviceModel.ssmLockLiveData.value as CHDevices).deviceStatus.value == CHDeviceLoginStatus.UnLogin) {
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


    }//end view created


}

