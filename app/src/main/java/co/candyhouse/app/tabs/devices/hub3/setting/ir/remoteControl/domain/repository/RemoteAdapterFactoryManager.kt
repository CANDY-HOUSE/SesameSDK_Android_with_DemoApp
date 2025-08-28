package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.repository

import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.air.AirControllerRemoteAdapterFactory
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.IRType
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.RemoteAdapterFactory
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.light.LightControllerRemoteAdapterFactory
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.tv.TVControllerRemoteAdapterFactory


object RemoteAdapterFactoryManager {
    fun getFactory(type: Int): RemoteAdapterFactory {
        return when(type) {
            IRType.DEVICE_REMOTE_AIR -> AirControllerRemoteAdapterFactory()
            IRType.DEVICE_REMOTE_TV -> TVControllerRemoteAdapterFactory()
            IRType.DEVICE_REMOTE_LIGHT -> LightControllerRemoteAdapterFactory()
            else -> throw IllegalArgumentException("Unknown remote type $type")
        }
    }
}