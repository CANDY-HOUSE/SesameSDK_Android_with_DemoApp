package co.candyhouse.sesame.open.devices

import androidx.lifecycle.LiveData
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.open.devices.base.CHSesameConnector
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.baseCapbale.CHCapabilityHost
import co.candyhouse.sesame.open.devices.sesameBiometric.parseData.CHRemoteNanoTriggerSettings
import co.candyhouse.sesame.utils.Event

/**
 * 统一的生物识别设备接口
 *
 * @author frey on 2025/8/18
 */
interface CHSesameBiometricDevice : CHSesameConnector, CHCapabilityHost {
    val deviceType: BiometricDeviceType
    var triggerDelaySetting: CHRemoteNanoTriggerSettings?

    fun supportedCapabilities(): Set<BiometricCapability>
    fun login(token: String?)
    fun goIOT()
    fun getSSM2KeysLiveData(): LiveData<Map<String, ByteArray>>?
    fun getSSM2SlotFullLiveData(): LiveData<Event<Boolean>>?
    fun getSSM2SupportLiveDataLiveData(): LiveData<Event<Boolean>>?
}

// 设备支持的能力
enum class BiometricCapability {
    CARD, FINGERPRINT, PASSCODE, FACE, PALM
}

enum class BiometricDeviceType {
    OPEN_SENSOR,
    OPEN_SENSOR_2,
    REMOTE,
    SESAME_TOUCH,
    SESAME_TOUCH_PRO,
    SESAME_FACE,
    SESAME_FACE_PRO,
    SESAME_FACE_AI,
    SESAME_FACE_PRO_AI,
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