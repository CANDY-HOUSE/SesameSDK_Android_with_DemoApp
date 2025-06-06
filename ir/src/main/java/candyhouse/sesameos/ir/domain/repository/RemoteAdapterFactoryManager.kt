package candyhouse.sesameos.ir.domain.repository

import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.RemoteAdapterFactory
import candyhouse.sesameos.ir.domain.bizAdapter.air.AirControllerRemoteAdapterFactory
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.IRType
import candyhouse.sesameos.ir.domain.bizAdapter.light.LightControllerRemoteAdapterFactory
import candyhouse.sesameos.ir.domain.bizAdapter.tv.TVControllerRemoteAdapterFactory


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