package co.candyhouse.sesame.ble.os3.biometric.touchPro

import co.candyhouse.sesame.open.device.sesameBiometric.capability.card.CHCardCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.fingerPrint.CHFingerPrintCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.passcode.CHPassCodeCapable
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBase

interface CHSesameTouchPro : CHSesameBiometricBase, CHPassCodeCapable, CHCardCapable, CHFingerPrintCapable {

}