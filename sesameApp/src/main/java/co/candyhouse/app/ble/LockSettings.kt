package co.candyhouse.app.ble

import co.candyhouse.sesame.ble.CHDeviceLoginStatus
import co.candyhouse.sesame.ble.CHDevices
import co.candyhouse.sesame.ble.CHProductModel
import co.candyhouse.sesame.ble.CHSesameLock
import co.candyhouse.sesame.ble.os2.sesame2.CHSesame2
import co.candyhouse.sesame.ble.os3.sesame5.CHSesame5
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.L
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.milliseconds

/** BLE settings only. UI, cloud state and permissions remain in Biz. */
object LockSettings {
    suspend fun <T> awaitResult(call: (CHResult<T>) -> Unit): T = suspendCancellableCoroutine { continuation ->
        call { result ->
            if (continuation.isActive) result.fold({ continuation.resume(it.data) }, { continuation.resumeWithException(it) })
        }
    }

    private suspend fun <T> readResult(call: (CHResult<T>) -> Unit): T =
        withTimeoutOrNull(8000.milliseconds) { awaitResult(call) } ?: run {
            L.d("LockSettings", "BLE settings read timed out")
            error("BLE settings read timed out")
        }

    fun snapshot(device: CHDevices): JSONObject = JSONObject().apply {
        put("productType", device.productModel.productType())
        put("deviceModel", device.productModel.deviceModel())
        put("deviceModelName", device.productModel.deviceModelName())
        if (device.deviceStatus.value != CHDeviceLoginStatus.logined) return@apply
        if (device.bleTxPower.toInt() != CHDevices.UNSET_BLE_TX_POWER_VALUE) put("bleTxPower", device.bleTxPower.toInt())
        val setting = when (device) {
            is CHSesame5 -> device.mechSetting?.let { it.lockPosition to it.unlockPosition }
            is CHSesame2 -> device.mechSetting?.let { it.lockPosition to it.unlockPosition }
            else -> null
        }
        setting?.let { put("lockPosition", it.first.toInt()); put("unlockPosition", it.second.toInt()) }
        if (device is CHSesame5) {
            device.mechSetting?.let { put("autoLockSeconds", it.autoLockSecond.toInt()) }
            device.opsSetting?.let { put("sensorLockSeconds", it.opsLockSecond.toInt()) }
            put("magnet", true)
        }
        put("position", device.mechStatus?.position?.toInt() ?: JSONObject.NULL)
        if (device.hasLockUnlockSwitchPointSetting) put("switchPoint", device.lockUnlockSwitchPoint.toInt())
        if (device.sensorDetectIntervalMs != CHDevices.UNSET_SENSOR_DETECT_INTERVAL_MS) put("sensorInterval", device.sensorDetectIntervalMs.toInt())
    }

    suspend fun handle(ble: BleController, json: JSONObject): JSONObject {
        val device = requireNotNull(ble.device(json.getString("deviceUUID")))
        require(device.deviceStatus.value == CHDeviceLoginStatus.logined)
        val operation = json.getString("operation")
        require(device is CHSesame2 || device is CHSesame5 || (device is CHSesameLock && operation == "txPower"))
        when (operation) {
            "txPower" -> {
                require(device.bleTxPower.toInt() != CHDevices.UNSET_BLE_TX_POWER_VALUE)
                val value = json.getInt("value")
                require(value in -4..20)
                readResult<CHEmpty> { device.setBleTxPower(value.toByte(), it) }
            }

            "read" -> {
                val data = snapshot(device)
                if (device is CHSesame2) data.put("autoLockSeconds", readResult(device::getAutolockSetting))
                return data
            }

            "version" -> return JSONObject().put("version", readResult(device::getVersionTag))
            "lockPosition", "unlockPosition" -> {
                val position = requireNotNull(device.mechStatus).position
                when (device) {
                    is CHSesame5 -> {
                        val settings = requireNotNull(device.mechSetting)
                        awaitResult {
                            device.configureLockPosition(
                                if (operation == "lockPosition") position else settings.lockPosition,
                                if (operation == "unlockPosition") position else settings.unlockPosition,
                                it
                            )
                        }
                    }

                    is CHSesame2 -> {
                        val settings = requireNotNull(device.mechSetting)
                        awaitResult {
                            device.configureLockPosition(
                                if (operation == "lockPosition") position else settings.lockPosition,
                                if (operation == "unlockPosition") position else settings.unlockPosition,
                                it
                            )
                        }
                    }
                }
            }

            "autoLock" -> {
                val seconds = json.getInt("value")
                require(seconds in listOf(0, 3, 5, 7, 10, 15, 30, 60, 120, 180, 240, 300, 600, 900, 1800, 3600))
                when (device) {
                    is CHSesame5 -> awaitResult { device.autolock(seconds, it) }
                    is CHSesame2 -> if (seconds == 0) awaitResult { device.disableAutolock(ble.historyTag(), it) } else awaitResult {
                        device.enableAutolock(
                            seconds,
                            ble.historyTag(),
                            it
                        )
                    }
                }
                ble.snapshot()
                return snapshot(device).put("autoLockSeconds", seconds)
            }

            "sensorLock" -> {
                require(device is CHSesame5)
                val seconds = json.getInt("value")
                require(seconds in listOf(65535, 0, 1, 2, 3, 4, 5, 7, 10, 15, 30, 60, 120, 180, 240, 300, 600, 900, 1800, 3600))
                awaitResult { device.opSensorControl(seconds, it) }
                ble.snapshot()
                return snapshot(device).put("sensorLockSeconds", seconds)
            }

            "magnet" -> {
                require(device is CHSesame5); awaitResult(device::magnet)
            }

            "switchPoint" -> {
                require(device.hasLockUnlockSwitchPointSetting)
                awaitResult<CHEmpty> { device.setLockUnlockSwitchPoint(requireNotNull(device.mechStatus).position, it) }
            }

            "slidingMode" -> {
                require(device is CHSesame5)
                val target = when (device.productModel) {
                    CHProductModel.SS6Pro -> CHProductModel.SS6ProSlidingDoor
                    CHProductModel.SS6ProSlidingDoor -> CHProductModel.SS6Pro
                    else -> error("Unsupported product mode")
                }
                awaitResult<CHEmpty> { device.sendAdvProductTypeCommand(byteArrayOf(target.productType().toByte()), it) }
                device.productModel = target
                ble.saveModel(device)
            }

            "sensorInterval" -> {
                require(device.sensorDetectIntervalMs != CHDevices.UNSET_SENSOR_DETECT_INTERVAL_MS)
                val interval = json.getInt("value")
                require(interval == 0 || (interval in 50..1000 && interval % 50 == 0))
                awaitResult<CHEmpty> { device.setSensorDetectInterval(interval.toShort(), it) }
            }

            else -> error("Unsupported lock setting")
        }
        ble.snapshot()
        return snapshot(device)
    }
}
