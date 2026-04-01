package co.candyhouse.sesame.open.devices.sesameBiometric.capability.fingerPrint

import co.candyhouse.sesame.open.devices.sesameBiometric.capability.baseCapbale.CHCapabilityHost
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.baseCapbale.CHDataSynchronizeCapable
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult

interface CHFingerPrintCapable {
    fun fingerPrints(result: CHResult<CHEmpty>)
    fun fingerPrintDelete(fingerPrintID: String, deviceId: String, result: CHResult<CHEmpty>)
    fun fingerPrintsChange(ID: String, hexName: String, result: CHResult<CHEmpty>)
    fun fingerPrintModeGet(result: CHResult<Byte>)
    fun fingerPrintModeSet(mode: Byte, result: CHResult<CHEmpty>)
    fun getFingerPrintDataSyncCapable(): CHDataSynchronizeCapable

    fun registerEventDelegate(device: CHCapabilityHost, delegate: CHFingerPrintDelegate)
    fun unregisterEventDelegate(device: CHCapabilityHost, delegate: CHFingerPrintDelegate)
}