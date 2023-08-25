package co.candyhouse.sesame.ble.os2

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.pm.PackageManager
import android.os.Build
import co.candyhouse.sesame.ble.*
import co.candyhouse.sesame.ble.CHDeviceUtil
import co.candyhouse.sesame.ble.SesameResultCode
import co.candyhouse.sesame.ble.SesameNotifypayload
import co.candyhouse.sesame.ble.SSM2OpCode
import co.candyhouse.sesame.ble.SSM2ResponsePayload
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.ble.checkBle
import co.candyhouse.sesame.ble.SesameBleTransmit
import co.candyhouse.sesame.ble.os2.base.CHSesameOS2
import co.candyhouse.sesame.ble.os2.base.SSM2Payload
import co.candyhouse.sesame.ble.os2.base.SesameOS2BleCipher
import co.candyhouse.sesame.ble.os2.base.SesameOS2ResponseCallback
import co.candyhouse.sesame.open.CHBleManager.appContext
import co.candyhouse.sesame.open.CHBleManager.bluetoothAdapter
import co.candyhouse.sesame.db.CHDB
import co.candyhouse.sesame.db.model.CHDevice
import co.candyhouse.sesame.db.model.createHistag
import co.candyhouse.sesame.db.model.hisTagC
import co.candyhouse.sesame.open.*
import co.candyhouse.sesame.open.device.*
import co.candyhouse.sesame.open.CHAccountManager.makeApiCall
import co.candyhouse.sesame.server.dto.*
import co.candyhouse.sesame.server.dto.CHRemoveSignKeyRequest

import co.candyhouse.sesame.utils.*
import co.candyhouse.sesame.utils.EccKey
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.aescmac.AesCmac
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.util.*
import kotlinx.coroutines.channels.Channel

@SuppressLint("MissingPermission") internal class CHSesameBikeDevice() : CHSesameOS2(), CHSesameBike, CHDeviceUtil {


    var isConnectedByWM2: Boolean = false


    override var advertisement: CHadv? = null
        set(value) {
//            L.d("hcia", "adv:" + field + "😷 -> " + value + " " + deviceId.toString()+ deviceStatus)
            field = value
            if (value == null) {
                deviceStatus = CHDeviceStatus.NoBleSignal
                return
            }
            rssi = advertisement?.rssi
//            L.d("hcia", "rssi:" + rssi)
            deviceId = advertisement!!.deviceID
            isRegistered = advertisement!!.isRegistered

            if (deviceStatus.value == CHDeviceLoginStatus.Login && !isRegistered) {
                deviceStatus = CHDeviceStatus.Reset
            }

            if (deviceStatus == CHDeviceStatus.NoBleSignal) {
                deviceStatus = CHDeviceStatus.ReceivedAdV
            }

            if (deviceStatus == CHDeviceStatus.WaitingForAuth && isInternetAvailable()) {
                deviceStatus = CHDeviceStatus.ReceivedAdV
            }

        }


    override fun goIOT() {

    }


    override fun connect(result: CHResult<CHEmpty>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (appContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (CHBleManager.mScanning == CHScanStatus.BleClose) {
                    result.invoke(Result.failure(CHError.BleUnauth.value))
                    return
                }
            }
        }

        if (CHBleManager.mScanning == CHScanStatus.BleClose) {
            result.invoke(Result.failure(CHError.BlePoweroff.value))
            return
        }
        if (deviceStatus != CHDeviceStatus.ReceivedAdV) {
            result.invoke(Result.failure(CHError.Noble.value))
            return
        }

        if (isNeedAuthFromServer == true && isInternetAvailable() == false) {
            deviceStatus = CHDeviceStatus.WaitingForAuth
            return
        }


        if (CHBleManager.connectR.indexOf(advertisement?.device?.address) == -1) {
//            L.b("hcia", customDeviceName + ":連接紀錄:" + advertisement!!.device.address)
            CHBleManager.connectR.add(advertisement!!.device.address)
        } else {
//            L.b("hcia", customDeviceName + ":我已經連接過了：" + (advertisement as CHadv).device.address)
            return
        }

        deviceStatus = CHDeviceStatus.BleConnecting
        result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))

