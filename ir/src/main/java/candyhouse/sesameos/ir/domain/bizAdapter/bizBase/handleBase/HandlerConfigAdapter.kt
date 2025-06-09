package candyhouse.sesameos.ir.domain.bizAdapter.bizBase.handleBase

import candyhouse.sesameos.ir.base.IrRemote
import candyhouse.sesameos.ir.models.IrControlItem
import candyhouse.sesameos.ir.server.CHResult
import co.candyhouse.sesame.open.device.CHHub3

interface HandlerConfigAdapter {
    fun handleItemClick(item: IrControlItem, device: CHHub3, remoteDevice: IrRemote)
    fun modifyIRDeviceInfo(device: CHHub3, remoteDevice: IrRemote,onResponse: CHResult<Any>)
    fun clearHandlerCache()
    fun setHandlerCallback(handlerCallback: HandlerCallback)
    fun getCurrentState(device: CHHub3, remoteDevice: IrRemote): String
    fun getCurrentIRDeviceType(): Int
}