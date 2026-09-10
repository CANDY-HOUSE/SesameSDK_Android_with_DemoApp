package co.candyhouse.app.shortcut

import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.CHScanStatus
import co.candyhouse.sesame.open.devices.CHSesame2
import co.candyhouse.sesame.open.devices.CHSesame5
import co.candyhouse.sesame.open.devices.CHSesameBike
import co.candyhouse.sesame.open.devices.CHSesameBike2
import co.candyhouse.sesame.open.devices.CHSesameBot
import co.candyhouse.sesame.open.devices.CHSesameBot2
import co.candyhouse.sesame.open.devices.base.CHDeviceLoginStatus
import co.candyhouse.sesame.open.devices.base.CHDeviceStatus
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.open.devices.base.CHSesameLock
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult
import co.utils.UserUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

sealed interface SesameShortcutResult {
    data object Success : SesameShortcutResult
    data object DeviceNotFound : SesameShortcutResult
    data object DeviceLookupFailed : SesameShortcutResult
    data object UnsupportedAction : SesameShortcutResult
    data object PreparationTimedOut : SesameShortcutResult
    data object CommandOutcomeUnknown : SesameShortcutResult
    data object Failed : SesameShortcutResult
}

private sealed interface DeviceLookupResult {
    data class Found(val device: CHDevices) : DeviceLookupResult
    data object NotFound : DeviceLookupResult
    data object Failed : DeviceLookupResult
}

internal enum class ShortcutCommandPath {
    BLE,
    IOT,
}

internal enum class ShortcutCommandFailurePolicy {
    TERMINAL,
    WAIT_FOR_SUCCESS,
}

/** Some SDK families emit a BLE failure before completing their IoT fallback callback. */
internal fun shortcutCommandFailurePolicy(
    kind: SesameShortcutDeviceKind,
): ShortcutCommandFailurePolicy = when (kind) {
    SesameShortcutDeviceKind.SESAME5,
    SesameShortcutDeviceKind.BIKE2,
    SesameShortcutDeviceKind.BOT,
    SesameShortcutDeviceKind.BOT2,
    -> ShortcutCommandFailurePolicy.WAIT_FOR_SUCCESS

    SesameShortcutDeviceKind.SESAME2,
    SesameShortcutDeviceKind.BIKE,
    -> ShortcutCommandFailurePolicy.TERMINAL
}

object SesameShortcutExecutor {

    private const val DEVICE_LOOKUP_TIMEOUT_MS = 3_000L
    private const val READINESS_TIMEOUT_MS = 9_000L
    private const val READINESS_POLL_INTERVAL_MS = 500L
    private const val COMMAND_TIMEOUT_MS = 8_000L

