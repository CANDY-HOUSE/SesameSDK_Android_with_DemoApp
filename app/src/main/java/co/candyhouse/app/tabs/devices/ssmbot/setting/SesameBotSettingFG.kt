package co.candyhouse.app.tabs.devices.ssmbot.setting

import android.os.Bundle
import android.view.View
import android.widget.TextView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceSettingFG
import co.candyhouse.sesame.open.device.*
import kotlinx.android.synthetic.main.fg_ssm_bot_setting.*

class SesameBotSettingFG : BaseDeviceSettingFG(R.layout.fg_ssm_bot_setting) {

    override fun onUIDeviceStatus(status: CHDeviceStatus) {
        super.onUIDeviceStatus(status)
        if (status.value == CHDeviceLoginStatus.Login) {
            (mDeviceModel.ssmLockLiveData.value as? CHSesameBot)?.mechSetting?.let {
                mode_txt.text = getString(botMode(it).i18nResources())
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        change_mode_zone.setOnClickListener {
            (mDeviceModel.ssmLockLiveData.value as CHSesameBot).mechSetting?.let { setting ->
//                L.d("hcia", "setting:" + setting)
                (mDeviceModel.ssmLockLiveData.value as CHSesameBot).updateSetting(botMode(setting).changeNextMode(setting)) {
                    mode_txt.post {
                        mode_txt.text = getString(botMode(setting).i18nResources())
                    }
                }
            }
        }
        mDeviceModel.ssmLockLiveData.observe(viewLifecycleOwner) { ssm ->
            (ssm as? CHSesameBot)?.mechSetting?.let {
                getView()?.findViewById<TextView>(R.id.mode_txt)?.text = getString(botMode(it).i18nResources())
            }
        }


    }//end view created

}

