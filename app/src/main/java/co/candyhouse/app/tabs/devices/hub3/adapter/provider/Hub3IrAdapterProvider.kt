package co.candyhouse.app.tabs.devices.hub3.adapter.provider

import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote

/**
 *
 *
 * @author frey on 2025/4/10
 */
interface Hub3IrAdapterProvider {

    fun performRemote(data: IrRemote)
    fun deleteIRDevice(data: IrRemote)
}