package co.candyhouse.sesame.open.device.sesameBiometric.capability.fingerPrint

import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBase
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.server.dto.CHFingerPrintNameRequest

interface CHFingerPrintCapable {
    fun fingerPrints(result: CHResult<CHEmpty>)
    fun fingerPrintDelete(ID: String, result: CHResult<CHEmpty>)
    fun fingerPrintsChange(ID: String, name: String, result: CHResult<CHEmpty>)
    fun fingerPrintModeGet(result: CHResult<Byte>)
    fun fingerPrintModeSet(mode: Byte, result: CHResult<CHEmpty>)
    fun fingerPrintNameGet(fingerPrintID: String, fingerPrintNameUUID: String, subUUID: String, deviceUUID: String, result: CHResult<String>)
    fun fingerPrintNameSet(fingerPrintNameRequest: CHFingerPrintNameRequest, result: CHResult<String>)

    fun registerEventDelegate(device: CHSesameBiometricBase, delegate: CHFingerPrintDelegate)
    fun unregisterEventDelegate(device: CHSesameBiometricBase, delegate: CHFingerPrintDelegate)
}