    suspend fun execute(deviceId: String, action: SesameShortcutAction): SesameShortcutResult {
        var scanStartedByShortcut = false

        return try {
            val lookup = withTimeoutOrNull(DEVICE_LOOKUP_TIMEOUT_MS) {
                findDevice(deviceId)
            } ?: return SesameShortcutResult.PreparationTimedOut

            val device = when (lookup) {
                is DeviceLookupResult.Found -> lookup.device
                DeviceLookupResult.NotFound -> return SesameShortcutResult.DeviceNotFound
                DeviceLookupResult.Failed -> return SesameShortcutResult.DeviceLookupFailed
            }

            val kind = device.toShortcutDeviceKindOrNull()
            if (kind == null || !kind.supports(action)) {
                return SesameShortcutResult.UnsupportedAction
            }

            try {
                scanStartedByShortcut = startBleScanIfNeeded(device)

                awaitCommandPath(device)
                    ?: return SesameShortcutResult.PreparationTimedOut

                // Never use a physical command as a connectivity probe.
                currentCommandPath(device)
                    ?: return SesameShortcutResult.Failed

                val historyTag = UserUtils.getEnvironmentIdWithByte()
                val commandResult = withTimeoutOrNull(COMMAND_TIMEOUT_MS) {
                    executeOnce(device, kind, action, historyTag)
                } ?: return SesameShortcutResult.CommandOutcomeUnknown

                if (commandResult) SesameShortcutResult.Success else SesameShortcutResult.Failed
            } finally {
                if (scanStartedByShortcut) {
                    CHBleManager.disableScan { }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            SesameShortcutResult.Failed
        }
    }

    private suspend fun findDevice(deviceId: String): DeviceLookupResult =
        suspendCancellableCoroutine { continuation ->
            CHDeviceManager.getCandyDevices { result ->
                if (!continuation.isActive) return@getCandyDevices

                result.onSuccess { state ->
                    if (!continuation.isActive) return@onSuccess
                    val match = state.data.firstOrNull {
                        it.deviceId?.toString()?.equals(deviceId, ignoreCase = true) == true
                    }
                    continuation.resume(
                        match?.let { DeviceLookupResult.Found(it) } ?: DeviceLookupResult.NotFound,
                    )
                }

                result.onFailure {
                    if (!continuation.isActive) return@onFailure
                    continuation.resume(DeviceLookupResult.Failed)
                }
            }
        }

    private fun startBleScanIfNeeded(device: CHDevices): Boolean {
        val lockDevice = device as? CHSesameLock ?: return false
        if (currentCommandPath(lockDevice) != null || CHBleManager.mScanning == CHScanStatus.Enable) {
            return false
        }

        CHBleManager.enableScan(true) { }
        return CHBleManager.mScanning == CHScanStatus.Enable
    }

    private suspend fun awaitCommandPath(device: CHDevices): ShortcutCommandPath? =
        withTimeoutOrNull(READINESS_TIMEOUT_MS) {
            while (true) {
                val path = currentCommandPath(device)
                if (path != null) {
                    return@withTimeoutOrNull path
                }
                connectIfAdvertisementReceived(device)
                delay(READINESS_POLL_INTERVAL_MS)
            }
            @Suppress("UNREACHABLE_CODE")
            null
        }

    private fun connectIfAdvertisementReceived(device: CHDevices) {
        val lockDevice = device as? CHSesameLock ?: return
        if (lockDevice.deviceStatus == CHDeviceStatus.ReceivedAdV) {
            lockDevice.connect { }
        }
    }

    private fun currentCommandPath(device: CHDevices): ShortcutCommandPath? {
        val lockDevice = device as? CHSesameLock ?: return null
        return when {
            lockDevice.deviceStatus.value == CHDeviceLoginStatus.logined -> ShortcutCommandPath.BLE
            lockDevice.deviceShadowStatus != null -> ShortcutCommandPath.IOT
            else -> null
        }
    }

    private suspend fun executeOnce(
        device: CHDevices,
        kind: SesameShortcutDeviceKind,
        action: SesameShortcutAction,
        historyTag: ByteArray?,
    ): Boolean {
        val failurePolicy = shortcutCommandFailurePolicy(kind)

        return when (kind) {
            SesameShortcutDeviceKind.SESAME5 -> {
                val d = device as CHSesame5
                when (action) {
                    SesameShortcutAction.LOCK -> awaitCommand(failurePolicy) { d.lock(historytag = historyTag, result = it) }
                    SesameShortcutAction.UNLOCK -> awaitCommand(failurePolicy) { d.unlock(historytag = historyTag, result = it) }
                    SesameShortcutAction.TOGGLE -> awaitCommand(failurePolicy) { d.toggle(historytag = historyTag, result = it) }
                    SesameShortcutAction.CLICK -> false
                }
            }

            SesameShortcutDeviceKind.SESAME2 -> {
                val d = device as CHSesame2
                when (action) {
                    SesameShortcutAction.LOCK -> awaitCommand(failurePolicy) { d.lock(result = it) }
                    SesameShortcutAction.UNLOCK -> awaitCommand(failurePolicy) { d.unlock(result = it) }
                    SesameShortcutAction.TOGGLE -> awaitCommand(failurePolicy) { d.toggle(result = it) }
                    SesameShortcutAction.CLICK -> false
                }
            }

            SesameShortcutDeviceKind.BIKE -> {
                val d = device as CHSesameBike
                if (action == SesameShortcutAction.UNLOCK) {
                    awaitCommand(failurePolicy) { d.unlock(result = it) }
                } else {
                    false
                }
            }

            SesameShortcutDeviceKind.BIKE2 -> {
                val d = device as CHSesameBike2
                if (action == SesameShortcutAction.UNLOCK) {
                    awaitCommand(failurePolicy) { d.unlock(historytag = historyTag, result = it) }
                } else {
                    false
                }
            }

            SesameShortcutDeviceKind.BOT -> {
                val d = device as CHSesameBot
                if (action == SesameShortcutAction.CLICK) {
                    awaitCommand(failurePolicy) { d.click(result = it) }
                } else {
                    false
                }
            }

            SesameShortcutDeviceKind.BOT2 -> {
                val d = device as CHSesameBot2
                if (action == SesameShortcutAction.CLICK) {
                    awaitCommand(failurePolicy) { d.click(historytag = historyTag, result = it) }
                } else {
                    false
                }
            }
        }
    }

    private suspend fun awaitCommand(
        failurePolicy: ShortcutCommandFailurePolicy,
        command: (CHResult<CHEmpty>) -> Unit,
    ): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            command { result ->
                if (!continuation.isActive) return@command
                when {
                    result.isSuccess -> continuation.resume(true)
                    failurePolicy == ShortcutCommandFailurePolicy.TERMINAL -> continuation.resume(false)
                    else -> Unit
                }
            }
        } catch (_: Exception) {
            if (continuation.isActive) {
                continuation.resume(false)
            }
        }
    }
}
