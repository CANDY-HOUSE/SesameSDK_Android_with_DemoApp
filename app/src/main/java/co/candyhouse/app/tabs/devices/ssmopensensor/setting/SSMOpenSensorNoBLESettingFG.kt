package co.candyhouse.app.tabs.devices.ssmopensensor.setting

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceSettingFG
import co.candyhouse.app.databinding.FgSesameOpensensorNobleSettingBinding
import co.candyhouse.app.tabs.devices.ssm2.*
import co.candyhouse.sesame.open.device.*
import co.candyhouse.sesame.utils.L
import java.util.Date

class SesameOpenSensorNoBLESettingFG :
    BaseDeviceSettingFG<FgSesameOpensensorNobleSettingBinding>() {
    override fun getViewBinder() = FgSesameOpensensorNobleSettingBinding.inflate(layoutInflater)

    override fun onResume() {
        super.onResume()
    }

    @SuppressLint("SimpleDateFormat")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val device = mDeviceModel.ssmLockLiveData.value

        bind.batteryZone.visibility = View.GONE
        bind.openSensorStatusZone.visibility = View.GONE
        bind.openSensorLastUpdateTimeZone.visibility = View.GONE
        bind.batteryZone.visibility = View.GONE
        bind.openSensorStatusZone.visibility = View.GONE
        bind.openSensorLastUpdateTimeZone.visibility = View.GONE
        if (device != null) {
            bind.pleaseResetOpenSensor.text =
                getString(R.string.pleaseResetOpenSensor, device.getNickname())
            bind.trashDeviceKeyTxt.text =
                getString(R.string.trash_device_key, device.getNickname())
        }
    }
}