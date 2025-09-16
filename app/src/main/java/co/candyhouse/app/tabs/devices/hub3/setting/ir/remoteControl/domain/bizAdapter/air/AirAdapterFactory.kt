package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.air

import android.content.Context
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.air.handler.AirHandlerAdapter
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.air.ui.AirUIAdapter
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.RemoteAdapterFactory
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.handleBase.RemoteHandlerAdapter
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.uiBase.RemoteUIAdapter
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.uiBase.RemoteUIType

class AirAdapterFactory : RemoteAdapterFactory {
    override fun createUIAdapter(context: Context, uiType: RemoteUIType): RemoteUIAdapter {
        return AirUIAdapter(context, uiType)
    }

    override fun createHandlerAdapter(context: Context, uiAdapter: RemoteUIAdapter): RemoteHandlerAdapter {
        return AirHandlerAdapter(context, uiAdapter)
    }
}
