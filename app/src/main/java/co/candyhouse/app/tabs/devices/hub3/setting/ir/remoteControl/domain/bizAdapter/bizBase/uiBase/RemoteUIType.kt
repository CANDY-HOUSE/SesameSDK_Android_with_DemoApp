package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.uiBase

import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.IRType

enum class RemoteUIType(val configPath: String, val irType: Int) {

    AIR("config/air_control_config.json", IRType.DEVICE_REMOTE_AIR),
    LIGHT("config/light_control_config.json", IRType.DEVICE_REMOTE_LIGHT),
    TV("config/tv_control_config.json", IRType.DEVICE_REMOTE_TV),
    FAN("config/fan_control_config.json", IRType.DEVICE_REMOTE_FANS),
    ERROR("", 0)
}