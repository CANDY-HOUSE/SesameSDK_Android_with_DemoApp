package candyhouse.sesameos.ir.ext

object IRDeviceType {
    // 電視
    const val DEVICE_REMOTE_TV = 0x2000
    // 機上盒
    const val DEVICE_REMOTE_STB = 0x4000
    // 影碟機
    const val DEVICE_REMOTE_DVD = 0x6000
    // 風扇
    const val DEVICE_REMOTE_FANS = 0x8000
    // 投影機
    const val DEVICE_REMOTE_PJT = 0xA000
    // 空調
    const val DEVICE_REMOTE_AIR = 0xC000
    // 遙控燈
    const val DEVICE_REMOTE_LIGHT = 0xE000
    // IPTV
    const val DEVICE_REMOTE_IPTV = 0x2100
    // 數位相機
    const val DEVICE_REMOTE_DC = 0x2300

    const val DEVICE_REMOTE_BOX = 0x2500
    // 空氣淨化器
    const val DEVICE_REMOTE_AP = 0x2700
    // 音響
    const val DEVICE_REMOTE_AUDIO = 0x2900
    const val DEVICE_REMOTE_POWER = 0x2B00
    const val DEVICE_REMOTE_SLR = 0x2D00
    // 熱水器
    const val DEVICE_REMOTE_HW = 0x2F00
    const val DEVICE_REMOTE_ROBOT = 0x3100
    const val DEVICE_REMOTE_CUSTOM = 0xFE00
    const val DEVICE_REMOTE_DIY= 0xFEFF
    object  REMOTE_KEY_STB {
        const val  KEY_COUNT = 23
        const val  KEY_STB_AWAIT = DEVICE_REMOTE_STB or 0x01
        const val  KEY_STB_KEY1 = DEVICE_REMOTE_STB or 0x03
        const val  KEY_STB_KEY2 = DEVICE_REMOTE_STB or 0x05
        const val  KEY_STB_KEY3 = DEVICE_REMOTE_STB or 0x07
        const val  KEY_STB_KEY4 = DEVICE_REMOTE_STB or 0x09
        const val  KEY_STB_KEY5 = DEVICE_REMOTE_STB or 0x0B
        const val  KEY_STB_KEY6 = DEVICE_REMOTE_STB or 0x0D
        const val  KEY_STB_KEY7 = DEVICE_REMOTE_STB or 0x0F
        const val  KEY_STB_KEY8 = DEVICE_REMOTE_STB or 0x11
        const val  KEY_STB_KEY9 = DEVICE_REMOTE_STB or 0x13
        const val  KEY_STB_GUIDE = DEVICE_REMOTE_STB or 0x15
        const val  KEY_STB_KEY0 = DEVICE_REMOTE_STB or 0x17
        const val  KEY_STB_BACK = DEVICE_REMOTE_STB or 0x19
        const val  KEY_STB_UP = DEVICE_REMOTE_STB or 0x1B
        const val  KEY_STB_LEFT = DEVICE_REMOTE_STB or 0x1D
        const val  KEY_STB_OK = DEVICE_REMOTE_STB or 0x1F
        const val  KEY_STB_RIGHT = DEVICE_REMOTE_STB or 0x21
        const val  KEY_STB_DOWN = DEVICE_REMOTE_STB or 0x23
        const val  KEY_STB_VOLUME_IN = DEVICE_REMOTE_STB or 0x25
        const val  KEY_STB_VOLUME_OUT = DEVICE_REMOTE_STB or 0x27
        const val  KEY_STB_CHANNEL_IN = DEVICE_REMOTE_STB or 0x29
        const val  KEY_STB_CHANNEL_OUT = DEVICE_REMOTE_STB or 0x2B
        const val  KEY_STB_MENU = DEVICE_REMOTE_STB or 0x2D
    }
    object REMOTE_KEY_TV {
        const val KEY_COUNT = 26
        const val KEY_TV_VOLUME_OUT = DEVICE_REMOTE_TV or 0x01
        const val KEY_TV_CHANNEL_IN = DEVICE_REMOTE_TV or 0x03
        const val KEY_TV_MENU = DEVICE_REMOTE_TV or 0x05
        const val KEY_TV_CHANNEL_OUT = DEVICE_REMOTE_TV or 0x07
        const val KEY_TV_VOLUME_IN = DEVICE_REMOTE_TV or 0x09
        const val KEY_TV_POWER = DEVICE_REMOTE_TV or 0x0B
        const val KEY_TV_MUTE = DEVICE_REMOTE_TV or 0x0D
        const val KEY_TV_KEY1 = DEVICE_REMOTE_TV or 0x0F
        const val KEY_TV_KEY2 = DEVICE_REMOTE_TV or 0x11
        const val KEY_TV_KEY3 = DEVICE_REMOTE_TV or 0x13
        const val KEY_TV_KEY4 = DEVICE_REMOTE_TV or 0x15
        const val KEY_TV_KEY5 = DEVICE_REMOTE_TV or 0x17
        const val KEY_TV_KEY6 = DEVICE_REMOTE_TV or 0x19
        const val KEY_TV_KEY7 = DEVICE_REMOTE_TV or 0x1B
        const val KEY_TV_KEY8 = DEVICE_REMOTE_TV or 0x1D
        const val KEY_TV_KEY9 = DEVICE_REMOTE_TV or 0x1F
        const val KEY_TV_SELECT = DEVICE_REMOTE_TV or 0x21
        const val KEY_TV_KEY0 = DEVICE_REMOTE_TV or 0x23
        const val KEY_TV_AV_TV = DEVICE_REMOTE_TV or 0x25
        const val KEY_TV_BACK = DEVICE_REMOTE_TV or 0x27
        const val KEY_TV_OK = DEVICE_REMOTE_TV or 0x29
        const val KEY_TV_UP = DEVICE_REMOTE_TV or 0x2B
        const val KEY_TV_LEFT = DEVICE_REMOTE_TV or 0x2D
        const val KEY_TV_RIGHT = DEVICE_REMOTE_TV or 0x2F
        const val KEY_TV_DOWN = DEVICE_REMOTE_TV or 0x31
        const val KEY_TV_HOME = DEVICE_REMOTE_TV or 0x33
    }

