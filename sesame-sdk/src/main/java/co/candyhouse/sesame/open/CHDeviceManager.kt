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
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHProductModel
import co.candyhouse.sesame.server.CHIotManager
import co.candyhouse.sesame.utils.L
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

    init {
        CHIotManager
    }

    fun vibrateDevice(context: Context) {
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

    fun vibrateDevice(view: View?) {
        view?.context?.let { vibrateDevice(it) }
    }

    fun getCandyDevices(result: CHResult<List<CHDevices>>) {
        CHDB.CHSS2Model.getAllDB { it ->
            it.onSuccess { chDevices ->
                result.invoke(Result.success(CHResultState.CHResultStateBLE(chDevices.filter {
                    CHProductModel.getByModel(
                        it.deviceModel
                    ) != null
                }.map { keyData ->
                    val model = keyData.deviceModel
                    val productModel = CHProductModel.getByModel(model)!!

                    CHBleManager.chDeviceMap.getOrPut(keyData.deviceUUID) { productModel.deviceFactory() }
                        .apply {
                            (this as CHDeviceUtil)
                            this.productModel = productModel;
                            if (this.sesame2KeyData == null) {
                                this.sesame2KeyData = keyData
                            } else {
                                this.sesame2KeyData =
                                    keyData.copy(historyTag = this.sesame2KeyData?.historyTag)
                            }
                        }
                })))
            }
            it.onFailure { error ->
                result.invoke(Result.failure(error))
            }
        }
    }

    private fun isValidUUID(uuid: String): Boolean {
        return try {
            UUID.fromString(uuid)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    fun receiveCHDeviceKeys(vararg devicesKeys: CHDevice, result: CHResult<ArrayList<CHDevices>>) {
        val receiveCHDevices: ArrayList<CHDevices> = arrayListOf()
        val ssmIDs: ArrayList<String> = ArrayList()
        devicesKeys.forEach { it ->
            val candyDevice = it.copy()
            candyDevice.deviceUUID = candyDevice.deviceUUID.lowercase()
            ssmIDs.add(candyDevice.deviceUUID)

            CHDB.CHSS2Model.insert(candyDevice) {
                it.onSuccess {
                    L.d("hcia", "收鑰匙 寫入ＤＢ  candyDevice.historyTag:" + candyDevice.historyTag)
                }
            }
        }

        CHDB.CHSS2Model.getAllDB { it ->
            it.onSuccess {
                it.forEach { keyData ->
                    L.d("hcia", "uuid:" + isValidUUID(keyData.deviceUUID) + "---keyData" + keyData)

                    if (isValidUUID(keyData.deviceUUID)) {
                        CHProductModel.getByModel(keyData.deviceModel)?.let { model ->
                            val cDevice = CHBleManager.chDeviceMap.getOrPut(
                                keyData.deviceUUID.lowercase(Locale.getDefault())
                            ) { model.deviceFactory() }
                            cDevice as CHDeviceUtil
                            cDevice.sesame2KeyData = keyData
                            if (ssmIDs.contains(cDevice.deviceId.toString())) {
                                receiveCHDevices.add(cDevice)
                            }
                        }
                    }
                }
                result.invoke(Result.success(CHResultState.CHResultStateCache(receiveCHDevices)))
            }
            it.onFailure {
                result.invoke(Result.failure(it))
            }
        }
    }

    fun receiveCHDeviceKeys(devicesKeys: List<CHDevice>, result: CHResult<ArrayList<CHDevices>>) {
        val receiveCHDevices: ArrayList<CHDevices> = arrayListOf()
        val ssmIDs: ArrayList<String> = ArrayList()
        L.d("receiveCHDeviceKeys", "插入" + devicesKeys.size)
        CHDB.CHSS2Model.clearAll()
        devicesKeys.forEach { it ->
            L.d("receiveCHDeviceKeys", "插入$it")
            val candyDevice = it.copy()
            candyDevice.deviceUUID = candyDevice.deviceUUID.lowercase(Locale.getDefault())

            ssmIDs.add(candyDevice.deviceUUID)
            CHDB.CHSS2Model.insert(candyDevice) {
                it.onSuccess {
//                    L.d("hcia", "收鑰匙 寫入ＤＢ  candyDevice.historyTag:" + candyDevice.historyTag)
                }
            }
        }

        CHDB.CHSS2Model.getAllDB { it ->
            it.onSuccess {
                it.forEach { keyData ->
                    CHProductModel.getByModel(keyData.deviceModel)?.let { model ->
                        val tmpCDevice = CHBleManager.chDeviceMap.getOrPut(
                            keyData.deviceUUID.lowercase(Locale.getDefault())
                        ) { model.deviceFactory() }

                        if (tmpCDevice is CHDeviceUtil) {
                            tmpCDevice.productModel = model
                            if (tmpCDevice.sesame2KeyData == null) {
                                tmpCDevice.sesame2KeyData = keyData
                            } else {
                                tmpCDevice.sesame2KeyData =
                                    keyData.copy(historyTag = tmpCDevice.sesame2KeyData?.historyTag)
                            }
                            if (ssmIDs.contains(tmpCDevice.deviceId.toString())) {
                                receiveCHDevices.add(tmpCDevice)
                            }
                        }
                    }
                }
                result.invoke(Result.success(CHResultState.CHResultStateCache(receiveCHDevices)))
            }
            it.onFailure {
                result.invoke(Result.failure(it))
            }
        }
    }
}

data class LockDeviceState(var state: Int, var position: Float?, var time: Long = 0L)