package co.candyhouse.app.tabs.devices.ssm2.setting


import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.databinding.FgMatterViewBinding

class MatterQrCodeFG : BaseDeviceFG<FgMatterViewBinding>() {
    override fun getViewBinder()= FgMatterViewBinding.inflate(layoutInflater)

}
