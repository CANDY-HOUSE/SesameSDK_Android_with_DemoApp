package co.candyhouse.sesame.ble.os3.biometric.face

import co.candyhouse.sesame.open.device.sesameBiometric.capability.card.CHCardCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.face.CHFaceCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.fingerPrint.CHFingerPrintCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.palm.CHPalmCapable
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBase

interface CHSesameFace : CHSesameBiometricBase, CHCardCapable, CHFingerPrintCapable, CHFaceCapable, CHPalmCapable {

}