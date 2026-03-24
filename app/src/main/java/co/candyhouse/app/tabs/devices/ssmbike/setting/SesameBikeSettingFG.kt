package co.candyhouse.app.tabs.devices.ssmbike.setting

import android.os.Bundle
import android.view.View
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceSettingFG
import co.candyhouse.app.databinding.FgSsmBikeBot2settingBinding
import co.candyhouse.sesame.open.device.CHProductModel
import co.utils.safeNavigate

class SesameBikeSettingFG : BaseDeviceSettingFG<FgSsmBikeBot2settingBinding>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind.clickScriptZone.visibility = View.GONE

        usePressText()

        if (mDeviceModel.ssmLockLiveData.value!!.productModel == CHProductModel.BiKeLock3) {
            bind.fpZone.visibility = View.VISIBLE
            bind.fpZone.setOnClickListener {
                safeNavigate(R.id.to_SesameKeyboardFingerprint)
            }
        } else {
            bind.fpZone.visibility = View.GONE
        }
    }

    override fun getViewBinder() = FgSsmBikeBot2settingBinding.inflate(layoutInflater)
}
