package candyhouse.sesameos.ir.domain.bizAdapter.light

import android.content.Context
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.RemoteAdapterFactory
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.handleBase.HandlerConfigAdapter
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.uiBase.UIConfigAdapter
import candyhouse.sesameos.ir.domain.bizAdapter.light.handler.LightControllerHandlerAdapter
import candyhouse.sesameos.ir.domain.bizAdapter.light.ui.LightControllerConfigAdapter

class LightControllerRemoteAdapterFactory: RemoteAdapterFactory {
    override fun createUIConfigAdapter(context: Context): UIConfigAdapter {
        return LightControllerConfigAdapter(context)
    }

    override fun createHandlerConfigAdapter(context: Context, uiConfigAdapter: UIConfigAdapter): HandlerConfigAdapter {
        return LightControllerHandlerAdapter(context, uiConfigAdapter)
    }
}