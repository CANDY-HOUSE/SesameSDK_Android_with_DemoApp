package co.candyhouse.sesame.ble.os3.biometric.facePro

import co.candyhouse.sesame.open.device.sesameBiometric.capability.card.CHCardCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.card.CHCardCapableImpl
import co.candyhouse.sesame.open.device.sesameBiometric.capability.face.CHFaceCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.face.CHFaceCapableImpl
import co.candyhouse.sesame.open.device.sesameBiometric.capability.fingerPrint.CHFingerPrintCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.fingerPrint.CHFingerPrintCapableImpl
import co.candyhouse.sesame.open.device.sesameBiometric.capability.palm.CHPalmCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.palm.CHPalmCapableImpl
import co.candyhouse.sesame.open.device.sesameBiometric.capability.passcode.CHPassCodeCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.passcode.CHPassCodeCapableImpl
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBaseDevice

internal class CHSesameFaceProDevice:
    CHSesameBiometricBaseDevice(),
    CHSesameFacePro,
    CHCardCapable by CHCardCapableImpl(),
    CHPassCodeCapable by CHPassCodeCapableImpl(),
    CHFingerPrintCapable by CHFingerPrintCapableImpl(),
    CHPalmCapable by CHPalmCapableImpl(),
    CHFaceCapable by CHFaceCapableImpl()
{
}