//        L.d("hcia", "Build.VERSION.SDK_INT:" + Build.VERSION.SDK_INT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                L.d("hcia", "ssm" + ":連接 O:")
            bluetoothAdapter.getRemoteDevice(advertisement!!.device.address).connectGatt(appContext, false, mBluetoothGattCallback, BluetoothDevice.TRANSPORT_LE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                L.d("hcia", "ssm" + ":連接 M:")
            bluetoothAdapter.getRemoteDevice(advertisement!!.device.address).connectGatt(appContext, false, mBluetoothGattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
//                L.d("hcia", "ssm" + ":主動連接 old:")
            bluetoothAdapter.getRemoteDevice(advertisement!!.device.address).connectGatt(appContext, false, mBluetoothGattCallback)
        }
    }


    private val mBluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)
            val ssmSay = gattRxBuffer.feed(characteristic!!.value)
            if (ssmSay?.first == DeviceSegmentType.cipher) {
                parceNotifyPayload(cipher!!.decrypt(ssmSay.second))
            }
            if (ssmSay?.first == DeviceSegmentType.plain) {
                parceNotifyPayload(ssmSay.second)
            }
        }

        private fun parceNotifyPayload(palntext: ByteArray) {
            val ssm2notify = SesameNotifypayload(palntext)//1

            if (ssm2notify.notifyOpCode == SSM2OpCode.publish) {
                val ssm2pubPayload = SSM3PublishPayload(ssm2notify.payload)
                onGattSesamePublish(ssm2pubPayload)
            }
            if (ssm2notify.notifyOpCode == SSM2OpCode.response) {
                val ssm2responsePayload = SSM2ResponsePayload(ssm2notify.payload)
                onGattSesameResponse(ssm2responsePayload)
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            transmit()
        }


        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
//            L.d("hcia", "BluetoothGatt " + ":發現服務:")

            for (service in gatt?.services!!) {
                if (service.uuid == Sesame2Chracs.uuidService01) {
                    for (charc in service.characteristics) {
//                        L.d("hcia", "BluetoothGatt 特徵:" + charc.uuid)
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
//                    L.d("hcia", "ssm " + ":連接狀態＋＋" + BleBaseType.GattConnectStateDec(newState) + " 狀態:" + BleBaseType.GattConnectStatusDec(status))
                deviceStatus = CHDeviceStatus.DiscoverServices
                mBluetoothGatt = gatt
                gatt.discoverServices()
            } else {
//                L.d("hcia", "bike " + ":連接狀態 --" + BleBaseType.GattConnectStateDec(newState) + " 狀態:" + BleBaseType.GattConnectStatusDec(status))
//                gatt?.disconnect()
                gatt.close()
                advertisement = null
                mBluetoothGatt = null
                CHBleManager.connectR.remove(gatt.device.address)

            }

        }
    }

    private fun onGattSesameResponse(ssm2ResponsePayload: SSM2ResponsePayload) {
        CoroutineScope(IO).launch {
//            L.d("hcia","解鎖<-------")
            semaphore.receive().invoke(ssm2ResponsePayload)
        }
    }

    private fun onGattSesamePublish(receivePayload: SSM3PublishPayload) {

//        L.d("hcia", "receivePayload.cmdItCode:" + receivePayload.cmdItCode)
        if (receivePayload.cmdItCode == SesameItemCode.login.value) {

//            L.d("hcia", "註冊完畢收到login:")

            val loginResponse = SSMBotLoginResponsePayload(receivePayload.payload)
//            L.d("hcia", ":登入成功 loginResponse:" + loginResponse + " 歷史數量:" + loginResponse.historyCnt)

            sendEncryptCommand(SSM2Payload(SSM2OpCode.update, SesameItemCode.timePhone, System.currentTimeMillis().toUInt32ByteArray())) { }

            mechStatus = loginResponse.SSMBotMechStatus
            deviceStatus = if (mechStatus!!.isInLockRange) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked

        }
        if (receivePayload.cmdItCode == SesameItemCode.initial.value) {
            mSesameToken = receivePayload.payload
            if (isRegistered) {
                if (isNeedAuthFromServer == true) {
                    val userIdx = sesame2KeyData!!.keyIndex.hexStringToByteArray()
                    val appPublicKeyBytes = EccKey.getPubK().hexStringToByteArray()
                    val sessionToken = mAppToken + mSesameToken
                    val signPayload = userIdx + appPublicKeyBytes + sessionToken
                    CHAccountManager.signGuestKey(CHRemoveSignKeyRequest(deviceId.toString().uppercase(), signPayload.toHexString(), sesame2KeyData!!.secretKey)) {
                        it.onSuccess {
                            login(it.data)
                        }
                        it.onFailure {}
                    }

                } else {
                    login()
                }
            } else {
                deviceStatus = CHDeviceStatus.ReadyToRegister
            }
        }

        if (receivePayload.cmdItCode == SesameItemCode.mechStatus.value) {
//            L.d("hcia", "receivePayload.payload:" + receivePayload.payload.toHexString())
            mechStatus = CHSesameBotMechStatus(receivePayload.payload)
//            L.d("hcia", "isStop:" + mechStatus!!.isStop)
//            L.d("hcia", "mechStatus!!.retCode:" + mechStatus!!.retCode + " target:" + mechStatus!!.target)
            deviceStatus = if (mechStatus!!.isInLockRange) CHDeviceStatus.Locked else if (mechStatus!!.isInUnlockRange) CHDeviceStatus.Unlocked else CHDeviceStatus.Moved
//            L.d("hcia", "ssm Status:" + deviceStatus.toString())
        }
    }


    override fun unlock(historyTag: ByteArray?, result: CHResult<CHEmpty>) {
        if (deviceStatus.value == CHDeviceLoginStatus.UnLogin && isConnectedByWM2) {
            CHAccountManager.cmdSesame(SesameItemCode.unlock, this, sesame2KeyData!!.hisTagC(historyTag), result)
        }
        if (checkBle(result)) return
        val his = sesame2KeyData!!.createHistag(historyTag)

        sendEncryptCommand(SSM2Payload(SSM2OpCode.async, SesameItemCode.unlock, his)) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            } else {
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
            }
        }
    }


    override fun login(token: String?) {
//        L.d("hcia", "重作配置semaphore")
        semaphore = Channel(capacity = 1)
        deviceStatus = CHDeviceStatus.BleLogining
        val secret = sesame2KeyData!!.secretKey.hexStringToByteArray()
        val userIdx = sesame2KeyData!!.keyIndex.hexStringToByteArray()
        val ssmPublicKeyBytes = sesame2KeyData!!.sesame2PublicKey.hexStringToByteArray()
        val devicePublicKeyBytes = EccKey.getPubK().hexStringToByteArray()
        val sessionToken = mAppToken + mSesameToken

        val signPayload = userIdx + devicePublicKeyBytes + sessionToken
        val sessionAuth: ByteArray? = if (isNeedAuthFromServer == true) {
            token?.hexStringToByteArray()
        } else {
            AesCmac(secret, 16).computeMac(signPayload)
        }
        val ecdhSecret = EccKey.ecdh(ssmPublicKeyBytes)
        val ecdhSecretPre16 = ecdhSecret.sliceArray(0..15)
        val sessionKey = AesCmac(ecdhSecretPre16, 16).computeMac(sessionToken)
        val loginPayload = userIdx + devicePublicKeyBytes + mAppToken + sessionAuth!!.sliceArray(0..3)
        cipher = SesameOS2BleCipher(sessionKey!!, sessionToken)
        val cmd = SSM2Payload(SSM2OpCode.sync, SesameItemCode.login, loginPayload)
        sendPlainCommand(cmd) { ssm2ResponsePayload ->

//            L.d("hcia", "下指令登入成功")
            if (ssm2ResponsePayload.cmdItCode == SesameItemCode.login.value && ssm2ResponsePayload.cmdResultCode == SesameResultCode.success.value) {

//                L.d("hcia", "登入 payload:" + ssm2ResponsePayload.payload.toHexString())
                val loginResponse = SSMBotLoginResponsePayload(ssm2ResponsePayload.payload)
                val currentTimestamp = System.currentTimeMillis() / 1000
                val timeError = currentTimestamp.minus(loginResponse.systemTime)
//                L.d("hcia", "loginResponse.fw_version:" + loginResponse.fw_version)
//                L.d("hcia", "timeError:" + timeError)
                if (timeError > 3) { // check check  time error
//                    L.d("hcia", "登入後設定時間開始 ==>")
                    sendEncryptCommand(SSM2Payload(SSM2OpCode.update, SesameItemCode.timePhone, System.currentTimeMillis().toUInt32ByteArray())) { }
                }
                mechStatus = loginResponse.SSMBotMechStatus
                deviceStatus = if (mechStatus!!.isInLockRange) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked
//                L.d("hcia", "deviceStatus:" + deviceStatus)
            }
        }
    }

    override fun getVersionTag(result: CHResult<String>) {
        if (checkBle(result)) return

        sendEncryptCommand(SSM2Payload(SSM2OpCode.read, SesameItemCode.versionTag, byteArrayOf())) { res ->
            val gitTag = res.payload.sliceArray(4..15)
//            CHIotManager.updateSS2ShadowVertion(this, String(gitTag))
            result.invoke(Result.success(CHResultState.CHResultStateBLE(String(gitTag))))
        }
    }

    override fun register(resultRegister: CHResult<CHEmpty>) {

        if (deviceStatus != CHDeviceStatus.ReadyToRegister) {
            resultRegister.invoke(Result.failure(NSError("Busy", "CBCentralManager", 7)))
            return
        }
        semaphore = Channel(capacity = 1)
        sendPlainCommand(SSM2Payload(SSM2OpCode.read, SesameItemCode.IRER, byteArrayOf())) { IRRes ->
            val ER = IRRes.payload.drop(16).toByteArray().toHexString()


        }
    }

    override fun reset(result: CHResult<CHEmpty>) {
        if (checkBle(result)) return

        sendEncryptCommand(SSM2Payload(SSM2OpCode.delete, SesameItemCode.registration, byteArrayOf())) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                dropKey(result)
            } else {
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
            }
        }
    }

    override fun updateFirmware(result: CHResult<BluetoothDevice>) {
        L.d("hcia", "啟動dfu" + deviceStatus + " " + isRegistered)

        if (isRegistered) {
            if (checkBle(result)) return
            sendEncryptCommand(SSM2Payload(SSM2OpCode.update, SesameItemCode.enableDFU, "01".hexStringToByteArray())) { res ->
                if (res.cmdResultCode == SesameResultCode.success.value) {
                    result.invoke(Result.success(CHResultState.CHResultStateBLE(advertisement?.device!!)))
                } else {
                    result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
                }
            }
        } else {
            sendPlainCommand(SSM2Payload(SSM2OpCode.update, SesameItemCode.enableDFU, "01".hexStringToByteArray())) { res ->
                if (res.cmdResultCode == SesameResultCode.success.value) {
                    result.invoke(Result.success(CHResultState.CHResultStateBLE(advertisement?.device!!)))
                } else {
                    result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
                }
            }
        }
    }


    private fun sendPlainCommand(payload: SSM2Payload, onResponse: SesameOS2ResponseCallback) {
        CoroutineScope(IO).launch {
            semaphore.send(onResponse)

            sendCommand(SesameBleTransmit(DeviceSegmentType.plain, payload.toDataWithHeader()))
        }
    }

    private fun sendEncryptCommand(payload: SSM2Payload, onResponse: SesameOS2ResponseCallback) {
        CoroutineScope(IO).launch {
            semaphore.send(onResponse)
            val cipherText = cipher!!.encrypt(payload.toDataWithHeader())
            sendCommand(SesameBleTransmit(DeviceSegmentType.cipher, cipherText))
        }
    }

    private fun sendCommand(txClosure: SesameBleTransmit) {
        CoroutineScope(IO).launch {
//            L.d("hcia", "🀄Command: ==>:" + payload.itemCode + " " + payload.opCode + " " + delegate)
            gattTxBuffer = txClosure
//            gattTxBuffer?.payloadCMD = payload
            transmit()
        }
    }

    private fun transmit() {
        if (mCharacteristic == null) {
            return
        }
        val data = gattTxBuffer?.getChunk() ?: return
        mCharacteristic?.value = data
        mBluetoothGatt?.writeCharacteristic(mCharacteristic)
//        L.d("hcia", "botWrite:" + botWrite)
    }

}

internal class SSMBotLoginResponsePayload(loginPayload: ByteArray) {
    var systemTime = loginPayload.sliceArray(0..3).toBigLong()
    var fw_version = loginPayload[4]
    val historyCnt: Byte = loginPayload[6]
    val mech_setting_t = loginPayload.sliceArray(8..19)
    val mech_status_t = loginPayload.sliceArray(20..27)
    var SSMBotMechStatus = CHSesameBotMechStatus(mech_status_t)
    var SSMBotMechSetting = CHSesameBotMechSettings(mech_setting_t[0], mech_setting_t[1], mech_setting_t[2], mech_setting_t[3], mech_setting_t[4], mech_setting_t[5], mech_setting_t[6])
}