    // 其他REMOTE_KEY_*对象的定义与REMOTE_KEY_TV类似，这里省略...

    object REMOTE_KEY_ROBOT {
        const val KEY_COUNT = 21
        const val KEY_ROBOT_POWER_ON = DEVICE_REMOTE_ROBOT or 0x01
        const val KEY_ROBOT_POWER_OFF = DEVICE_REMOTE_ROBOT or 0x03
        const val KEY_ROBOT_UP = DEVICE_REMOTE_ROBOT or 0x05
        const val KEY_ROBOT_DOWN = DEVICE_REMOTE_ROBOT or 0x07
        const val KEY_ROBOT_LEFT = DEVICE_REMOTE_ROBOT or 0x09
        const val KEY_ROBOT_RIGHT = DEVICE_REMOTE_ROBOT or 0x0B
        const val KEY_ROBOT_OK = DEVICE_REMOTE_ROBOT or 0x0D
        const val KEY_ROBOT_HC = DEVICE_REMOTE_ROBOT or 0x0F
        const val KEY_ROBOT_MODE = DEVICE_REMOTE_ROBOT or 0x11
        const val KEY_ROBOT_HOMEPAGE = DEVICE_REMOTE_ROBOT or 0x13
        const val KEY_ROBOT_TIME = DEVICE_REMOTE_ROBOT or 0x15
        const val KEY_ROBOT_QY = DEVICE_REMOTE_ROBOT or 0x17
        const val KEY_ROBOT_YB = DEVICE_REMOTE_ROBOT or 0x19
        const val KEY_ROBOT_JB = DEVICE_REMOTE_ROBOT or 0x1B
        const val KEY_ROBOT_AUTO = DEVICE_REMOTE_ROBOT or 0x1D
        const val KEY_ROBOT_DD = DEVICE_REMOTE_ROBOT or 0x1F
        const val KEY_ROBOT_GX = DEVICE_REMOTE_ROBOT or 0x21
        const val KEY_ROBOT_SJ = DEVICE_REMOTE_ROBOT or 0x23
        const val KEY_ROBOT_YY = DEVICE_REMOTE_ROBOT or 0x25
        const val KEY_ROBOT_SPEED = DEVICE_REMOTE_ROBOT or 0x27
        const val KEY_ROBOT_SET = DEVICE_REMOTE_ROBOT or 0x29
    }
     object REMOTE_KEY_FANS {
        const val  KEY_COUNT = 22
        const val  KEY_FANS_POWER = DEVICE_REMOTE_FANS or 0x01
        const val  KEY_FANS_WIND_SPEED = DEVICE_REMOTE_FANS or 0x03
        const val  KEY_FANS_SHAKE_HEAD = DEVICE_REMOTE_FANS or 0x05
        const val  KEY_FANS_MODE = DEVICE_REMOTE_FANS or 0x07
        const val  KEY_FANS_TIMER = DEVICE_REMOTE_FANS or 0x09
        const val  KEY_FANS_LIGHT = DEVICE_REMOTE_FANS or 0x0B
        const val  KEY_FANS_ANION = DEVICE_REMOTE_FANS or 0x0D
        const val  KEY_FANS_KEY1 = DEVICE_REMOTE_FANS or 0x0F
        const val  KEY_FANS_KEY2 = DEVICE_REMOTE_FANS or 0x11
        const val  KEY_FANS_KEY3 = DEVICE_REMOTE_FANS or 0x13
        const val  KEY_FANS_KEY4 = DEVICE_REMOTE_FANS or 0x15
        const val  KEY_FANS_KEY5 = DEVICE_REMOTE_FANS or 0x17
        const val  KEY_FANS_KEY6 = DEVICE_REMOTE_FANS or 0x19
        const val  KEY_FANS_KEY7 = DEVICE_REMOTE_FANS or 0x1B
        const val  KEY_FANS_KEY8 = DEVICE_REMOTE_FANS or 0x1D
        const val  KEY_FANS_KEY9 = DEVICE_REMOTE_FANS or 0x1F
        const val  KEY_FANS_SLEEP = DEVICE_REMOTE_FANS or 0x21
        const val  KEY_FANS_COOL = DEVICE_REMOTE_FANS or 0x23
        const val  KEY_FANS_AIR_RATE = DEVICE_REMOTE_FANS or 0x25
        const val  KEY_FANS_AIR_RATE_LOW = DEVICE_REMOTE_FANS or 0x27
        const val  KEY_FANS_AIR_RATE_MIDDLE = DEVICE_REMOTE_FANS or 0x29
        const val  KEY_FANS_AIR_RATE_HIGH = DEVICE_REMOTE_FANS or 0x2B
    }

