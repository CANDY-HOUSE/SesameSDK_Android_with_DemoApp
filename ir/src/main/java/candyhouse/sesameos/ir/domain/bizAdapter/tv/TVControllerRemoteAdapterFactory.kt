package candyhouse.sesameos.ir.domain.bizAdapter.tv

import android.content.Context
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.RemoteAdapterFactory
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.handleBase.HandlerConfigAdapter
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.uiBase.UIConfigAdapter
import candyhouse.sesameos.ir.domain.bizAdapter.tv.handler.TVControllerHandlerAdapter
import candyhouse.sesameos.ir.domain.bizAdapter.tv.ui.TVControllerConfigAdapter

class TVControllerRemoteAdapterFactory : RemoteAdapterFactory {
    override fun createUIConfigAdapter(context: Context): UIConfigAdapter {
        return TVControllerConfigAdapter(context)
    }

    override fun createHandlerConfigAdapter(
        context: Context,
        uiConfigAdapter: UIConfigAdapter
    ): HandlerConfigAdapter {
        return TVControllerHandlerAdapter(context, uiConfigAdapter)
    }
}