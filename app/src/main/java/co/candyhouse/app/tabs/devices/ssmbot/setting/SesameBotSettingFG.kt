package co.candyhouse.app.tabs.devices.ssmbot.setting

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import co.candyhouse.app.base.BaseDeviceSettingFG
import co.candyhouse.app.databinding.FgSsmBotSettingBinding
import co.candyhouse.sesame.open.device.*

class SesameBotSettingFG : BaseDeviceSettingFG<FgSsmBotSettingBinding>() {
    override fun getViewBinder()= FgSsmBotSettingBinding.inflate(layoutInflater)

    override fun onUIDeviceStatus(status: CHDeviceStatus) {
        super.onUIDeviceStatus(status)
        if (status.value == CHDeviceLoginStatus.logined) {

            (mDeviceModel.ssmLockLiveData.value as? CHSesameBot)?.mechSetting?.let { setting ->
                // Check if the view lifecycle is at least in the STARTED state
          /*      if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    bind.modeTxt?.text = getString(botMode(setting).i18nResources())
                }*/
                if (isAdded && view != null && viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
               bind.modeTxt.text = getString(botMode(setting).i18nResources())
                }
            }

        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        usePressText()
        val device=mDeviceModel.ssmLockLiveData.value
        if (device is CHSesameBot){
            bind.changeModeZone    .setOnClickListener {
                device.mechSetting?.let { setting ->
                    device.updateSetting(botMode(setting).changeNextMode(setting)) {
                        // Check if the view is still valid
                        bind.modeTxt.let { textView ->
                            textView.post {

                                textView.text = getString(botMode(setting).i18nResources())
                            }
                        }
                    }
                }
            }

            mDeviceModel.ssmLockLiveData.observe(viewLifecycleOwner) { ssm ->
                if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    (ssm as? CHSesameBot)?.mechSetting?.let {
                        bind.modeTxt.text = getString(botMode(it).i18nResources())
                    }
                }
            }
        }




    }//end view created

}

