package co.candyhouse.app.tabs.devices.ssmtouchpro.setting

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceSettingFG
import co.candyhouse.app.tabs.devices.ssm2.*
import co.candyhouse.sesame.open.device.*
import co.utils.L
import kotlinx.android.synthetic.main.fg_sesame_opensensor_noble_setting.*
import kotlinx.android.synthetic.main.fg_sesame_touchpro_setting.*
import kotlinx.android.synthetic.main.fg_sesame_touchpro_setting.trash_device_key_txt
import kotlinx.android.synthetic.main.fg_wm2_setting.*

class SesameOpenSensorNoBLESettingFG : BaseDeviceSettingFG(R.layout.fg_sesame_opensensor_noble_setting), CHSesameTouchProDelegate {

    override fun onResume() {
        super.onResume()
        mDeviceModel.ssmosLockDelegates[(mDeviceModel.ssmLockLiveData.value!! as CHSesameTouchPro)] = object : CHSesameTouchProDelegate {
            override fun onBleDeviceStatusChanged(device: CHDevices, status: CHDeviceStatus, shadowStatus: CHDeviceStatus?) {
                onChange()
                onUIDeviceStatus(status)
                checkVersionTag(status, device)
                if (device.deviceStatus == CHDeviceStatus.ReceivedAdV) {
                    device.connect { }
                }
            }

        }
    }

    @SuppressLint("SimpleDateFormat")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        L.d("harry", "【OpenSensor】 没有蓝牙信号的 设置页面")
        super.onViewCreated(view, savedInstanceState)
        val mDevice: CHSesameTouchPro = mDeviceModel.ssmLockLiveData.value!! as CHSesameTouchPro
        pleaseResetOpenSensor.text = getString(R.string.pleaseResetOpenSensor)
        trash_device_key_txt.text = getString(R.string.trash_device_key, mDevice.productModel.modelName())
    }

    override fun onDestroy() {
        super.onDestroy()
        mDeviceModel.ssmLockLiveData.value?.disconnect { }
    }
}