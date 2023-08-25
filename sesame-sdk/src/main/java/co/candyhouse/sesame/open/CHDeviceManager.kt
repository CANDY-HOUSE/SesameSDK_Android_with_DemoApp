package co.candyhouse.sesame.open

import co.candyhouse.sesame.db.CHDB
import co.candyhouse.sesame.db.model.CHDevice
import co.candyhouse.sesame.ble.CHDeviceUtil
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHProductModel
import co.candyhouse.sesame.utils.L
import kotlin.collections.ArrayList


object CHDeviceManager {
    init {

//        CHAccountManager
    }

    fun getCandyDevices(result: CHResult<List<CHDevices>>) {
        CHDB.CHSS2Model.getAllDB {
            it.onSuccess {
//                L.d("hcia", "🎃 it.size:" + it.size)

                result.invoke(Result.success(CHResultState.CHResultStateBLE(it.filter { CHProductModel.getByModel(it.deviceModel) != null }.map { keyData ->

//                    L.d("hcia", "keyData:" + keyData)
                    val model = keyData.deviceModel
                    val productmodel = CHProductModel.getByModel(model)!!
                    
                    CHBleManager.chDeviceMap.getOrPut(keyData.deviceUUID) { productmodel.deviceFactory() }.apply {
                        (this as CHDeviceUtil)
//                        L.d("hcia", "🎃 this:" + this)
//                        L.d("hcia", "🎃1  keyData:" + (this as CHDevicesUtil).sesame2KeyData)
//                        L.d("hcia", "🎃2  keyData:" + keyData)
//                        L.d("hcia", "🎃3+ (this.sesame2KeyData == keyData):" + (this.sesame2KeyData == keyData))
//                        L.d("hcia", "🎃3- (this.sesame2KeyData e= keyData):" + (this.sesame2KeyData?.equals(keyData) ))
                        this.sesame2KeyData?.apply {

                        }

                        this.productModel = productmodel;
                        if (this.sesame2KeyData == null) {
                            this.sesame2KeyData = keyData
                        } else {
                            this.sesame2KeyData = keyData.copy(historyTag = this.sesame2KeyData?.historyTag)
                        }

                    }
                })))
            }
            it.onFailure { error ->
                result.invoke(Result.failure(error))
            }

        }
    }


    fun receiveCHDeviceKeys(vararg devicesKeys: CHDevice, result: CHResult<ArrayList<CHDevices>>) {
//        L.d("hcia", "CHDevice 收鑰匙多多")
        val receiveCHDevices: ArrayList<CHDevices> = arrayListOf<CHDevices>()
        val ssmIDs: ArrayList<String> = ArrayList<String>()
        devicesKeys.forEach {
            val candyDevice = it.copy()
            candyDevice.deviceUUID = candyDevice.deviceUUID.lowercase()
            ssmIDs.add(candyDevice.deviceUUID)
//            L.d("hcia", "收鑰匙 candyDevice.historyTag:" + candyDevice.historyTag?.toHexString())

            CHDB.CHSS2Model.insert(candyDevice) {
                it.onSuccess {
                    L.d("hcia", "收鑰匙 寫入ＤＢ  candyDevice.historyTag:" + candyDevice.historyTag)
                }
            }
        }

        CHDB.CHSS2Model.getAllDB {
            it.onSuccess {
                it.forEach { keyData ->
                    CHProductModel.getByModel(keyData.deviceModel)?.let { model ->
                        val tmpCHdevice = CHBleManager.chDeviceMap.getOrPut(keyData.deviceUUID.toLowerCase(), { model.deviceFactory() })
                        tmpCHdevice as CHDeviceUtil
                        tmpCHdevice.sesame2KeyData = keyData
                        if (ssmIDs.contains(tmpCHdevice.deviceId.toString())) {
                            receiveCHDevices.add(tmpCHdevice)
                        }
                    }

                }
//                L.d("🔑", "寫入db")
                result.invoke(Result.success(CHResultState.CHResultStateCache(receiveCHDevices)))

            }
            it.onFailure {
                result.invoke(Result.failure(it))
            }
        }
    }

    fun receiveCHDeviceKeys(devicesKeys: List<CHDevice>, result: CHResult<ArrayList<CHDevices>>) {
//        L.d("hcia", "CHDevice 收鑰匙多多")
        val receiveCHDevices: ArrayList<CHDevices> = arrayListOf<CHDevices>()
        val ssmIDs: ArrayList<String> = ArrayList<String>()
        devicesKeys.forEach {
            val candyDevice = it.copy()
            candyDevice.deviceUUID = candyDevice.deviceUUID.toLowerCase()
            ssmIDs.add(candyDevice.deviceUUID)
//            L.d("hcia", "收鑰匙 candyDevice.historyTag:" + candyDevice.historyTag?.toHexString())
            CHDB.CHSS2Model.insert(candyDevice) {
                it.onSuccess {
//                    L.d("hcia", "收鑰匙 寫入ＤＢ  candyDevice.historyTag:" + candyDevice.historyTag)
                }
            }
        }

        CHDB.CHSS2Model.getAllDB {
            it.onSuccess {
                it.forEach { keyData ->
                    CHProductModel.getByModel(keyData.deviceModel)?.let { model ->
                        val tmpCHdevice = CHBleManager.chDeviceMap.getOrPut(keyData.deviceUUID.toLowerCase(), { model.deviceFactory() })
                        tmpCHdevice as CHDeviceUtil
                        tmpCHdevice.sesame2KeyData = keyData
                        if (ssmIDs.contains(tmpCHdevice.deviceId.toString())) {
                            receiveCHDevices.add(tmpCHdevice)
                        }
                    }

                }
//                L.d("🔑", "寫入db")
                result.invoke(Result.success(CHResultState.CHResultStateCache(receiveCHDevices)))
            }
            it.onFailure {
                result.invoke(Result.failure(it))
            }
        }
    }


}



