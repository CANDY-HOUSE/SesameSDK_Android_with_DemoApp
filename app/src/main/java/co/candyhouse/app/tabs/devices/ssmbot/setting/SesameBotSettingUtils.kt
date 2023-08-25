package co.candyhouse.app.tabs.devices.ssmbot.setting

import co.candyhouse.app.R
import co.candyhouse.sesame.open.device.CHSesameBotMechSettings

fun botMode(setting: CHSesameBotMechSettings): SesameBotMode {
    if (setting.clickLockSec.toInt() == 20) {
        return SesameBotMode.Cell
    } else if (setting.userPrefDir.toInt() == 0) {
        return SesameBotMode.Normal
    } else {
        return SesameBotMode.LongPress
    }
}

enum class SesameBotMode {
    Normal {
        override fun i18nResources() = R.string.BotModeNormal
        override fun changeNextMode(setting: CHSesameBotMechSettings): CHSesameBotMechSettings {
            setting.userPrefDir = 1
            setting.clickLockSec = 10
            setting.clickHoldSec = 20
            setting.clickUnlockSec = 15
            return setting
        }

    },
    LongPress {
        override fun i18nResources() = R.string.PressModeCell
        override fun changeNextMode(setting: CHSesameBotMechSettings): CHSesameBotMechSettings {
            setting.userPrefDir = 0
            setting.clickLockSec = 20
            setting.clickHoldSec = 0
            setting.clickUnlockSec = 0
            return setting
        }
    },
    Cell {
        override fun i18nResources() = R.string.BotModeCell
        override fun changeNextMode(setting: CHSesameBotMechSettings): CHSesameBotMechSettings {
            setting.userPrefDir = 0
            setting.clickLockSec = 10
            setting.clickHoldSec = 10
            setting.clickUnlockSec = 8
            return setting
        }
    };
    abstract fun i18nResources(): Int
    abstract fun changeNextMode(setting: CHSesameBotMechSettings): CHSesameBotMechSettings
}
