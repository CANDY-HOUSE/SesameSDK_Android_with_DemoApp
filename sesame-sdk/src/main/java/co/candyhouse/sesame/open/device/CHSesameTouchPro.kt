package co.candyhouse.sesame.open.device

import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.bytesToShort

class CHSesameTouchProMechStatus(override val data: ByteArray) : CHSesameProtocolMechStatus {
    private val battery = bytesToShort(data[0], data[1])
    override fun getBatteryVoltage(): Float {//確認設備電壓，getBatteryPrecentage 有必要再複寫。
        return battery * 2f / 1000f
    }
}

interface CHSesameTouchProDelegate : CHDeviceStatusDelegate {
    fun onSSM2KeysChanged(device: CHSesameConnector, ssm2keys: Map<String, ByteArray>) {}

    fun onKeyBoardReceive(device: CHSesameConnector, ID: String, name: String, type: Byte) {}
    fun onKeyBoardChanged(device: CHSesameConnector, ID: String, name: String, type: Byte) {}
    fun onKeyBoardReceiveEnd(device: CHSesameConnector) {}
    fun onKeyBoardReceiveStart(device: CHSesameConnector) {}

    fun onCardReceive(device: CHSesameConnector, ID: String, name: String, type: Byte) {}
    fun onCardChanged(device: CHSesameConnector, ID: String, name: String, type: Byte) {}
    fun onCardReceiveEnd(device: CHSesameConnector) {}
    fun onCardReceiveStart(device: CHSesameConnector) {}

    fun onFingerPrintReceive(device: CHSesameConnector, ID: String, name: String, type: Byte) {}
    fun onFingerPrintChanged(device: CHSesameConnector, ID: String, name: String, type: Byte) {}
    fun onFingerPrintReceiveEnd(device: CHSesameConnector) {}
    fun onFingerPrintReceiveStart(device: CHSesameConnector) {}
}

interface CHSesameTouchPro : CHSesameConnector {

    fun cards(result: CHResult<CHEmpty>)
    fun cardDelete(ID: String, result: CHResult<CHEmpty>)
    fun cardChange(ID: String, name: String, result: CHResult<CHEmpty>)
    fun cardModeGet(result: CHResult<Byte>)
    fun cardModeSet(mode: Byte, result: CHResult<CHEmpty>)

    fun fingerPrints(result: CHResult<CHEmpty>)
    fun fingerPrintDelete(ID: String, result: CHResult<CHEmpty>)
    fun fingerPrintsChange(ID: String, name: String, result: CHResult<CHEmpty>)
    fun fingerPrintModeGet(result: CHResult<Byte>)
    fun fingerPrintModeSet(mode: Byte, result: CHResult<CHEmpty>)

    fun keyBoardPassCode(result: CHResult<CHEmpty>)
    fun keyBoardPassCodeChange(ID: String, name: String, result: CHResult<CHEmpty>)
    fun keyBoardPassCodeDelete(ID: String, result: CHResult<CHEmpty>)
    fun keyBoardPassCodeModeGet(result: CHResult<Byte>)
    fun keyBoardPassCodeModeSet(mode: Byte, result: CHResult<CHEmpty>)

}
