package co.candyhouse.sesame.ble.os3.biometric.touch

import co.candyhouse.sesame.open.device.sesameBiometric.capability.card.CHCardCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.card.CHCardCapableImpl
import co.candyhouse.sesame.open.device.sesameBiometric.capability.fingerPrint.CHFingerPrintCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.fingerPrint.CHFingerPrintCapableImpl
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBaseDevice

internal class CHSesameTouchDevice :
    CHSesameBiometricBaseDevice(),
    CHSesameTouch,
    CHCardCapable by CHCardCapableImpl(),
    CHFingerPrintCapable by CHFingerPrintCapableImpl() {
}