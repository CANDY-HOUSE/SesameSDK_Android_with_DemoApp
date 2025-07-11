package co.candyhouse.app.tabs.devices.hub3.adapter.provider

import candyhouse.sesameos.ir.base.IrRemote

/**
 *
 *
 * @author frey on 2025/4/10
 */
interface Hub3IrAdapterProvider {
    fun setIrRemote(data: IrRemote)

    fun performStudy(data: IrRemote)

    fun performAirControl(data: IrRemote)

    fun deleteIRDevice(data: IrRemote)
}