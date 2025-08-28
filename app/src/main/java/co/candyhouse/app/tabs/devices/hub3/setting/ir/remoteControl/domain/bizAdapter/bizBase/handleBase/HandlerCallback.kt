package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.handleBase

import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrControlItem

interface HandlerCallback {
    fun onItemUpdate(item: IrControlItem)
}