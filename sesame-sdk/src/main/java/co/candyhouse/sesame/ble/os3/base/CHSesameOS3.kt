package co.candyhouse.sesame.ble.os3.base

import android.annotation.SuppressLint
import android.bluetooth.*
import android.os.Build
import co.candyhouse.sesame.ble.*
import co.candyhouse.sesame.ble.CHBaseDevice
import co.candyhouse.sesame.ble.CHDeviceUtil
import co.candyhouse.sesame.ble.os2.CHError
import co.candyhouse.sesame.open.*
import co.candyhouse.sesame.open.CHBleManager.appContext
import co.candyhouse.sesame.open.CHBleManager.bluetoothAdapter
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.NSError
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.server.dto.CHRemoveSignKeyRequest
import co.candyhouse.sesame.utils.*
import co.candyhouse.sesame.utils.L
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.Semaphore

interface CHSesameOS3Publish {
    fun onGattSesamePublish(receivePayload: SSM3PublishPayload)
}
internal typealias SesameOS3ResponseCallback = (result: SSM3ResponsePayload) -> Unit

@SuppressLint("MissingPermission") internal open class CHSesameOS3 : CHBaseDevice(), CHSesameOS3Publish {

    var cipher: SesameOS3BleCipher? = null//[加解密層]
    var cmdCallBack: MutableMap<UByte, SesameOS3ResponseCallback> = mutableMapOf()
    var semaphore: Semaphore = Semaphore(1)

    open fun connect(result: CHResult<CHEmpty>) {
        if (CHBleManager.mScanning == CHScanStatus.BleClose) {
            result.invoke(Result.failure(CHError.BlePoweroff.value))
            return
        }
        if (deviceStatus != CHDeviceStatus.ReceivedAdV) {
            result.invoke(Result.failure(CHError.Noble.value))
            return
        }
        if (CHBleManager.connectR.indexOf((this as CHDeviceUtil).advertisement?.device?.address) == -1) { //            L.b("hcia", customDeviceName + ":連接紀錄:" + advertisement!!.device.address)
            CHBleManager.connectR.add(advertisement!!.device.address)
        } else { //            L.b("hcia", customDeviceName + ":我已經連接過了：" + (advertisement as CHadv).device.address)
            return
        }
        deviceStatus = CHDeviceStatus.BleConnecting
        result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bluetoothAdapter.getRemoteDevice(advertisement!!.device.address).connectGatt(appContext, false, mBluetoothGattCallback, BluetoothDevice.TRANSPORT_LE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bluetoothAdapter.getRemoteDevice(advertisement!!.device.address).connectGatt(appContext, false, mBluetoothGattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            bluetoothAdapter.getRemoteDevice(advertisement!!.device.address).connectGatt(appContext, false, mBluetoothGattCallback)
        }
    }

    private val mBluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
//            L.d("hcia", "[ssm][say]:" + characteristic.value.toHexString() +" "+ characteristic.value.size)
            L.l("onCharacteristicChanged",byToString(characteristic.value))
            val ssmSay = gattRxBuffer.feed(characteristic.value)
            val c=ssmSay?.first?:"null"
            L.l("onCharacteristicChanged",c.toString(),byToString(characteristic.value))
            if (ssmSay?.first == DeviceSegmentType.plain) {
                parseNotifyPayload(ssmSay.second)
            } else if (ssmSay?.first == DeviceSegmentType.cipher) {
                parseNotifyPayload(cipher!!.decrypt(ssmSay.second))
            }

        }

        private fun parseNotifyPayload(palntext: ByteArray) {
            L.l("parse data  rx",byToString(palntext))
            val ssm2notify = SesameNotifypayload(palntext)
//            L.d("hcia", "[ss5] ssm2notify.notifyOpCode:" + ssm2notify.notifyOpCode)
            if (ssm2notify.notifyOpCode == SSM2OpCode.response) {
                onGattSesameResponse(SSM3ResponsePayload(ssm2notify.payload))
            } else if (ssm2notify.notifyOpCode == SSM2OpCode.publish) {
                onGattSesamePublish(SSM3PublishPayload(ssm2notify.payload))
            }
        }

