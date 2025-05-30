package candyhouse.sesameos.ir.domain.bizAdapter.air

import android.content.Context
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.RemoteAdapterFactory
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.handleBase.HandlerConfigAdapter
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.uiBase.UIConfigAdapter
import candyhouse.sesameos.ir.domain.bizAdapter.air.ui.AirControllerConfigAdapter
import candyhouse.sesameos.ir.domain.bizAdapter.air.handler.AirControllerHandlerAdapter

class AirControllerRemoteAdapterFactory: RemoteAdapterFactory {
    override fun createUIConfigAdapter(context: Context): UIConfigAdapter {
        return AirControllerConfigAdapter(context)
    }

    override fun createHandlerConfigAdapter(context: Context, uiConfigAdapter: UIConfigAdapter): HandlerConfigAdapter {
        return AirControllerHandlerAdapter(context, uiConfigAdapter)
    }
}
