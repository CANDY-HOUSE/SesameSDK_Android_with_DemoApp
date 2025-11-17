package co.candyhouse.sesame.open

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import co.candyhouse.sesame.ble.CHDeviceUtil
import co.candyhouse.sesame.db.CHDB
import co.candyhouse.sesame.db.model.CHDevice
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHProductModel
import co.candyhouse.sesame.server.CHIotManager
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.utils.L
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("StaticFieldLeak")
object CHDeviceManager {

    lateinit var app: Context
    var isRefresh = AtomicBoolean(false)
    var isScroll = AtomicBoolean(false)
    val listDevices = mutableListOf<CHDevices>()
    var lockStates = mutableMapOf<String, LockDeviceState>()

    const val NOTIFICATION_FLAG = "notification"
    const val NOTIFICATION_ACTION = "notification_action"

    init {
        CHIotManager
    }

    fun vibrateDevice(view: View?) {
        view?.context?.let { vibrateDevice(it) }
    }

    private fun vibrateDevice(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                vibrator.vibrate(100)
            }
        }
    }

    private fun isValidUUID(uuid: String): Boolean {
        return try {
            UUID.fromString(uuid)
            true
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private fun CHDevice.toDeviceOrNull(): CHDevices? {
        if (!isValidUUID(deviceUUID)) return null

        val productModel = CHProductModel.getByModel(deviceModel) ?: return null
        val normalizedUUID = deviceUUID.lowercase(Locale.getDefault())

        return CHBleManager.chDeviceMap.getOrPut(normalizedUUID) {
            productModel.deviceFactory()
        }.apply {
            (this as CHDeviceUtil)
            this.productModel = productModel
            this.sesame2KeyData = if (this.sesame2KeyData == null) {
                this@toDeviceOrNull
            } else {
                this@toDeviceOrNull.copy(historyTag = this.sesame2KeyData?.historyTag)
            }
        }
    }

    fun getCandyDevices(result: CHResult<List<CHDevices>>) {
        CHDB.CHSS2Model.getAllDB { dbResult ->
            dbResult.onSuccess { deviceDataList ->
                val devices = deviceDataList.mapNotNull { it.toDeviceOrNull() }
                result.invoke(Result.success(CHResultState.CHResultStateBLE(devices)))
            }
            dbResult.onFailure { error ->
                result.invoke(Result.failure(error))
            }
        }
    }

    fun getCandyDeviceByUUID(deviceID: String, result: CHResult<CHDevices>) {
        CHDB.CHSS2Model.getDevice(deviceID.lowercase()) { dbResult ->
            dbResult.onSuccess { device ->
                val chDevices = device.toDeviceOrNull()
                if (chDevices != null) {
                    result.invoke(Result.success(CHResultState.CHResultStateBLE(chDevices)))
                } else {
                    result.invoke(Result.failure(Exception("Device data is invalid or corrupted for ID: $deviceID")))
                }
            }
            dbResult.onFailure { error ->
                result.invoke(Result.failure(error))
            }
        }
    }

    fun dropAllKeys(devices: List<CHDevices>, result: CHResult<CHEmpty>) {
        if (devices.isEmpty()) {
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            return
        }

        val deviceIds = devices.map { it.deviceId.toString() }

        CHDB.CHSS2Model.deleteByDeviceIds(deviceIds) { deleteResult ->
            when {
                deleteResult.isSuccess -> {
                    devices.forEach { device ->
                        device.delegate = null
                        device.deviceStatus = CHDeviceStatus.NoBleSignal
                        device.disconnect {}
                    }
                    result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
                }

                deleteResult.isFailure -> {
                    result.invoke(Result.failure(deleteResult.exceptionOrNull()!!))
                }
            }
        }
    }

    // 单个或多个设备插入（增量添加）
    fun receiveCHDeviceKeys(vararg devicesKeys: CHDevice, result: CHResult<ArrayList<CHDevices>>) {
        if (devicesKeys.isEmpty()) {
            result.invoke(Result.success(CHResultState.CHResultStateCache(arrayListOf())))
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val normalizedDevices = devicesKeys.map { device ->
                device.copy().apply {
                    deviceUUID = deviceUUID.lowercase(Locale.getDefault())
                }
            }

            normalizedDevices.forEach { device ->
                CHDB.CHSS2Model.insert(device) {
                    it.onSuccess {
                        L.d("hcia", "收鑰匙 寫入ＤＢ historyTag: ${device.historyTag}")
                    }
                }
            }

            delay(100)

            val addedDevices = ArrayList(normalizedDevices.mapNotNull { it.toDeviceOrNull() })
            result.invoke(Result.success(CHResultState.CHResultStateCache(addedDevices)))
        }
    }

    // 批量设备替换（全量覆盖）
    fun receiveCHDeviceKeys(devicesKeys: List<CHDevice>, result: CHResult<ArrayList<CHDevices>>) {
        L.d("hcia", "插入" + devicesKeys.size)

        CoroutineScope(Dispatchers.IO).launch {
            val normalizedDevices = devicesKeys.map { device ->
                device.copy().apply {
                    deviceUUID = deviceUUID.lowercase(Locale.getDefault())
                }
            }

            val deviceUUIDs = normalizedDevices.map { it.deviceUUID }.toSet()

            CHDB.CHSS2Model.replaceAll(normalizedDevices) { replaceResult ->
                replaceResult.onSuccess {
                    CHDB.CHSS2Model.getAllDB { dbResult ->
                        dbResult.onSuccess { allDevices ->
                            val devices = ArrayList(
                                allDevices
                                    .mapNotNull { it.toDeviceOrNull() }
                                    .filter { it.deviceId.toString() in deviceUUIDs }
                            )
                            result.invoke(Result.success(CHResultState.CHResultStateCache(devices)))
                        }
                        dbResult.onFailure { error ->
                            result.invoke(Result.failure(error))
                        }
                    }
                }
                replaceResult.onFailure { error ->
                    result.invoke(Result.failure(error))
                }
            }
        }
    }
}

data class LockDeviceState(var state: Int, var position: Float?, var time: Long = 0L)