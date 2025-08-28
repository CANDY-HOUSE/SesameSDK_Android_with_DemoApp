package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.tv

import android.content.Context
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.RemoteAdapterFactory
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.handleBase.HandlerConfigAdapter
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.uiBase.UIConfigAdapter
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.tv.handler.TVControllerHandlerAdapter
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.tv.ui.TVControllerConfigAdapter

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