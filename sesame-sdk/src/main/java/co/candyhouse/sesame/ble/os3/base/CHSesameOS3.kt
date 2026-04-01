package co.candyhouse.sesame.ble.os3.base

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.os.Build
import co.candyhouse.sesame.ble.CHBaseDevice
import co.candyhouse.sesame.ble.CHDeviceUtil
import co.candyhouse.sesame.ble.CHadv
import co.candyhouse.sesame.ble.DeviceSegmentType
import co.candyhouse.sesame.ble.SSM2OpCode
import co.candyhouse.sesame.ble.SSM3PublishPayload
import co.candyhouse.sesame.ble.SSM3ResponsePayload
import co.candyhouse.sesame.ble.Sesame2Chracs
import co.candyhouse.sesame.ble.SesameBleTransmit
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.ble.SesameNotifypayload
import co.candyhouse.sesame.ble.SesameResultCode
import co.candyhouse.sesame.ble.isBleAvailable
import co.candyhouse.sesame.ble.os2.CHError
import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.open.CHBleManager.appContext
import co.candyhouse.sesame.open.CHBleManager.bluetoothAdapter
import co.candyhouse.sesame.open.devices.base.CHDeviceStatus
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.open.devices.base.CHProductModel
import co.candyhouse.sesame.open.devices.base.NSError
import co.candyhouse.sesame.server.CHAPIClientBiz
import co.candyhouse.sesame.server.dto.CHRemoveSignKeyRequest
import co.candyhouse.sesame.utils.BleBaseType
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.CHResultState
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.isInternetAvailable
import co.candyhouse.sesame.utils.toHexString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore

interface CHSesameOS3Publish {
    fun onGattSesamePublish(receivePayload: SSM3PublishPayload)
}
internal typealias SesameOS3ResponseCallback = (result: SSM3ResponsePayload) -> Unit
internal typealias thisItemCodeLastSendTime = Long

@SuppressLint("MissingPermission")
internal open class CHSesameOS3 : CHBaseDevice(), CHSesameOS3Publish {

    var cipher: SesameOS3BleCipher? = null//[加解密層]
    var cmdCallBack: MutableMap<UByte, SesameOS3ResponseCallback> = mutableMapOf()
    private var cmdCallBackMap: ConcurrentHashMap<UByte, thisItemCodeLastSendTime> =
        ConcurrentHashMap()

    private var semaphore: Semaphore = Semaphore(1)

