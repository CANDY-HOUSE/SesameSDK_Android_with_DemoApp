package co.candyhouse.sesame.open.device.sesameBiometric.capability.connect

import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.server.dto.CHEmpty

interface CHDeviceConnectCapable {
    fun insertSesame(sesame: CHDevices, result: CHResult<CHEmpty>)
    fun removeSesame(tag: String, result: CHResult<CHEmpty>)
    fun setRadarSensitivity(payload: ByteArray, result: CHResult<CHEmpty>){}
}