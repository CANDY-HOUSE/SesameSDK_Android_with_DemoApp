package candyhouse.sesameos.ir.domain.bizAdapter.bizBase.handleBase

import candyhouse.sesameos.ir.models.ItemType
import co.candyhouse.sesame.utils.L

class HXDParametersSwapper {

    val tag = HXDParametersSwapper::class.java.simpleName

// hxd 提供能力：电源(power)、温度(temperature)、模式(mode)、风量(fanSpeed)、风向(windDirection)、自动风向(autoWindDirection)的索引和数值转换
     fun getPowerIndex(value: Int): Boolean {
        return value == 0x01
    }

    fun getPowerValue(isPowerOn: Boolean) = if(isPowerOn) 0x01 else 0x00

    fun getTemperature(value :Int): Int { // 温度暂不作转换
        return value
    }

     fun getModeIndex(value: Int): Int {
        return when (value) {
            0x01 -> 0
            0x02 -> 1
            0x03 -> 2
            0x04 -> 3
            0x05 -> 4
            else -> 0
        }
    }

    fun getModeValue(index: Int): Int {
        return when (index) {
            0 -> 0x01 // 自动
            1 -> 0x02 // 制冷
            2 -> 0x03 // 抽湿
            3 -> 0x04 // 送风
            4 -> 0x05 // 制热
            else -> 0x01 // 默认制冷
        }
    }

     fun getFanSpeedIndex(value: Int): Int {
        return when (value) {
            0x01 -> 0
            0x02 -> 1
            0x03 -> 2
            0x04 -> 3
            else -> 0
        }
    }

    fun getFanSpeedValue(index: Int): Int {
        return when (index) {
            0 -> 0x01 // 自动
            1 -> 0x02 //低
            2 -> 0x03 //中
            3 -> 0x04 //高
            else -> 0x01
        }
    }

     fun getWindDirectionIndex(value: Int): Int {
        return when (value) {
            0x01 -> 0
            0x02 -> 1
            0x03 -> 2
            else -> 0
        }
    }

    fun getWindDirectionValue(index: Int): Int {
        return when (index) {
            0 -> 0x01  //向上
            1 -> 0x02  //中
            2 -> 0x03  //向下
            else -> 0x02 // 默认02,与显示对应
        }
    }

     fun getAutoWindDirectionIndex(value: Int): Int {
        return when (value) {
            0x01 -> 0
            0x00 -> 1
            else -> 0
        }
    }

    fun getAutoWindDirectionValue(index: Int): Int {
        return when (index) {
            0 -> 0x01 // 打开
            1 -> 0x00 // 关闭
            else -> 0x01
        }
    }

    /**
     * 获取当前按键指令
     * x01//电源
     * 0x02//模式
     * 0x03//风量
     * 0x04//手动风向
     * 0x05//自动风向
     * 0x06//温度加
     * 0x07//温度减
     * 表示当前按下的是哪个键
     */
    fun getAirKey(type: ItemType): Int {
        return when (type) {
            ItemType.POWER_STATUS_ON -> 0x01
            ItemType.POWER_STATUS_OFF -> 0x01
            ItemType.TEMP_CONTROL_ADD -> 0x06
            ItemType.TEMP_CONTROL_REDUCE -> 0x07
            ItemType.MODE -> 0x02
            ItemType.FAN_SPEED -> 0x03
            ItemType.WIND_DIRECTION -> 0x04
            ItemType.AUTO_WIND_DIRECTION -> 0x05
            else -> {
                L.e(tag, "Unknown item type")
                0x01
            }
        }
    }

    /**
     * /*   	  开灯			关灯				亮度+			亮度-			模式			设置			定时+			定时-		  色温+			色温-			1			2			3			4			5			6			A			B			C			D*/
     * {0x1d,0x2c,0x25,0x2f,0x26,0x2a,0x23,0x2b,0x22,0x80,0xb9,0x82,0xbb,0xff,0x00,0xff,0x00,0x8b,0xb2,0x8a,0xb3,0xff,0x00,0xff,0x00,0xff,0x00,0xff,0x00,0xff,0x00,0xff,0x00,0xff,0x00,0xff,0x00,0xff,0x00,0xff,0x00,0x2c,0x52,0x09,0x00},
     *  POWER_STATUS_ON = 0x01,
     *  MODE = 0x05,
     *  POWER_STATUS_OFF = 0x02,
     *  BRIGHTNESS_UP = 0x03,
     *  BRIGHTNESS_DOWN = 0x04,
     *  COLOR_TEMP_UP = 0x09,
     *  COLOR_TEMP_DOWN = 0x0A,
     */
    fun getLightKey(type: ItemType): Int {
        return when (type) {
            ItemType.POWER_STATUS_ON -> 0x01
            ItemType.POWER_STATUS_OFF -> 0x02
            ItemType.MODE -> 0x05
            ItemType.BRIGHTNESS_UP -> 0x03
            ItemType.BRIGHTNESS_DOWN -> 0x04
            ItemType.COLOR_TEMP_UP -> 0x09
            ItemType.COLOR_TEMP_DOWN -> 0x0A
            else -> {
                L.e(tag, "Unknown item type")
                0x01
            }
        }
    }

    /**
    KEY_TV_VOLUME_OUT =  0x01,// vol-
    KEY_TV_CHANNEL_IN =  0x02,// ch+
    KEY_TV_MENU =  0x03,// menu
    KEY_TV_CHANNEL_OUT =  0x04,// ch-
    KEY_TV_VOLUME_IN =  0x05,// vol+
    KEY_TV_POWER =  0x06,// power
    KEY_TV_MUTE =  0x07,// mute
    KEY_TV_KEY1 =  0x08,// 1 2 3 4 5 6 7 8 9
    KEY_TV_KEY2 =  0x09,
    KEY_TV_KEY3 =  0x0A,
    KEY_TV_KEY4 =  0x0B,
    KEY_TV_KEY5 =  0x0C,
    KEY_TV_KEY6 =  0x0D,
    KEY_TV_KEY7 =  0x0E,
    KEY_TV_KEY8 =  0x0F,
    KEY_TV_KEY9 =  0x10,
    KEY_TV_SELECT =  0x11,// -/--
    KEY_TV_KEY0 =  0x12,// 0
    KEY_TV_AV_TV =  0x13,// AV/TV
    KEY_TV_BACK =  0x14,// back
    KEY_TV_OK =  0x15,// ok
    KEY_TV_UP =  0x16,// up
    KEY_TV_LEFT =  0x17,// left
    KEY_TV_RIGHT =  0x18,// right
    KEY_TV_DOWN =  0x19,// down
    KEY_TV_HOME =  0x1A,// home
     */

    fun getTVKey(type: ItemType): Int {
        return when (type) {
            ItemType.POWER_STATUS_ON -> 0x06
            ItemType.POWER_STATUS_OFF -> 0x06
            ItemType.MUTE -> 0x07
            ItemType.BACK -> 0x14
            ItemType.UP -> 0x16
            ItemType.MENU -> 0x03
            ItemType.LEFT -> 0x17
            ItemType.OK -> 0x15
            ItemType.RIGHT -> 0x18
            ItemType.VOLUME_UP -> 0x05
            ItemType.DOWN -> 0x19
            ItemType.CHANNEL_UP -> 0x02
            ItemType.VOLUME_DOWN -> 0x01
            ItemType.HOME -> 0x1A
            ItemType.CHANNEL_DOWN -> 0x04
            else -> {
                L.e(tag, "Unknown item type")
                0x01
            }
        }
    }


}