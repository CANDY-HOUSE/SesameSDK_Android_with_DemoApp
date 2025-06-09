package co.candyhouse.sesame.ble.os3.biometric.face

import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHDataSynchronizeCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHDataSynchronizeCapableImpl
import co.candyhouse.sesame.open.device.sesameBiometric.capability.card.CHCardCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.card.CHCardCapableImpl
import co.candyhouse.sesame.open.device.sesameBiometric.capability.face.CHFaceCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.face.CHFaceCapableImpl
import co.candyhouse.sesame.open.device.sesameBiometric.capability.fingerPrint.CHFingerPrintCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.fingerPrint.CHFingerPrintCapableImpl
import co.candyhouse.sesame.open.device.sesameBiometric.capability.palm.CHPalmCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.palm.CHPalmCapableImpl
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBaseDevice


internal class CHSesameFaceDevice:
    CHSesameBiometricBaseDevice(),
    CHSesameFace,
    CHCardCapable by CHCardCapableImpl(),
    CHFingerPrintCapable by CHFingerPrintCapableImpl(),
    CHPalmCapable by CHPalmCapableImpl(),
    CHFaceCapable by CHFaceCapableImpl()
{
}