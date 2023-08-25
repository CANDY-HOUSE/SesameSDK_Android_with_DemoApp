package co.candyhouse.app.tabs.devices.ssm2.setting.angle

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.R
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.sesame.open.device.*
import co.utils.L
import kotlinx.android.synthetic.main.fg_set_angle.*

class SSM2SetAngleFG : BaseDeviceFG(R.layout.fg_set_angle) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        L.d("hcia", "SSM2SetAngleFG onViewCreated:")
        titlec?.text = mDeviceModel.ssmLockLiveData.value!!.getNickname()

        (mDeviceModel.ssmLockLiveData.value as? CHSesame2)?.let {
            ssmView?.setLock(it)
            mDeviceModel.ssmosLockDelegates[it] = object : CHDeviceStatusDelegate {
                override fun onMechStatus(device: CHDevices) {
                    ssmView?.setLock(it)
                }
            }
            magnet_zone.visibility = View.GONE
        }
        (mDeviceModel.ssmLockLiveData.value as? CHSesame5)?.let {
            ssmView?.setLock(it)
            mDeviceModel.ssmosLockDelegates[it] = object : CHDeviceStatusDelegate {
                override fun onMechStatus(device: CHDevices) {
                    ssmView?.setLock(it)
                }
            }
        }

        ssmView?.setOnClickListener {
            (mDeviceModel.ssmLockLiveData.value as? CHSesame2)?.toggle {}
            (mDeviceModel.ssmLockLiveData.value as? CHSesame5)?.toggle {}
        }

        setunlock_zone?.setOnClickListener {

            if ((mDeviceModel.ssmLockLiveData.value as CHDevices).deviceStatus.value == CHDeviceLoginStatus.UnLogin) {
                return@setOnClickListener
            }

            (mDeviceModel.ssmLockLiveData.value as? CHSesame2)?.let { device ->
                device.configureLockPosition(device.mechSetting!!.lockPosition, device.mechStatus!!.position) {
                    ssmView?.setLock(device)
                }
            }
            (mDeviceModel.ssmLockLiveData.value as? CHSesame5)?.let { device ->
                device.configureLockPosition(device.mechSetting!!.lockPosition, device.mechStatus!!.position) {
                    ssmView?.setLock(device)
                }
            }

        }
        setlock_zone?.setOnClickListener {
            if ((mDeviceModel.ssmLockLiveData.value as CHDevices).deviceStatus.value == CHDeviceLoginStatus.UnLogin) {
                return@setOnClickListener
            }

            (mDeviceModel.ssmLockLiveData.value as? CHSesame2)?.let { device ->
                device.configureLockPosition(device.mechStatus!!.position, device.mechSetting!!.unlockPosition) {
                    ssmView?.setLock(device)
                }
            }
            (mDeviceModel.ssmLockLiveData.value as? CHSesame5)?.let { device ->
                device.configureLockPosition(device.mechStatus!!.position, device.mechSetting!!.unlockPosition) {
                    ssmView?.setLock(device)
                }
            }

        }


        magnet_zone?.setOnClickListener {
            (mDeviceModel.ssmLockLiveData.value as? CHSesame5)?.magnet {}
        }


    }//end view created


}

