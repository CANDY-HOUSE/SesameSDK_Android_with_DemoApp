package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.nonAir

import android.content.Context
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.RemoteAdapterFactory
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.handleBase.RemoteHandlerAdapter
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.uiBase.RemoteUIType
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.uiBase.RemoteUIAdapter
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.nonAir.handler.NonAirHandlerAdapter
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.nonAir.ui.NonAirUIAdapter

class NonAirAdapterFactory : RemoteAdapterFactory {
    override fun createUIAdapter(context: Context, uiType: RemoteUIType): RemoteUIAdapter {
        return NonAirUIAdapter(context, uiType)
    }

    override fun createHandlerAdapter(context: Context, uiAdapter: RemoteUIAdapter): RemoteHandlerAdapter {
        return NonAirHandlerAdapter(context, uiAdapter)
    }
}