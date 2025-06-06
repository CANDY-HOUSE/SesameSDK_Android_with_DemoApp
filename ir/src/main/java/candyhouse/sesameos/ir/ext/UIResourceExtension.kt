package candyhouse.sesameos.ir.ext

import android.content.Context
import candyhouse.sesameos.ir.R
import co.candyhouse.sesame.utils.L

object UIResourceExtension {

    fun getIconResourceIds(context: Context, iconNames: List<String>): List<Int> {
        return iconNames.map { iconName ->
            context.resources.getIdentifier(
                iconName,
                "drawable",
                context.packageName
            )
        }
    }

    fun getTemperatureString(context: Context, temperature: Int): String {
        return "$temperature" + context.getString(R.string.temperature_format)
    }

    fun getStringResource(context: Context, resourceName: String): String {
        val resourceId = context.resources.getIdentifier(
            resourceName,
            "string",
            context.packageName
        )
        return if (resourceId != 0) {
            context.getString(resourceId)
        } else resourceName
    }

    fun getIconResourceId(context: Context, iconName: String): Int {
        return context.resources.getIdentifier(
            iconName,
            "drawable",
            context.packageName
        )
    }



    private val stringMap = mapOf(
        // Power Control
        "ir_remote_power_on" to R.string.ir_remote_power_on,
        "ir_remote_power_off" to R.string.ir_remote_power_off,

        // Temperature
        "air_conditioner_temperature" to R.string.air_conditioner_temperature,

        // Mode
        "air_conditioner_model_auto_01" to R.string.air_conditioner_model_auto_01,
        "air_conditioner_model_cold_02" to R.string.air_conditioner_model_cold_02,
        "air_conditioner_model_dry_03" to R.string.air_conditioner_model_dry_03,
        "air_conditioner_model_wind_04" to R.string.air_conditioner_model_wind_04,
        "air_conditioner_model_hot_05" to R.string.air_conditioner_model_hot_05,

        // Fan Speed
        "air_conditioner_wind_speed_auto" to R.string.air_conditioner_wind_speed_auto,
        "air_conditioner_wind_speed_v1" to R.string.air_conditioner_wind_speed_v1,
        "air_conditioner_wind_speed_v2" to R.string.air_conditioner_wind_speed_v2,
        "air_conditioner_wind_speed_v3" to R.string.air_conditioner_wind_speed_v3,

        // Vertical Swing
        "air_conditioner_wind_vertical_v1" to R.string.air_conditioner_wind_vertical_v1,
        "air_conditioner_wind_vertical_v2" to R.string.air_conditioner_wind_vertical_v2,
        "air_conditioner_wind_vertical_v3" to R.string.air_conditioner_wind_vertical_v3,

        // Horizontal Swing
        "air_conditioner_wind_horizontal_auto" to R.string.air_conditioner_wind_horizontal_auto,
        "air_conditioner_wind_horizontal_stop" to R.string.air_conditioner_wind_horizontal_stop,

        "ir_remote_power" to R.string.ir_remote_power,
        "tv_remote_mute" to R.string.tv_remote_mute,
        "tv_remote_back" to R.string.tv_remote_back,
        "tv_remote_up" to R.string.tv_remote_up,
        "tv_remote_menu" to R.string.tv_remote_menu,
        "tv_remote_left" to R.string.tv_remote_left,
        "tv_remote_ok" to R.string.tv_remote_ok,
        "tv_remote_right" to R.string.tv_remote_right,
        "tv_remote_volume_up" to R.string.tv_remote_volume_up,
        "tv_remote_down" to R.string.tv_remote_down,
        "tv_remote_channel_up" to R.string.tv_remote_channel_up,
        "tv_remote_volume_down" to R.string.tv_remote_volume_down,
        "tv_remote_home" to R.string.tv_remote_home,
        "tv_remote_channel_down" to R.string.tv_remote_channel_down,

        "ir_mode" to R.string.ir_mode,
        "light_mode_normal" to R.string.light_mode_normal,
        "light_mode_reading" to R.string.light_mode_reading,
        "light_mode_working" to R.string.light_mode_working,
        "light_mode_evening" to R.string.light_mode_evening,
        "light_brightness_up" to R.string.light_brightness_up,
        "light_brightness_down" to R.string.light_brightness_down,
        "light_color_temp_up" to R.string.light_color_temp_up,
        "light_color_temp_down" to R.string.light_color_temp_down
    )

    fun getString(context: Context, key: String): String {
        return try {
            val resId = stringMap[key] ?: return key
            context.getString(resId)
        } catch (e: Exception) {
            L.e("StringResourceMapper", "Error getting string for key: $key", e)
            key
        }
    }
}