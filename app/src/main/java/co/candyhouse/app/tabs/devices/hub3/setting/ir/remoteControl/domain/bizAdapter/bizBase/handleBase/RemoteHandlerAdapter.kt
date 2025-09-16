package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.handleBase

import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrControlItem
import co.candyhouse.server.CHResult

interface RemoteHandlerAdapter {
    fun handleItemClick(item: IrControlItem, hub3DeviceId: String, remoteDevice: IrRemote)
    fun modifyIRDeviceInfo(hub3DeviceId: String, remoteDevice: IrRemote, onResponse: CHResult<Any>)
    fun clearHandlerCache()
    fun setHandlerCallback(handlerCallback: HandlerCallback)
    fun getCurrentState(hub3DeviceId: String, remoteDevice: IrRemote): String
    fun getCurrentIRType(): Int
    fun addIrDeviceToMatter(irRemote: IrRemote?, hub3DeviceId: String)
}