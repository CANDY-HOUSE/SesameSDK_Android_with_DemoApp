package co.candyhouse.sesame.open.device

import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.server.dto.CHEmpty
interface CHWifiModule2Delegate : CHDeviceStatusDelegate {
    fun onAPSettingChanged(device: CHWifiModule2, settings: CHWifiModule2MechSettings) {}
//    fun onNetWorkStatusChanged(device: CHWifiModule2, settings: CHWifiModule2NetWorkStatus) {}
    fun onSSM2KeysChanged(device: CHWifiModule2, ssm2keys: Map<String, String>) {}
    fun onOTAProgress(device: CHWifiModule2, percent: Byte) {}
    fun onScanWifiSID(device: CHWifiModule2, ssid: String, rssi: Short) {}
}
class CHWifiModule2MechSettings(var wifiSSID: String?, var wifiPassWord: String?)
class CHWifiModule2NetWorkStatus(var isAPWork: Boolean?, var isNetWork: Boolean?, var isIOTWork: Boolean?, var isAPConnecting: Boolean, var isConnectingNet: Boolean, var isConnectingIOT: Boolean, var isAPCheck: Boolean?):CHSesameProtocolMechStatus{
    override val data: ByteArray
        get() = TODO("Not yet implemented")

    override fun getBatteryVoltage(): Float {
        TODO("Not yet implemented")
    }

}

interface CHWifiModule2 : CHDevices {
    var ssm2KeysMap: MutableMap<String, String>
//    var networkStatus: CHWifiModule2NetWorkStatus?
    var mechSetting: CHWifiModule2MechSettings?
    fun scanWifiSSID(result: CHResult<CHEmpty>)
    fun setWifiSSID(ssid: String, result: CHResult<CHEmpty>)
    fun setWifiPassword(password: String, result: CHResult<CHEmpty>)
    fun connectWifi(result: CHResult<CHEmpty>)
    fun insertSesames(sesame: CHDevices, result: CHResult<CHEmpty>)
    fun removeSesame(sesameKeyTag: String, result: CHResult<CHEmpty>)
}