     object REMOTE_KEY_PJT {
        const val  KEY_COUNT = 22
        const val  KEY_PJT_POWER_ON = DEVICE_REMOTE_PJT or 0x01
        const val  KEY_PJT_POWER_OFF = DEVICE_REMOTE_PJT or 0x03
        const val  KEY_PJT_COMPUTER = DEVICE_REMOTE_PJT or 0x05
        const val  KEY_PJT_VIDEO = DEVICE_REMOTE_PJT or 0x07
        const val  KEY_PJT_SIGNAL = DEVICE_REMOTE_PJT or 0x09
        const val  KEY_PJT_ZOOM_IN = DEVICE_REMOTE_PJT or 0x0B
        const val  KEY_PJT_ZOOM_OUT = DEVICE_REMOTE_PJT or 0x0D
        const val  KEY_PJT_PICTURE_IN = DEVICE_REMOTE_PJT or 0x0F
        const val  KEY_PJT_PICTURE_OUT = DEVICE_REMOTE_PJT or 0x11
        const val  KEY_PJT_MENU = DEVICE_REMOTE_PJT or 0x13
        const val  KEY_PJT_OK = DEVICE_REMOTE_PJT or 0x15
        const val  KEY_PJT_UP = DEVICE_REMOTE_PJT or 0x17
        const val  KEY_PJT_LEFT = DEVICE_REMOTE_PJT or 0x19
        const val  KEY_PJT_RIGHT = DEVICE_REMOTE_PJT or 0x1B
        const val  KEY_PJT_DOWN = DEVICE_REMOTE_PJT or 0x1D
        const val  KEY_PJT_EXIT = DEVICE_REMOTE_PJT or 0x1F
        const val  KEY_PJT_VOLUME_IN = DEVICE_REMOTE_PJT or 0x21
        const val  KEY_PJT_VOLUME_OUT = DEVICE_REMOTE_PJT or 0x23
        const val  KEY_PJT_MUTE = DEVICE_REMOTE_PJT or 0x25
        const val  KEY_PJT_AUTOMATIC = DEVICE_REMOTE_PJT or 0x27
        const val  KEY_PJT_PAUSE = DEVICE_REMOTE_PJT or 0x29
        const val  KEY_PJT_BRIGHTNESS = DEVICE_REMOTE_PJT or 0x2B
    }

