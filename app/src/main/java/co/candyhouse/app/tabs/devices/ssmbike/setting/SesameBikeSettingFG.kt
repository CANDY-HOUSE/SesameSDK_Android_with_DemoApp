package co.candyhouse.app.tabs.devices.ssmbike.setting

import android.os.Bundle
import android.view.View
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceSettingFG
import co.candyhouse.app.databinding.FgSsmBikeSettingBinding
import co.candyhouse.sesame.utils.L

class SesameBikeSettingFG : BaseDeviceSettingFG<FgSsmBikeSettingBinding>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        L.d("hcia", "SesameBikeSettingFG onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        getView()?.findViewById<View>(R.id.click_script_zone)?.visibility = View.GONE

        usePressText()

        showBatteryLevel(bind.battery, mDeviceModel.ssmLockLiveData.value)
    }//end view created
    override fun getViewBinder()= FgSsmBikeSettingBinding.inflate(layoutInflater)

}