        private fun onGattSesameResponse(ssm2ResponsePayload: SSM3ResponsePayload) {
            cmdCallBack.get(ssm2ResponsePayload.cmdItCode)?.invoke(ssm2ResponsePayload)
            cmdCallBack.remove(ssm2ResponsePayload.cmdItCode)
//            L.d("hcia", "[ss5] 🀄Command: <==:" + (ssm2ResponsePayload.cmdItCode) + " " + (ssm2ResponsePayload.cmdResultCode))
        }


        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            L.l("onCharacteristicWrite")
            transmit()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)

            for (service in gatt?.services!!) {
                if (service.uuid == Sesame2Chracs.uuidService01) {

                    for (charc in service.characteristics) {
                        if (charc.uuid == Sesame2Chracs.uuidChr02) {
                            mCharacteristic = charc
                        }

                        if (charc.uuid == Sesame2Chracs.uuidChr03) {
                            gatt.setCharacteristicNotification(charc, true)
                            val descriptor = charc.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                            gatt.writeDescriptor(descriptor)
                        }
                    }
                }
            }

        }

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
//                L.d("hcia", "[ss5] " + ":連接狀態＋＋" + BleBaseType.GattConnectStateDec(newState) + " 狀態:" + BleBaseType.GattConnectStatusDec(status))
                deviceStatus = CHDeviceStatus.DiscoverServices
                mBluetoothGatt = gatt
                gatt.discoverServices()
            } else {

//                L.d("hcia", "[ss5][$deviceId][${BleBaseType.GattConnectStateDec(newState)}][${BleBaseType.GattConnectStatusDec(status)}]")
                gatt.close()
                (this@CHSesameOS3 as CHDeviceUtil).advertisement = null
                mBluetoothGatt = null
                cmdCallBack.clear()
                CHBleManager.connectR.remove(gatt.device.address)
            }
        }
    }
    fun   byToString (bs:ByteArray):String{
        val buffer:StringBuffer= StringBuffer()
        buffer.append("[len:${bs.size}],")
        bs.forEach {
            buffer.append(it)
            buffer.append("-")

        }
        buffer.append(",[${bs.toHexString()}]")
        return buffer.toString()
    }
    fun transmit() {
        mCharacteristic?.value = gattTxBuffer?.getChunk()
        if (mCharacteristic?.value == null) {
            semaphore.release()
            return
        }
        val check = mBluetoothGatt?.writeCharacteristic(mCharacteristic)
//        L.d("hcia", "[ss5][app][say]:" + mCharacteristic?.value?.toHexString() + " check:" + check)
        if (check == false) {
            semaphore.release()
            disconnect { }
        }
    }

    fun sendCommand(payload: SesameOS3Payload, isEncryt: DeviceSegmentType = DeviceSegmentType.cipher, onResponse: SesameOS3ResponseCallback) {



        val tmp = cmdCallBack[payload.itemCode]
//        L.d("hcia", "[put][qeueu][${payload.itemCode}][again]")

        cmdCallBack[payload.itemCode] = onResponse
        if (tmp != null) {
            L.d("hcia", "[qeueu][${payload.itemCode}][again]")
            return
        }
        val  isChipher=if (isEncryt == DeviceSegmentType.cipher) "cipher" else "no cipher"
        L.l("parse data send",payload.itemCode.toString(),isChipher,byToString(payload.toDataWithHeader()),payload.toDataWithHeader().toHexString())
        CoroutineScope(IO).launch {
//            L.d("hcia", "[acquire] semaphore?.availablePermits():" + semaphore?.availablePermits())
            semaphore.acquire()

//            L.d("hcia", "[device] 🀄Command: ==>:" + (payload.itemCode) + " " + isEncryt)
            val say2ssm = if (isEncryt == DeviceSegmentType.cipher) {
                cipher?.encrypt(payload.toDataWithHeader())
            } else {
                payload.toDataWithHeader()
            }
            gattTxBuffer = SesameBleTransmit(isEncryt, say2ssm!!)
            transmit()
        }
    }

    /** 預設指令: 版本。重置。更新韌體(DFU) */
    open fun getVersionTag(result: CHResult<String>) {
        if ((this as CHDevices).checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.versionTag.value, byteArrayOf()), DeviceSegmentType.cipher) { res -> //            val gitTag = res.payload.sliceArray(4..15) //
            // CHIotManager.updateSS2ShadowVertion(this, String(gitTag))
            result.invoke(Result.success(CHResultState.CHResultStateBLE(String(res.payload))))
        }
    }

    open fun reset(result: CHResult<CHEmpty>) {
        sendCommand(SesameOS3Payload(SesameItemCode.Reset.value, byteArrayOf()), DeviceSegmentType.cipher) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                dropKey(result)
            } else {
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
            }
        }
    }

    open fun updateFirmware(onResponse: CHResult<BluetoothDevice>) {
        onResponse.invoke(Result.success(CHResultState.CHResultStateBLE((this as CHDeviceUtil).advertisement?.device!!)))
    }

    fun parceADV(value: CHadv?) {
        value?.let {
            rssi = it.rssi
            deviceId = it.deviceID
            isRegistered = it.isRegistered
            productModel = it.productModel!!
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
                if (isNeedAuthFromServer == true) {
                    CHAccountManager.signGuestKey(CHRemoveSignKeyRequest(deviceId.toString().uppercase(), mSesameToken.toHexString(), sesame2KeyData!!.secretKey)) {
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

internal data class SesameOS3Payload(val itemCode: UByte, val data: ByteArray) {
    fun toDataWithHeader(): ByteArray {
        return byteArrayOf(itemCode.toByte()) + data
    }
}
