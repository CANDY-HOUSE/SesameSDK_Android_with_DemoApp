package co.candyhouse.sesame.ble.os3.biometric.touchPro

import co.candyhouse.sesame.open.device.sesameBiometric.capability.card.CHCardCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.card.CHCardCapableImpl
import co.candyhouse.sesame.open.device.sesameBiometric.capability.fingerPrint.CHFingerPrintCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.fingerPrint.CHFingerPrintCapableImpl
import co.candyhouse.sesame.open.device.sesameBiometric.capability.passcode.CHPassCodeCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.passcode.CHPassCodeCapableImpl
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBaseDevice

internal class CHSesameTouchProDevice :
    CHSesameBiometricBaseDevice(),
    CHSesameTouchPro,
    CHCardCapable by CHCardCapableImpl(),
    CHPassCodeCapable by CHPassCodeCapableImpl(),
    CHFingerPrintCapable by CHFingerPrintCapableImpl()
{

}