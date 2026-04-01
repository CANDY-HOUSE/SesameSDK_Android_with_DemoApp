package co.candyhouse.sesame.open.devices.sesameBiometric.capability.connect

import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.utils.CHEmpty

interface CHDeviceConnectCapable {
    fun insertSesame(sesame: CHDevices, result: CHResult<CHEmpty>)
    fun removeSesame(tag: String, result: CHResult<CHEmpty>)
    fun setRadarSensitivity(payload: ByteArray, result: CHResult<CHEmpty>){}
}