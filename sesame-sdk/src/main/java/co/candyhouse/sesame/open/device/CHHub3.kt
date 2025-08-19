package co.candyhouse.sesame.open.device

import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.server.dto.CHEmpty

interface CHHub3Delegate : CHWifiModule2Delegate {
    fun onHub3BrightnessReceive(device: CHHub3, brightness: Int) {}
}

interface CHHub3 : CHWifiModule2 {
    var versionTagFromIoT: String?
    var hub3LastFirmwareVer: String?
    var hub3Brightness: Byte
    fun insertSesame(sesame: CHDevices, nickName: String, matterProductModel: MatterProductModel, result: CHResult<CHEmpty>)
    fun getMatterPairingCode(result: CHResult<ByteArray>)
    fun openMatterPairingWindow(result: CHResult<Byte>)
    fun getHub3StatusFromIot(deviceUUID: String, result: CHResult<Byte>)
    fun updateHub3Firmware(deviceUUID: String, result: CHResult<CHEmpty>)
    fun setHub3Brightness(brightness: Byte, result: CHResult<Byte>)
    fun subscribeTopic(topic: String, result: CHResult<ByteArray>)
    fun unsubscribeTopic(topic: String)
}
