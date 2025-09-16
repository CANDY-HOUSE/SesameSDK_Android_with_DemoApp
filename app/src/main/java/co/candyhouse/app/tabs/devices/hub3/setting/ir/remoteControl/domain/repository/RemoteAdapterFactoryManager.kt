package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.repository

import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.air.AirAdapterFactory
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.IRType
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.RemoteAdapterFactory
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.nonAir.NonAirAdapterFactory
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.uiBase.RemoteUIType


object RemoteAdapterFactoryManager {
    fun getFactory(type: Int): RemoteAdapterFactory {
        return when (type) {
            IRType.DEVICE_REMOTE_AIR -> AirAdapterFactory()
            IRType.DEVICE_REMOTE_TV, IRType.DEVICE_REMOTE_LIGHT, IRType.DEVICE_REMOTE_FANS -> NonAirAdapterFactory()
            else -> throw IllegalArgumentException("Unknown remote type $type")
        }
    }

    fun getUIType(type: Int): RemoteUIType {
        return when (type) {
            IRType.DEVICE_REMOTE_AIR -> RemoteUIType.AIR
            IRType.DEVICE_REMOTE_TV -> RemoteUIType.TV
            IRType.DEVICE_REMOTE_LIGHT -> RemoteUIType.LIGHT
            IRType.DEVICE_REMOTE_FANS -> RemoteUIType.FAN
            else -> RemoteUIType.ERROR
        }
    }
}