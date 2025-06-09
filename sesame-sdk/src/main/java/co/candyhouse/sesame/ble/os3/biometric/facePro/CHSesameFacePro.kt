package co.candyhouse.sesame.ble.os3.biometric.facePro

import co.candyhouse.sesame.open.device.sesameBiometric.capability.card.CHCardCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.face.CHFaceCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.fingerPrint.CHFingerPrintCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.palm.CHPalmCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.passcode.CHPassCodeCapable
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBase

interface CHSesameFacePro : CHSesameBiometricBase, CHPassCodeCapable, CHCardCapable, CHFingerPrintCapable , CHFaceCapable, CHPalmCapable{

}