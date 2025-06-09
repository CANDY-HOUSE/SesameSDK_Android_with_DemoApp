package co.candyhouse.sesame.ble.os3.biometric.touch

import co.candyhouse.sesame.open.device.sesameBiometric.capability.card.CHCardCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.fingerPrint.CHFingerPrintCapable
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBase

interface CHSesameTouch : CHSesameBiometricBase, CHCardCapable, CHFingerPrintCapable {

}