     object REMOTE_KEY_AIR {
        const val  KEY_COUNT = 18
        const val  KEY_AIR_POWER = DEVICE_REMOTE_AIR or 0x01
        const val  KEY_AIR_MODE = DEVICE_REMOTE_AIR or 0x03
        const val  KEY_AIR_WIND_RATE = DEVICE_REMOTE_AIR or 0x05
        const val  KEY_AIR_WIND_DIRECTION = DEVICE_REMOTE_AIR or 0x07
        const val  KEY_AIR_AUTOMATIC_WIND_DIRECTION = DEVICE_REMOTE_AIR or 0x09
        const val  KEY_AIR_TEMPERATURE_IN = DEVICE_REMOTE_AIR or 0x0B
        const val  KEY_AIR_TEMPERATURE_OUT = DEVICE_REMOTE_AIR or 0x0D
        const val  KEY_AIR_SLEEP = DEVICE_REMOTE_AIR or 0x0F
        const val  KEY_AIR_HEAT = DEVICE_REMOTE_AIR or 0x11
        const val  KEY_AIR_LIGHT = DEVICE_REMOTE_AIR or 0x13
        const val  KEY_AIR_ECO = DEVICE_REMOTE_AIR or 0x15
        const val  KEY_AIR_COOL = DEVICE_REMOTE_AIR or 0x17
        const val  KEY_AIR_HOT = DEVICE_REMOTE_AIR or 0x19
        const val  KEY_AIR_MUTE = DEVICE_REMOTE_AIR or 0x1B
        const val  KEY_AIR_STRONG = DEVICE_REMOTE_AIR or 0x1D
        const val  KEY_AIR_XX = DEVICE_REMOTE_AIR or 0x1F
        const val  KEY_AIR_YY = DEVICE_REMOTE_AIR or 0x21
        const val  KEY_AIR_HF = DEVICE_REMOTE_AIR or 0x23
    }

     object REMOTE_KEY_IPTV {
        const val  KEY_COUNT = 25
        const val  KEY_IPTV_POWER = DEVICE_REMOTE_IPTV or 0x01
        const val  KEY_IPTV_MUTE = DEVICE_REMOTE_IPTV or 0x03
        const val  KEY_IPTV_VOLUME_IN = DEVICE_REMOTE_IPTV or 0x05
        const val  KEY_IPTV_VOLUME_OUT = DEVICE_REMOTE_IPTV or 0x07
        const val  KEY_IPTV_CHANNEL_IN = DEVICE_REMOTE_IPTV or 0x09
        const val  KEY_IPTV_CHANNEL_OUT = DEVICE_REMOTE_IPTV or 0x0B
        const val  KEY_IPTV_UP = DEVICE_REMOTE_IPTV or 0x0D
        const val  KEY_IPTV_LEFT = DEVICE_REMOTE_IPTV or 0x0F
        const val  KEY_IPTV_OK = DEVICE_REMOTE_IPTV or 0x11
        const val  KEY_IPTV_RIGHT = DEVICE_REMOTE_IPTV or 0x13
        const val  KEY_IPTV_DOWN = DEVICE_REMOTE_IPTV or 0x15
        const val  KEY_IPTV_PLAY_PAUSE = DEVICE_REMOTE_IPTV or 0x17
        const val  KEY_IPTV_KEY1 = DEVICE_REMOTE_IPTV or 0x19
        const val  KEY_IPTV_KEY2 = DEVICE_REMOTE_IPTV or 0x1B
        const val  KEY_IPTV_KEY3 = DEVICE_REMOTE_IPTV or 0x1D
        const val  KEY_IPTV_KEY4 = DEVICE_REMOTE_IPTV or 0x1F
        const val  KEY_IPTV_KEY5 = DEVICE_REMOTE_IPTV or 0x21
        const val  KEY_IPTV_KEY6 = DEVICE_REMOTE_IPTV or 0x23
        const val  KEY_IPTV_KEY7 = DEVICE_REMOTE_IPTV or 0x25
        const val  KEY_IPTV_KEY8 = DEVICE_REMOTE_IPTV or 0x27
        const val  KEY_IPTV_KEY9 = DEVICE_REMOTE_IPTV or 0x29
        const val  KEY_IPTV_KEY0 = DEVICE_REMOTE_IPTV or 0x2B
        const val  KEY_IPTV_BACK = DEVICE_REMOTE_IPTV or 0x2D
        const val  KEY_IPTV_HOME = DEVICE_REMOTE_IPTV or 0x2F
        const val  KEY_IPTV_MENU = DEVICE_REMOTE_IPTV or 0x31
    }

