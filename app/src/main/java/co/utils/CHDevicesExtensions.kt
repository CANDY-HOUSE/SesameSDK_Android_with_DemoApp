package co.utils

import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.server.dto.CHUserKey
import co.candyhouse.sesame.server.dto.StateInfo
import java.util.WeakHashMap

/**
 * 设备访问扩展
 *
 * @author frey on 2025/8/28
 */

private val userKeyHolderMap = WeakHashMap<CHDevices, CHUserKey>()

var CHDevices.userKeyRef: CHUserKey?
    get() = userKeyHolderMap[this]
    set(value) {
        if (value != null) {
            userKeyHolderMap[this] = value
        } else {
            userKeyHolderMap.remove(this)
        }
    }

val CHDevices.stateInfo: StateInfo?
    get() = userKeyRef?.stateInfo