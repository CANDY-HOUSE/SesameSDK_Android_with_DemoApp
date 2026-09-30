package co.candyhouse.sesame.ble.os3.biometric.capability.connect

import co.candyhouse.sesame.ble.CHDevices
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult

interface CHDeviceConnectCapable {
    fun insertSesame(sesame: CHDevices, result: CHResult<CHEmpty>)
    fun removeSesame(tag: String, result: CHResult<CHEmpty>)
    fun setRadarSensitivity(payload: ByteArray, result: CHResult<CHEmpty>) {}
}
