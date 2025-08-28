package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.handleBase

import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrControlItem
import co.candyhouse.server.CHResult
import co.candyhouse.sesame.open.device.CHHub3

interface HandlerConfigAdapter {
    fun handleItemClick(item: IrControlItem, device: CHHub3, remoteDevice: IrRemote)
    fun modifyIRDeviceInfo(device: CHHub3, remoteDevice: IrRemote,onResponse: CHResult<Any>)
    fun clearHandlerCache()
    fun setHandlerCallback(handlerCallback: HandlerCallback)
    fun getCurrentState(device: CHHub3, remoteDevice: IrRemote): String
    fun getCurrentIRType(): Int
    fun addIrDeviceToMatter(irRemote: IrRemote?, hub3: CHHub3)
}