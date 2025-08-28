package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.light

import android.content.Context
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.RemoteAdapterFactory
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.handleBase.HandlerConfigAdapter
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.uiBase.UIConfigAdapter
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.light.handler.LightControllerHandlerAdapter
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.light.ui.LightControllerConfigAdapter

class LightControllerRemoteAdapterFactory: RemoteAdapterFactory {
    override fun createUIConfigAdapter(context: Context): UIConfigAdapter {
        return LightControllerConfigAdapter(context)
    }

    override fun createHandlerConfigAdapter(context: Context, uiConfigAdapter: UIConfigAdapter): HandlerConfigAdapter {
        return LightControllerHandlerAdapter(context, uiConfigAdapter)
    }
}