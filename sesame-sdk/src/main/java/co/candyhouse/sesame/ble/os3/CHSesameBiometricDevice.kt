package co.candyhouse.sesame.ble.os3

import co.candyhouse.sesame.open.device.CHDevices
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
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBase
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBaseDevice

/**
 * 统一的生物识别设备接口
 *
 * @author frey on 2025/8/18
 */
interface CHSesameBiometricDevice : CHSesameBiometricBase {
    val deviceType: BiometricDeviceType

    // 获取设备支持的能力
    fun supportedCapabilities(): Set<BiometricCapability>
}

// 设备支持的能力
enum class BiometricCapability {
    CARD, FINGERPRINT, PASSCODE, FACE, PALM
}

enum class BiometricDeviceType {
    OPEN_SENSOR,
    REMOTE,
    SESAME_TOUCH,
    SESAME_TOUCH_PRO,
    SESAME_FACE,
    SESAME_FACE_PRO,
    SESAME_FACE_AI,
    SESAME_FACE_PRO_AI,
    OPEN_SENSOR_2
}

internal class CHSesameBiometricDeviceImpl(
    override val deviceType: BiometricDeviceType,
    private val capabilities: Set<BiometricCapability>
) : CHSesameBiometricBaseDevice(),
    CHSesameBiometricDevice,
    CHCardCapable by CHCardCapableImpl(),
    CHPassCodeCapable by CHPassCodeCapableImpl(),
    CHFingerPrintCapable by CHFingerPrintCapableImpl(),
    CHPalmCapable by CHPalmCapableImpl(),
    CHFaceCapable by CHFaceCapableImpl() {

    override fun supportedCapabilities() = capabilities
}

object DeviceProfiles {
    val SESAME_TOUCH = setOf(BiometricCapability.CARD, BiometricCapability.FINGERPRINT)
    val SESAME_TOUCH_PRO = setOf(BiometricCapability.FINGERPRINT, BiometricCapability.PASSCODE, BiometricCapability.CARD)
    val SESAME_FACE = setOf(BiometricCapability.CARD, BiometricCapability.FINGERPRINT, BiometricCapability.PALM, BiometricCapability.FACE)
    val SESAME_FACE_AI = setOf(BiometricCapability.PALM, BiometricCapability.FACE)
    val SESAME_FACE_PRO = setOf(
        BiometricCapability.PASSCODE,
        BiometricCapability.CARD,
        BiometricCapability.FINGERPRINT,
        BiometricCapability.PALM,
        BiometricCapability.FACE
    )
    val SESAME_FACE_PRO_AI = setOf(BiometricCapability.PASSCODE, BiometricCapability.PALM, BiometricCapability.FACE)
}

fun CHDevices.hasBiometricCapability(capabilities: BiometricCapability): Boolean {
    return this is CHSesameBiometricDevice && supportedCapabilities().contains(capabilities)
}

fun CHDevices.isOpenSensor(): Boolean {
    return this is CHSesameBiometricDevice && deviceType == BiometricDeviceType.OPEN_SENSOR
}

fun CHDevices.isRemote(): Boolean {
    return this is CHSesameBiometricDevice && deviceType == BiometricDeviceType.REMOTE
}

fun CHDevices.isSesameTouch(): Boolean {
    return this is CHSesameBiometricDevice && deviceType == BiometricDeviceType.SESAME_TOUCH
}

fun CHDevices.isSesameTouchPro(): Boolean {
    return this is CHSesameBiometricDevice && deviceType == BiometricDeviceType.SESAME_TOUCH_PRO
}

fun CHDevices.isSesameFace(): Boolean {
    return this is CHSesameBiometricDevice && deviceType == BiometricDeviceType.SESAME_FACE
}

fun CHDevices.isSesameFacePro(): Boolean {
    return this is CHSesameBiometricDevice && deviceType == BiometricDeviceType.SESAME_FACE_PRO
}

fun CHDevices.isSesameFaceAi(): Boolean {
    return this is CHSesameBiometricDevice && deviceType == BiometricDeviceType.SESAME_FACE_AI
}

fun CHDevices.isSesameFaceProAi(): Boolean {
    return this is CHSesameBiometricDevice && deviceType == BiometricDeviceType.SESAME_FACE_PRO_AI
}