    object REMOTE_KEY_DC {
        const val  KEY_COUNT = 1
        const val  KEY_DC_SWITCH = DEVICE_REMOTE_DC or 0x01 // switch
    }

    object REMOTE_KEY_SLR {
        const val  KEY_COUNT = 1
        const val  KEY_SLR_SWITCH = DEVICE_REMOTE_SLR or 0x01 // switch
    }

    object REMOTE_KEY_POWER {
        const val  KEY_COUNT = 6
        const val  KEY_POWER_SWITCH = DEVICE_REMOTE_POWER or 0x01 // switch
        const val  KEY_POWER_SPEED = DEVICE_REMOTE_POWER or 0x03 // wind speed
        const val  KEY_POWER_HEAD = DEVICE_REMOTE_POWER or 0x05 // shake head
        const val  KEY_POWER_MODE = DEVICE_REMOTE_POWER or 0x07 // mode
        const val  KEY_POWER_TIMER = DEVICE_REMOTE_POWER or 0x09 // timer
        const val  KEY_POWER_TIMER1 = DEVICE_REMOTE_POWER or 0x11
    }

    object REMOTE_KEY_LIGHT {
        const val  KEY_COUNT = 20
        const val  KEY_LIGHT_POWER_ON = DEVICE_REMOTE_LIGHT or 0x01
        const val  KEY_LIGHT_POWER_OFF = DEVICE_REMOTE_LIGHT or 0x03
        const val  KEY_LIGHT_LD_UP = DEVICE_REMOTE_LIGHT or 0x05
        const val  KEY_LIGHT_LD_DOWN = DEVICE_REMOTE_LIGHT or 0x07
        const val  KEY_LIGHT_MODE = DEVICE_REMOTE_LIGHT or 0x09
        const val  KEY_LIGHT_SET = DEVICE_REMOTE_LIGHT or 0x0B
        const val  KEY_LIGHT_TIME_UP = DEVICE_REMOTE_LIGHT or 0x0D
        const val  KEY_LIGHT_TIME_DOWN = DEVICE_REMOTE_LIGHT or 0x0F
        const val  KEY_LIGHT_SW_UP = DEVICE_REMOTE_LIGHT or 0x11
        const val  KEY_LIGHT_SW_DOWN = DEVICE_REMOTE_LIGHT or 0x13
        const val  KEY_LIGHT_KEY1 = DEVICE_REMOTE_LIGHT or 0x15
        const val  KEY_LIGHT_KEY2 = DEVICE_REMOTE_LIGHT or 0x17
        const val  KEY_LIGHT_KEY3 = DEVICE_REMOTE_LIGHT or 0x19
        const val  KEY_LIGHT_KEY4 = DEVICE_REMOTE_LIGHT or 0x1B
        const val  KEY_LIGHT_KEY5 = DEVICE_REMOTE_LIGHT or 0x1D
        const val  KEY_LIGHT_KEY6 = DEVICE_REMOTE_LIGHT or 0x1F
        const val  KEY_LIGHT_KEYA = DEVICE_REMOTE_LIGHT or 0x21
        const val  KEY_LIGHT_KEYB = DEVICE_REMOTE_LIGHT or 0x23
        const val  KEY_LIGHT_KEYC = DEVICE_REMOTE_LIGHT or 0x25
        const val  KEY_LIGHT_KEYD = DEVICE_REMOTE_LIGHT or 0x27
    }

