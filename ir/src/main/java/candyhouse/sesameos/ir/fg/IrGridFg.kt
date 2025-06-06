package candyhouse.sesameos.ir.fg

import android.os.Bundle
import android.view.View
import candyhouse.sesameos.ir.adapter.IrGridAdapter

import candyhouse.sesameos.ir.base.CHHub3IRCode
import candyhouse.sesameos.ir.databinding.FgIrGridBinding
import candyhouse.sesameos.ir.ext.Config
import candyhouse.sesameos.ir.ext.IRDeviceType

class IrGridFg : IrBaseFG<FgIrGridBinding>() {
    override fun getViewBinder() = FgIrGridBinding.inflate(layoutInflater)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val key = arguments?.getInt(Config.productKey) ?: -1
        val list = keyToListModels(key)

        bind.ryView.adapter =
            IrGridAdapter(
                requireContext(),
                list,
                true,
                onClickItem = { },
                onLongClickItem = { position, item -> {} })
        setTitle(arguments?.getString(Config.productName) ?: "")

    }

    private fun keyToListModels(key: Int): List<CHHub3IRCode> {
        var value = mutableListOf<CHHub3IRCode>()
        if (key == IRDeviceType.DEVICE_REMOTE_AIR) {
            value = mutableListOf(CHHub3IRCode(name = "电源1"))
        } else if (key == IRDeviceType.DEVICE_REMOTE_HW) {
            value = mutableListOf(CHHub3IRCode(name = "电源2"))
        } else if (key == IRDeviceType.DEVICE_REMOTE_AP) {
            value = mutableListOf(CHHub3IRCode(name = "电源3"))
        } else if (key == IRDeviceType.DEVICE_REMOTE_TV) {
            value = mutableListOf(CHHub3IRCode(name = "电源4"))
        } else if (key == IRDeviceType.DEVICE_REMOTE_IPTV) {
            value = mutableListOf(CHHub3IRCode(name = "电源5"))
        } else if (key == IRDeviceType.DEVICE_REMOTE_BOX) {
            value = mutableListOf(CHHub3IRCode(name = "电源6"))
        } else if (key == IRDeviceType.DEVICE_REMOTE_DVD) {
            value = mutableListOf(CHHub3IRCode(name = "电源7"))
        } else if (key == IRDeviceType.DEVICE_REMOTE_FANS) {
            value = mutableListOf(CHHub3IRCode(name = "电源8"))
        } else if (key == IRDeviceType.DEVICE_REMOTE_PJT) {
            value = mutableListOf(CHHub3IRCode(name = "电源"))
        } else if (key == IRDeviceType.DEVICE_REMOTE_LIGHT) {
            value = mutableListOf(CHHub3IRCode(name = "电源"))
        } else if (key == IRDeviceType.DEVICE_REMOTE_DC) {
            value = mutableListOf(CHHub3IRCode(name = "电源"))
        } else if (key == IRDeviceType.DEVICE_REMOTE_AUDIO) {
            value = mutableListOf(CHHub3IRCode(name = "电源"))
        } else if (key == IRDeviceType.DEVICE_REMOTE_ROBOT) {
            value = mutableListOf(CHHub3IRCode(name = "电源"))
        }
        return value
    }
}