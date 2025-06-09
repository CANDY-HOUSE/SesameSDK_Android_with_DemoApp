package co.candyhouse.sesame.open.device

import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.server.dto.CHEmpty

interface CHHub3Delegate : CHWifiModule2Delegate {
    fun onIRCodeChanged(device: CHHub3, id: String, name: String) {}
    fun onIRCodeReceive(device: CHHub3, id: String, name: String) {}
    fun onIRCodeReceiveStart(device: CHHub3) {}
    fun onIRCodeReceiveEnd(device: CHHub3) {}
    fun onIRModeReceive(device: CHHub3, mode: Int) {}
    fun onHub3BrightnessReceive(device: CHHub3, brightness: Int) {}
}

interface CHHub3 : CHWifiModule2 {
    var versionTagFromIoT: String?
    var hub3LastFirmwareVer: String?
    var hub3Brightness: Byte
    fun insertSesame(sesame: CHDevices, nickName: String, matterProductModel: MatterProductModel, result: CHResult<CHEmpty>)
    fun irModeSet(mode: Byte, result: CHResult<CHEmpty>)
    fun irModeGet(result: CHResult<Byte>)
    fun irCodeEmit(id: String, result: CHResult<CHEmpty>)
    fun irCodeEmit(topic: String, data: ByteArray, result: CHResult<CHEmpty>)
    fun irCodeDelete(id: String, result: CHResult<CHEmpty>)
    fun irCodeDelete(topic: String, data: ByteArray)
    fun getIRCodes(result: CHResult<CHEmpty>)
    fun irCodeChange(id: String, name: String, result: CHResult<CHEmpty>)
    fun getMatterPairingCode(result: CHResult<ByteArray>)
    fun openMatterPairingWindow(result: CHResult<Byte>)
    fun getHub3StatusFromIot(deviceUUID: String, result: CHResult<Byte>)
    fun updateHub3Firmware(deviceUUID: String, result: CHResult<CHEmpty>)
    fun setHub3Brightness(brightness: Byte, result: CHResult<Byte>)
    fun getIrLearnedData(result: CHResult<ByteArray>)
    fun unsubscribeLearnData()
}
