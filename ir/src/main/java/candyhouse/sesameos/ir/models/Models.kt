package candyhouse.sesameos.ir.models

data class UIControlConfig(
    val controls: List<ControlConfig>,
    val settings: UIParamsSettings
)

data class ControlConfig(
    val id: Int,
    val type: String,
    val titleIndex: Int,
    val titles: List<String> = emptyList(),
    val icons: List<String> = emptyList(),
    val defaultValue: String = "",
    val operateCode:String
){
    override fun toString(): String {
        return "ControlConfig(id=$id, type='$type', titleIndex=$titleIndex, titles=$titles, icons=$icons, defaultValue='$defaultValue', optionCode='$operateCode')"
    }
}

data class UIParamsSettings(
    val temperature: TemperatureSettings,
    val layout: LayoutSettings
)

data class TemperatureSettings(
    val min: Int,
    val max: Int,
    val default: Int,
    val step: Int
)

data class LayoutSettings(
    val columns: Int,
    val spacing: Int
)

data class States(
    val options: List<String>,
    val default: String
)


data class AnimationSettings(
    val duration: Long,
    val type: String
)

enum class ItemType {
    // 空调
    POWER_STATUS_ON,
    POWER_STATUS_OFF,
    TEMPERATURE_VALUE,
    TEMP_CONTROL_ADD,
    TEMP_CONTROL_REDUCE,
    MODE,
    FAN_SPEED,
    WIND_DIRECTION,
    AUTO_WIND_DIRECTION,
    // 电视
    MUTE,
    BACK,
    UP,
    MENU,
    LEFT,
    OK,
    RIGHT,
    VOLUME_UP,
    DOWN,
    CHANNEL_UP,
    VOLUME_DOWN,
    HOME,
    CHANNEL_DOWN,
    // 灯
    BRIGHTNESS_UP,
    BRIGHTNESS_DOWN,
    COLOR_TEMP_UP,
    COLOR_TEMP_DOWN,

    POWER
}
data class IrControlItem(
    val id: Int,
    val type: ItemType,
    val title: String,
    val value: String = "",
    val isSelected: Boolean = false,
    val iconRes: Int,
    val optionCode:String
)