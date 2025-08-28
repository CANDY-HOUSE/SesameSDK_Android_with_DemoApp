package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.uiBase

import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrControlItem

interface ConfigUpdateCallback {
    fun onItemUpdate(item: IrControlItem)
}