    open fun connect(result: CHResult<CHEmpty>) {

        val advertisement = (this as? CHDeviceUtil)?.advertisement
        val deviceAddress = advertisement?.device?.address

        if (deviceAddress == null) {
            L.d("[say]", "[connect] Advertisement or device address is null")
            result.invoke(Result.failure(CHError.Noble.value))
            return
        }

        // Check if the device address is already in the connectR list
        if (CHBleManager.connectR.contains(deviceAddress)) {
            L.d("[say]", "[connect] 已连接")
            return
        } else {
            CHBleManager.connectR.add(deviceAddress)
        }

        deviceStatus = CHDeviceStatus.BleConnecting
        result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))


        if (bluetoothAdapter == null) {
            L.d("[say]", "[connect] BluetoothAdapter is null")
            result.invoke(Result.failure(CHError.Noble.value))
            return
        }
        if (appContext == null) {
            result.invoke(Result.failure(CHError.Noble.value))
            return
        }
        val gattCallback = mBluetoothGattCallback
        if (gattCallback == null) {
            result.invoke(Result.failure(CHError.Noble.value))
            return
        }
        val remoteDevice = bluetoothAdapter.getRemoteDevice(deviceAddress)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O || Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            remoteDevice.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            remoteDevice.connectGatt(appContext, false, gattCallback)
        }
    }

    private val mBluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {

        //该方法在更广泛的Android版本上有效，具有兼容性
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            handleCharacteristicChanged(characteristic.value)
        }

        //此方法是新Google建议的新API调用，但Android 12及以下并不走该方法，以及可能存在Android 13以上部分厂商不兼容，暂时屏蔽。
        /*override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            L.d("sf", "Android 13 及以上调用...")
            super.onCharacteristicChanged(gatt, characteristic, value)
            handleCharacteristicChanged(value)
        }*/

        private fun handleCharacteristicChanged(value: ByteArray) {
            L.d("hcia", "[ssm][say]:" + value.toHexString() + ", bytes: " + value.size)
            val ssmSay = gattRxBuffer.feed(value)
            if (ssmSay?.first == DeviceSegmentType.plain) {
                parseNotifyPayload(ssmSay.second)
            } else if (ssmSay?.first == DeviceSegmentType.cipher) {
                parseNotifyPayload(cipher!!.decrypt(ssmSay.second))
            }
        }

        private fun parseNotifyPayload(palntext: ByteArray) {
            val ssm2notify = SesameNotifypayload(palntext)
            L.d("[say]", "[ss5] ssm2notify.notifyOpCode:" + ssm2notify.notifyOpCode)
            if (ssm2notify.notifyOpCode == SSM2OpCode.response) {
                onGattSesameResponse(SSM3ResponsePayload(ssm2notify.payload))
            } else if (ssm2notify.notifyOpCode == SSM2OpCode.publish) {
                onGattSesamePublish(SSM3PublishPayload(ssm2notify.payload))
            }
        }

        private fun onGattSesameResponse(ssm2ResponsePayload: SSM3ResponsePayload) {
            L.d(
                "onGattSesameResponse",
                "☆1、 ssm2ResponsePayload.cmdItCode: ${ssm2ResponsePayload.cmdItCode}"
            )
            cmdCallBack.get(ssm2ResponsePayload.cmdItCode)?.invoke(ssm2ResponsePayload)
            L.d(
                "onGattSesameResponse",
                "☆2、 ssm2ResponsePayload.cmdItCode: ${ssm2ResponsePayload.cmdItCode}"
            )
            cmdCallBack.remove(ssm2ResponsePayload.cmdItCode)
            L.d(
                "[say]",
                "☆3、 🀄Command: <==:  " + (ssm2ResponsePayload.cmdItCode) + " " + (ssm2ResponsePayload.cmdResultCode)
            )
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            transmit()
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            L.d("[say]", "[onMtuChanged] mtu: $mtu, status: $status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if ((productModel == CHProductModel.SSMTouchPro) || (productModel == CHProductModel.SSMTouch2Pro) || (productModel == CHProductModel.SSMTouch) || (productModel == CHProductModel.SSMTouch2) || (productModel == CHProductModel.SSMFacePro) || (productModel == CHProductModel.SSMFace2Pro)
                    || (productModel == CHProductModel.SSMFaceProAI) || (productModel == CHProductModel.SSMFace2ProAI) || (productModel == CHProductModel.SSMFaceAI) || (productModel == CHProductModel.SSMFace2AI) || (productModel == CHProductModel.SSMFace) || (productModel == CHProductModel.SSMFace2)) {
                    deviceStatus = CHDeviceStatus.DiscoverServices
                    mBluetoothGatt = gatt
                    gatt?.discoverServices()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            L.d("[say]", "[onServicesDiscovered]")
            super.onServicesDiscovered(gatt, status)
            for (service in gatt?.services!!) {
                L.d("[say]", "[onServicesDiscovered] service 01 : " + service.uuid)
                if (service.uuid == Sesame2Chracs.uuidService01) {
                    for (charc in service.characteristics) {
                        L.d("[say]", "[onServicesDiscovered] charc: " + charc.uuid)
                        if (charc.uuid == Sesame2Chracs.uuidChr02) {
                            mCharacteristic = charc
                        }
                        if (charc.uuid == Sesame2Chracs.uuidChr03) {
                            L.d(
                                "[say]",
                                "[NOTIFICATION]【start】[enable: " + BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE.toHexString() + "][disable: " + BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE.toHexString() + "]"
                            )
                            gatt.setCharacteristicNotification(charc, true)
                            val descriptor =
                                charc.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))

                            //writeDescriptor在Android 13以后过时，改用新的方式直接设置特征值
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                gatt.writeDescriptor(
                                    descriptor,
                                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                )
                            } else {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                                val check = gatt.writeDescriptor(descriptor)
                                L.d("[say]", "[NOTIFICATION]【end】 [enable] $check")
                            }
                        }
                    }
                }
            }
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if ((productModel == CHProductModel.SSMTouchPro) || (productModel == CHProductModel.SSMTouch2Pro) || (productModel == CHProductModel.SSMTouch) || (productModel == CHProductModel.SSMTouch2) || (productModel == CHProductModel.SSMFacePro) || (productModel == CHProductModel.SSMFace2Pro)
                    || (productModel == CHProductModel.SSMFaceAI) || (productModel == CHProductModel.SSMFace2AI) || (productModel == CHProductModel.SSMFaceProAI) || (productModel == CHProductModel.SSMFace2ProAI) || (productModel == CHProductModel.SSMFace) || (productModel == CHProductModel.SSMFace2)) { // 连接成功后立即请求MTU变更
                    // 如果是刷卡机, 需要设置 MTU。 iOS会自动协商， 但是 Android 需要手动设置。
                    val result = gatt.requestMtu(251) // 统一成 苹果手机 的 251
                    L.d("onConnectionStateChange", "MTU request result: $result")
                } else {
                    L.d(
                        "[say]",
                        "[ss5] " + ":連接狀態＋＋" + BleBaseType.GattConnectStateDec(newState) + " 狀態:" + BleBaseType.GattConnectStatusDec(
                            status
                        )
                    )
                    deviceStatus = CHDeviceStatus.DiscoverServices
                    mBluetoothGatt = gatt
                    gatt.discoverServices()
                }
                if (status == BluetoothProfile.STATE_DISCONNECTED) {
                    cmdCallBack.clear()
                }
            } else {
                L.d(
                    "[say]",
                    "[ss5][$deviceId][${BleBaseType.GattConnectStateDec(newState)}][${
                        BleBaseType.GattConnectStatusDec(status)
                    }]"
                )
                gatt.close()
                (this@CHSesameOS3 as CHDeviceUtil).advertisement = null
                mBluetoothGatt = null
                cmdCallBack.clear()
                CHBleManager.connectR.remove(gatt.device.address)
            }
        }
    }

    fun transmit() {
        val chunkData = gattTxBuffer?.getChunk()
        if (chunkData == null) {
            semaphore.release()
            return
        }

        L.d("sf", "chunkData[0]=${chunkData[0]}")

        var check: Boolean
        //writeCharacteristic在Android 13以后过时，改用新的方式直接设置特征值
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // 新的写入特征值方法
            if (mCharacteristic != null) {
                val result = mBluetoothGatt?.writeCharacteristic(
                    mCharacteristic!!,
                    chunkData,
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                )
                L.d("sf", "result===$result")
                check = result == BluetoothStatusCodes.SUCCESS
            } else {
                check = false
            }
        } else {
            mCharacteristic?.value = chunkData
            check = mBluetoothGatt?.writeCharacteristic(mCharacteristic) == true
        }

        L.d("[say]", "[app][say][$mBluetoothGatt][" + mCharacteristic?.uuid + "]")
        L.d(
            "sf",
            "[app][say]:" + mCharacteristic?.value?.toHexString() + "; bytes: " + mCharacteristic?.value?.size + "; check:" + check
        )
        /*测试方法： 快速从设置页切换到设备列表页，然后再切换回来。
        测试手机： Google Pixel 6a (2C051JEGR01824) Android 13, API 33
        测试记录： 最多看到有两万多次。
2023-11-10 21:02:05.416 24521-24562 hcia                    co.candyhouse.sesame2                D  [ss5][app][say]:0302782f3407; bytes: 6; check:false  (CHSesameOS3.kt:156) 副線成
2023-11-10 21:02:05.419 24521-24562 [say]                   co.candyhouse.sesame2                D  [ss5][app][say][retry][1]  (CHSesameOS3.kt:166) 副線成

2023-11-10 21:02:59.906 24521-24563 hcia                    co.candyhouse.sesame2                D  [ss5][app][say]:030225e60add; bytes: 6; check:false  (CHSesameOS3.kt:156) 副線成
2023-11-10 21:02:59.912 24521-24563 [say]                   co.candyhouse.sesame2                D  [ss5][app][say][retry][58]  (CHSesameOS3.kt:166) 副線成

2023-11-10 21:03:37.547 24521-24563 hcia                    co.candyhouse.sesame2                D  [ss5][app][say]:0302b70b17b9; bytes: 6; check:false  (CHSesameOS3.kt:156) 副線成
2023-11-10 21:03:37.572 24521-24563 [say]                   co.candyhouse.sesame2                D  [ss5][app][say][retry][2564]  (CHSesameOS3.kt:166) 副線成

2023-11-10 21:06:22.606 24521-24561 hcia                    co.candyhouse.sesame2                D  [ss5][app][say]:03023aa34cba; bytes: 6; check:false  (CHSesameOS3.kt:156) 副線成
2023-11-10 21:06:22.613 24521-24561 [say]                   co.candyhouse.sesame2                D  [ss5][app][say][retry][65]  (CHSesameOS3.kt:166) 副線成

2023-11-10 21:06:49.365 24521-24561 hcia                    co.candyhouse.sesame2                D  [ss5][app][say]:0302ba7a1f9e; bytes: 6; check:false  (CHSesameOS3.kt:156) 副線成
2023-11-10 21:06:49.370 24521-24561 [say]                   co.candyhouse.sesame2                D  [ss5][app][say][retry][1150]  (CHSesameOS3.kt:166) 副線成

2023-11-10 21:07:22.411 24521-24729 hcia                    co.candyhouse.sesame2                D  [ss5][app][say]:0302efde98bd; bytes: 6; check:false  (CHSesameOS3.kt:156) 副線成
2023-11-10 21:07:22.447 24521-24729 [say]                   co.candyhouse.sesame2                D  [ss5][app][say][retry][4191]  (CHSesameOS3.kt:166) 副線成*/

        if (!check) {   // 重试 30000 次， 不行再断线
            var retry = 0
            do {
                retry++
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // 新的写入特征值方法
                    val result = mBluetoothGatt?.writeCharacteristic(
                        mCharacteristic!!,
                        chunkData,
                        BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    )
                    check = result == BluetoothStatusCodes.SUCCESS
                } else {
                    check = mBluetoothGatt?.writeCharacteristic(mCharacteristic) == true
                }

                if (retry > 30000) { // 重试次数
                    break
                }
            } while (!check)
            L.d("sf", "[app][say][retry][$retry]")
            if (!check) {
                semaphore.release()
                disconnect { }
            }
        }
    }

    fun sendCommand(
        payload: SesameOS3Payload,
        isEncryt: DeviceSegmentType = DeviceSegmentType.cipher,
        onResponse: SesameOS3ResponseCallback
    ) {
        val tmp = cmdCallBack[payload.itemCode]
//        L.d("hcia", "[put][qeueu][${payload.itemCode}][again]")

        cmdCallBack[payload.itemCode] = onResponse
        if (tmp != null) {
            L.d("hcia", "[qeueu][${payload.itemCode}][again]")
//            if(payload.itemCode == SesameItemCode.history.value){
//                L.d("Harry", "历史记录的蓝牙 Response 丢失, 断线重连 ~~~")
//                semaphore.release()
//                disconnect { }
//            }
            if (cmdCallBackMap[payload.itemCode] != null && ((System.currentTimeMillis() - cmdCallBackMap[payload.itemCode]!!) > 2000)) {
                L.d("Harry", "上一次的 这个 itemCode 的蓝牙 Response 丢失 ")
                cmdCallBack.remove(payload.itemCode)
                cmdCallBackMap.remove(payload.itemCode)
            }
            return
        }
        cmdCallBackMap[payload.itemCode] = System.currentTimeMillis()
        CoroutineScope(IO).launch {
            try {
//            L.d("hcia", "[acquire] semaphore?.availablePermits():" + semaphore?.availablePermits())
                semaphore.acquire()

//            L.d("hcia", "[device] 🀄Command: ==>:" + (payload.itemCode) + " " + isEncryt)
                val say2ssm = if (isEncryt == DeviceSegmentType.cipher) {
                    cipher?.encrypt(payload.toDataWithHeader()) ?: run {

                        semaphore.release()
                        return@launch
                    }
                } else {
                    payload.toDataWithHeader()
                }
                gattTxBuffer = SesameBleTransmit(isEncryt, say2ssm!!)
                transmit()
            } catch (e: Exception) {
                e.printStackTrace()
                semaphore.release()  // 捕获异常 释放锁
            }
        }
    }

    /** 預設指令: 版本。重置。更新韌體(DFU) */
    open fun getVersionTag(result: CHResult<String>) {
        if (!(this as CHDevices).isBleAvailable(result)) return
        sendCommand(
            SesameOS3Payload(SesameItemCode.versionTag.value, byteArrayOf()),
            DeviceSegmentType.cipher
        ) { res ->
            val deviceUUID = deviceId.toString().uppercase()
            if (res.cmdResultCode == SesameResultCode.success.value) {
                val versionTag = String(res.payload)
                L.d("getVersionTag", "$deviceUUID == $versionTag")
                result.invoke(Result.success(CHResultState.CHResultStateBLE(versionTag)))
                CHAPIClientBiz.updateDeviceFirmwareVersion(deviceUUID, versionTag) {
                    it.onFailure { error ->
                        L.e("getVersionTag", error.message.toString())
                    }
                }
            } else {
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
            }
        }
    }

    open fun reset(result: CHResult<CHEmpty>) {
        sendCommand(
            SesameOS3Payload(SesameItemCode.Reset.value, byteArrayOf()),
            DeviceSegmentType.cipher
        ) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                dropKey(result)
            } else {
                result.invoke(
                    Result.failure(
                        NSError(
                            res.cmdResultCode.toString(),
                            "CBCentralManager",
                            res.cmdResultCode.toInt()
                        )
                    )
                )
            }
        }
    }

    open fun updateFirmware(onResponse: CHResult<BluetoothDevice>) {
        val device = (this as CHDeviceUtil).advertisement?.device
        if (device != null) {
            onResponse.invoke(Result.success(CHResultState.CHResultStateBLE(device)))
        } else {
            // Handle the case where device is null, perhaps by invoking a failure result
            onResponse.invoke(Result.failure(RuntimeException("Bluetooth device is not available.")))
        }
    }

    fun parceADV(value: CHadv?) {
        value?.let {
            rssi = it.rssi
            deviceId = it.deviceID
            isRegistered = it.isRegistered
            productModel = it.productModel!!
//            L.d("[say]", "[deviceStatus: $deviceStatus][deviceID: $deviceId][isRegistered: $isRegistered][productModel: $productModel]")
            deviceStatus = when {
                deviceStatus == CHDeviceStatus.NoBleSignal -> CHDeviceStatus.ReceivedAdV
                deviceStatus == CHDeviceStatus.WaitingForAuth && isInternetAvailable() -> CHDeviceStatus.ReceivedAdV
                else -> deviceStatus
            }
        } ?: run {
            deviceStatus = CHDeviceStatus.NoBleSignal
        }
    }

    override fun onGattSesamePublish(receivePayload: SSM3PublishPayload) {
        if (receivePayload.cmdItCode == SesameItemCode.initial.value) {
            mSesameToken = receivePayload.payload
            if (isRegistered) {
                L.d("[say]", "isNeedAuthFromServer: " + isNeedAuthFromServer.toString())
                if (isNeedAuthFromServer == true) {
                    CHAPIClientBiz.signGuestKey(
                        CHRemoveSignKeyRequest(
                            deviceId.toString().uppercase(),
                            mSesameToken.toHexString(),
                            sesame2KeyData!!.secretKey
                        )
                    ) {
                        it.onSuccess {
                            (this as CHDeviceUtil).login(it.data)
                        }
                    }
                } else {
                    (this as CHDeviceUtil).login()
                }
            } else {
                deviceStatus = CHDeviceStatus.ReadyToRegister
            }
        }
    }
}

data class SesameOS3Payload(val itemCode: UByte, val data: ByteArray) {
    fun toDataWithHeader(): ByteArray {
        return byteArrayOf(itemCode.toByte()) + data
    }
}