    object REMOTE_KEY_AP {
        const val  KEY_COUNT = 18
        const val  KEY_AP_POWER = DEVICE_REMOTE_AP or 0x01
        const val  KEY_AP_AUTO = DEVICE_REMOTE_AP or 0x03
        const val  KEY_AP_SPEED = DEVICE_REMOTE_AP or 0x05
        const val  KEY_AP_TIMER = DEVICE_REMOTE_AP or 0x07
        const val  KEY_AP_MODE = DEVICE_REMOTE_AP or 0x09
        const val  KEY_AP_LI = DEVICE_REMOTE_AP or 0x0B
        const val  KEY_AP_FEEL = DEVICE_REMOTE_AP or 0x0D
        const val  KEY_AP_MUTE = DEVICE_REMOTE_AP or 0x0F
        const val  KEY_AP_CLOSE_LIGHT = DEVICE_REMOTE_AP or 0x11
        const val  KEY_AP_STRONG = DEVICE_REMOTE_AP or 0x13
        const val  KEY_AP_NATIVE = DEVICE_REMOTE_AP or 0x15
        const val  KEY_AP_CLOSE = DEVICE_REMOTE_AP or 0x17
        const val  KEY_AP_SLEEP = DEVICE_REMOTE_AP or 0x19
        const val  KEY_AP_SMART = DEVICE_REMOTE_AP or 0x1B
        const val  KEY_AP_LIGHT1 = DEVICE_REMOTE_AP or 0x1D
        const val  KEY_AP_LIGHT2 = DEVICE_REMOTE_AP or 0x1F
        const val  KEY_AP_LIGHT3 = DEVICE_REMOTE_AP or 0x21
        const val  KEY_AP_UV = DEVICE_REMOTE_AP or 0x23
    }

    object REMOTE_KEY_HW {
        const val  KEY_COUNT = 10
        const val  KEY_HW_POWER = DEVICE_REMOTE_HW or 0x01
        const val  KEY_HW_SET = DEVICE_REMOTE_HW or 0x03
        const val  KEY_HW_TEMP_ADD = DEVICE_REMOTE_HW or 0x05
        const val  KEY_HW_TEMP_SUB = DEVICE_REMOTE_HW or 0x07
        const val  KEY_HW_MODE = DEVICE_REMOTE_HW or 0x09
        const val  KEY_HW_OK = DEVICE_REMOTE_HW or 0x0B
        const val  KEY_HW_TIMER = DEVICE_REMOTE_HW or 0x0D
        const val  KEY_HW_WAIT = DEVICE_REMOTE_HW or 0x0F
        const val  KEY_HW_TIME = DEVICE_REMOTE_HW or 0x11
        const val  KEY_HW_SAVE_TEMP = DEVICE_REMOTE_HW or 0x13
    }

    object REMOTE_KEY_AUDIO {
        const val  KEY_COUNT = 18
        const val  KEY_AUDIO_LEFT = DEVICE_REMOTE_AUDIO or 0x01
        const val  KEY_AUDIO_UP = DEVICE_REMOTE_AUDIO or 0x03
        const val  KEY_AUDIO_OK = DEVICE_REMOTE_AUDIO or 0x05
        const val  KEY_AUDIO_DOWN = DEVICE_REMOTE_AUDIO or 0x07
        const val  KEY_AUDIO_RIGHT = DEVICE_REMOTE_AUDIO or 0x09
        const val  KEY_AUDIO_POWER = DEVICE_REMOTE_AUDIO or 0x0B
        const val  KEY_AUDIO_VOLUME_IN = DEVICE_REMOTE_AUDIO or 0x0D
        const val  KEY_AUDIO_VOLUME_MUTE = DEVICE_REMOTE_AUDIO or 0x0F
        const val  KEY_AUDIO_VOLUME_OUT = DEVICE_REMOTE_AUDIO or 0x11
        const val  KEY_AUDIO_FAST_BACK = DEVICE_REMOTE_AUDIO or 0x13
        const val  KEY_AUDIO_PLAY = DEVICE_REMOTE_AUDIO or 0x15
        const val  KEY_AUDIO_FAST_FORWARD = DEVICE_REMOTE_AUDIO or 0x17
        const val  KEY_AUDIO_SONG_UP = DEVICE_REMOTE_AUDIO or 0x19
        const val  KEY_AUDIO_STOP = DEVICE_REMOTE_AUDIO or 0x1B
        const val  KEY_AUDIO_SONG_DOWN = DEVICE_REMOTE_AUDIO or 0x1D
        const val  KEY_AUDIO_PAUSE = DEVICE_REMOTE_AUDIO or 0x1F
        const val  KEY_AUDIO_MENU = DEVICE_REMOTE_AUDIO or 0x21
        const val  KEY_AUDIO_BACK = DEVICE_REMOTE_AUDIO or 0x23
    }
}
