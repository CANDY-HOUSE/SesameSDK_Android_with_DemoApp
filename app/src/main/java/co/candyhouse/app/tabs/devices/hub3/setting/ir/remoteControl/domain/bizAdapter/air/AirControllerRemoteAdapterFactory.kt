package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.air

import android.content.Context
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.air.handler.AirControllerHandlerAdapter
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.air.ui.AirControllerConfigAdapter
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.RemoteAdapterFactory
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.handleBase.HandlerConfigAdapter
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.uiBase.UIConfigAdapter

class AirControllerRemoteAdapterFactory: RemoteAdapterFactory {
    override fun createUIConfigAdapter(context: Context): UIConfigAdapter {
        return AirControllerConfigAdapter(context)
    }

    override fun createHandlerConfigAdapter(context: Context, uiConfigAdapter: UIConfigAdapter): HandlerConfigAdapter {
        return AirControllerHandlerAdapter(context, uiConfigAdapter)
    }
}
