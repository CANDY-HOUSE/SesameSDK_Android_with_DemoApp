package co.candyhouse.sesame.ble.os2

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.pm.PackageManager
import android.os.Build
import co.candyhouse.sesame.ble.CHDeviceUtil
import co.candyhouse.sesame.ble.CHadv
import co.candyhouse.sesame.ble.DeviceSegmentType
import co.candyhouse.sesame.ble.SSM2OpCode
import co.candyhouse.sesame.ble.SSM2ResponsePayload
import co.candyhouse.sesame.ble.SSM3PublishPayload
import co.candyhouse.sesame.ble.Sesame2Chracs
import co.candyhouse.sesame.ble.SesameBleTransmit
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.ble.SesameNotifypayload
import co.candyhouse.sesame.ble.SesameResultCode
import co.candyhouse.sesame.ble.isBleAvailable
import co.candyhouse.sesame.ble.os2.base.CHSesameOS2
import co.candyhouse.sesame.ble.os2.base.SSM2Payload
import co.candyhouse.sesame.ble.os2.base.SesameOS2BleCipher
import co.candyhouse.sesame.ble.os2.base.SesameOS2ResponseCallback
import co.candyhouse.sesame.db.CHDB
import co.candyhouse.sesame.db.model.CHDevice
import co.candyhouse.sesame.db.model.createHistag
import co.candyhouse.sesame.db.model.hisTagC
import co.candyhouse.sesame.open.CHAccountManager
import co.candyhouse.sesame.open.CHAccountManager.makeApiCall
import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.open.CHBleManager.appContext
import co.candyhouse.sesame.open.CHBleManager.bluetoothAdapter
import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.open.CHResultState
import co.candyhouse.sesame.open.CHScanStatus
import co.candyhouse.sesame.open.device.CHDeviceLoginStatus
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHSesame2
import co.candyhouse.sesame.open.device.CHSesame2MechSettings
import co.candyhouse.sesame.open.device.CHSesame2MechStatus
import co.candyhouse.sesame.open.device.NSError
import co.candyhouse.sesame.open.isInternetAvailable
import co.candyhouse.sesame.server.CHIotManager
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.server.dto.CHRemoveSignKeyRequest
import co.candyhouse.sesame.server.dto.CHSS2RegisterReq
import co.candyhouse.sesame.server.dto.CHSS2RegisterReqSig1
import co.candyhouse.sesame.server.dto.CHSS2RegisterRes
import co.candyhouse.sesame.utils.EccKey
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.aescmac.AesCmac
import co.candyhouse.sesame.utils.base64Encode
import co.candyhouse.sesame.utils.base64decodeByteArray
import co.candyhouse.sesame.utils.hexStringToByteArray
import co.candyhouse.sesame.utils.toBigLong
import co.candyhouse.sesame.utils.toHexString
import co.candyhouse.sesame.utils.toReverseBytes
import co.candyhouse.sesame.utils.toUInt32ByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs

internal enum class CHError(val value: NSError) {
    SesameUnlogin(NSError("Sesame BLE unlogin", "SesameSDK", -1)), Noble(NSError("without receivedBle", "SesameSDK", -2)), BlePoweroff(NSError("PoweredOff", "CBCentralManager", 4)), BleUnauth(NSError("Unauthorized", "CBCentralManager", 3)), BUSY(NSError("BUSY", "Sesame2SDK", 7)), INVALID_PARAM(NSError("INVALID_PARAM", "Sesame2SDK", 8)), BleInvalidAction(NSError("InvalidAction", "Sesame2SDK", 9)), NotfoundError(NSError("notfound", "Sesame2SDK", 5)), NetWorkError(NSError("NetWork", "Sesame2SDK", -3)),
}

@SuppressLint("MissingPermission") internal class CHSesame2Device() : CHSesameOS2(), CHSesame2, CHDeviceUtil {
    private var mResultRegister: CHResult<CHEmpty>? = null
    var isConnectedByWM2: Boolean = false

    override fun goIOT() {
//        L.d("hcia", "goIOT:" +this.deviceId)
        CHIotManager.subscribeSesame2Shadow(this) { result ->
            result.onSuccess { resourse ->
                if (deviceStatus.value == CHDeviceLoginStatus.unlogined) {
                    resourse.data.state.reported.mechst?.let { mechShadow ->
                        mechStatus = CHSesame2MechStatus(mechShadow.hexStringToByteArray())
                    }
                }
                resourse.data.state.reported.wm2s?.let { wm2s ->
                    isConnectedByWM2 = wm2s.map { it.value.hexStringToByteArray().first().toInt() }.contains(1)
                }
                if (isConnectedByWM2) {
                    deviceShadowStatus = if ((mechStatus as CHSesame2MechStatus).isInLockRange) CHDeviceStatus.Locked else if ((mechStatus as CHSesame2MechStatus).isInUnlockRange) CHDeviceStatus.Unlocked else CHDeviceStatus.Moved
                } else {
                    deviceShadowStatus = null
                }
            }
        }
    }

    override var advertisement: CHadv? = null
        set(value) {
            field = value
            if (value == null) {
                rssi = -100
                deviceStatus = CHDeviceStatus.NoBleSignal
                return
            }
            rssi = advertisement?.rssi
            deviceId = advertisement!!.deviceID
            isRegistered = advertisement!!.isRegistered
            productModel = advertisement!!.productModel!!

            if (deviceStatus.value == CHDeviceLoginStatus.logined && !isRegistered) {
                deviceStatus = CHDeviceStatus.Reset
            }

            if (deviceStatus == CHDeviceStatus.NoBleSignal) {
                deviceStatus = CHDeviceStatus.ReceivedAdV
            }

            if (deviceStatus == CHDeviceStatus.WaitingForAuth && isInternetAvailable()) {
                deviceStatus = CHDeviceStatus.ReceivedAdV
            }

        }


    override var mechSetting: CHSesame2MechSettings? = null


    override fun getVersionTag(result: CHResult<String>) {
        if (!isBleAvailable(result)) return
        sendEncryptCommand(SSM2Payload(SSM2OpCode.read, SesameItemCode.versionTag, byteArrayOf())) { res ->
            val gitTag = res.payload.sliceArray(4..15)
            result.invoke(Result.success(CHResultState.CHResultStateBLE(String(gitTag))))
        }
    }

    override fun enableAutolock(delay: Int, historytag: ByteArray?, result: CHResult<Int>) {
        if (!isBleAvailable(result)) return

        sendEncryptCommand(SSM2Payload(SSM2OpCode.update, SesameItemCode.autolock, delay.toShort().toReverseBytes() + sesame2KeyData!!.createHistag(historytag))) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                result.invoke(Result.success(CHResultState.CHResultStateBLE(delay)))
            } else {
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
            }
        }
    }

    override fun disableAutolock(historytag: ByteArray?, result: CHResult<Int>) {
        enableAutolock(0, historytag, result)
    }

    override fun getAutolockSetting(result: CHResult<Int>) {
        if (!isBleAvailable(result)) return

        sendEncryptCommand(SSM2Payload(SSM2OpCode.read, SesameItemCode.autolock, byteArrayOf())) { res ->
//            val autoLockSecond = res.payload.reversedArray().toInt()
            val autoLockSecond =  java.lang.Long.parseLong(res.payload.reversedArray().toHexString(), 16).toInt()
            result.invoke(Result.success(CHResultState.CHResultStateBLE(autoLockSecond)))
        }
    }


    override fun toggle(historytag: ByteArray?, result: CHResult<CHEmpty>) {


        if (deviceStatus.value == CHDeviceLoginStatus.unlogined && isConnectedByWM2) {
            CHAccountManager.cmdSesame(SesameItemCode.toggle, this, sesame2KeyData!!.hisTagC(historytag), result)
        } else {

            if (mechStatus?.isInLockRange == true) {
                unlock(historytag, result)
            } else {
                lock(historytag, result)
            }
        }
    }

    override fun lock(historytag: ByteArray?, result: CHResult<CHEmpty>) {
        if (deviceStatus.value == CHDeviceLoginStatus.unlogined && isConnectedByWM2) {
            CHAccountManager.cmdSesame(SesameItemCode.lock, this, sesame2KeyData!!.hisTagC(historytag), result)
        } else {
            if (!isBleAvailable(result)) return
            sendEncryptCommand(SSM2Payload(SSM2OpCode.async, SesameItemCode.lock, sesame2KeyData!!.createHistag(historytag))) { res ->
                if (res.cmdResultCode == SesameResultCode.success.value) {
                    result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
                } else {
                    result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
                }
            }
        }
    }

    override fun unlock(historytag: ByteArray?, result: CHResult<CHEmpty>) {

        if (deviceStatus.value == CHDeviceLoginStatus.unlogined && isConnectedByWM2) {
            CHAccountManager.cmdSesame(SesameItemCode.unlock, this, sesame2KeyData!!.hisTagC(historytag), result)
        } else {
            if (!isBleAvailable(result)) return
            val his = sesame2KeyData!!.createHistag(historytag)
            sendEncryptCommand(SSM2Payload(SSM2OpCode.async, SesameItemCode.unlock, his)) { res ->
                if (res.cmdResultCode == SesameResultCode.success.value) {
                    result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
                } else {
                    result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
                }
            }
        }

    }

    override fun login(token: String?) { //        L.d("hcia", "loginSesame BluetoothGatt " + " mSSTK:" + mSesameToken.toHexString())
        //        L.d("hcia", "semaphore.isEmpty:" + semaphore.isEmpty)
//        L.d("hcia", "[ss2][login]")
//        val ssm = this
//        L.d("hcia", "ssm.sesame2KeyData!!.deviceUUID:" + ssm.sesame2KeyData!!.deviceUUID.uppercase())
//        val noHashUUID = ssm.sesame2KeyData!!.deviceUUID.replace("-", "")
//        val b64k = noHashUUID.hexStringToByteArray().base64Encode().replace("=", "")
//        val ssmIRData = b64k.toByteArray()
//        val ssmPKData = ssm.sesame2KeyData!!.sesame2PublicKey.hexStringToByteArray()
//        val ssmSecKa = ssm.sesame2KeyData!!.secretKey.hexStringToByteArray()
//        val ssmUUid = ssm.sesame2KeyData!!.deviceUUID.uppercase(Locale.getDefault()).toByteArray()
//        val allKey = ssmIRData + ssmPKData + ssmSecKa + ssmUUid
//        L.d("hcia", "[TEST][mSesameToken]:" + mSesameToken.toHexString()+" "+mSesameToken.size)
//        L.d("hcia", "[TEST][IR]:" + ssmIRData.toHexString())
//        L.d("hcia", "[TEST][ssm-public-key]:" + ssmPKData.toHexString())
//        L.d("hcia", "[TEST][ssm-secret-key]:" + ssmSecKa.toHexString())
//        L.d("hcia", "[TEST][ALL]:" + allKey.toHexString())

        semaphore = Channel(capacity = 1)
        deviceStatus = CHDeviceStatus.BleLogining
        val secret = sesame2KeyData!!.secretKey.hexStringToByteArray()
        val userIdx = sesame2KeyData!!.keyIndex.hexStringToByteArray()
        val ssmPublicKeyBytes = sesame2KeyData!!.sesame2PublicKey.hexStringToByteArray()
        val appPublicKeyBytes = EccKey.getPubK().hexStringToByteArray()
        val sessionToken = mAppToken + mSesameToken
        val signPayload = userIdx + appPublicKeyBytes + sessionToken
        val sessionAuth: ByteArray?
        if (isNeedAuthFromServer == true) {
            sessionAuth = token?.hexStringToByteArray()
        } else {
            sessionAuth = AesCmac(secret, 16).computeMac(signPayload)
        }

        val ecdhSecret = EccKey.ecdh(ssmPublicKeyBytes)
        val ecdhSecretPre16 = ecdhSecret.sliceArray(0..15)
        val sessionKey = AesCmac(ecdhSecretPre16, 16).computeMac(sessionToken)
//        L.d("hcia", "ecdhSecretPre16:" + ecdhSecretPre16.toHexString())
//        L.d("hcia", "sessionKey:" + sessionKey!!.toHexString())
        cipher = SesameOS2BleCipher(sessionKey!!, sessionToken)
        val loginPayload = userIdx + appPublicKeyBytes + mAppToken + sessionAuth!!.sliceArray(0..3)
//        L.d("hcia", "loginPayload:"+loginPayload.toHexString()+" " + loginPayload.size)
        val cmd = SSM2Payload(SSM2OpCode.sync, SesameItemCode.login, loginPayload) //        L.d("hcia", "下指令登入成功---->")
        sendEncryptCommand(cmd, DeviceSegmentType.plain) { ssm2ResponsePayload -> //            L.d("hcia", "下指令登入成功<-------")
            if (ssm2ResponsePayload.cmdItCode == SesameItemCode.login.value && ssm2ResponsePayload.cmdResultCode == SesameResultCode.success.value) {
//                L.d("hcia", "ssm2ResponsePayload.payload:" + ssm2ResponsePayload.payload.toHexString())
                val loginResponse = SSM2LoginResponsePayload(ssm2ResponsePayload.payload)
                val currentTimestamp = System.currentTimeMillis() / 1000
                val timeError = currentTimestamp.minus(loginResponse.systemTime)
                if (abs(timeError) > 3) {
                    if (loginResponse.fw_version >= 1) {
                        sendEncryptCommand(SSM2Payload(SSM2OpCode.update, SesameItemCode.timePhone, System.currentTimeMillis().toUInt32ByteArray())) { }
                    }
                }
                mechStatus = loginResponse.SSM2MechStatus
                mechSetting = loginResponse.SSM2MechSetting
                deviceStatus = if (!mechSetting!!.isConfigured) CHDeviceStatus.NoSettings else if ((mechStatus as CHSesame2MechStatus).isInLockRange) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked

            }
        }
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

        if (isNeedAuthFromServer == true && !isInternetAvailable()) {
            deviceStatus = CHDeviceStatus.WaitingForAuth
            return
        }

        // 检查 advertisement 是否为 null
        if (advertisement?.device?.address == null) {
            result.invoke(Result.failure(CHError.Noble.value))
            return
        }
        val address=advertisement!!.device.address

        if (CHBleManager.connectR.indexOf(address) == -1) {
            CHBleManager.connectR.add(address)
        } else {
            return
        }

        deviceStatus = CHDeviceStatus.BleConnecting
        result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))

        val remoteDevice = bluetoothAdapter.getRemoteDevice(address)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            remoteDevice.connectGatt(appContext, false, mBluetoothGattCallback, BluetoothDevice.TRANSPORT_LE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            remoteDevice.connectGatt(appContext, false, mBluetoothGattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            remoteDevice.connectGatt(appContext, false, mBluetoothGattCallback)
        }
    }

    private val mBluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)
//            L.d("hcia", "[ssm][say]:" + characteristic!!.value.toHexString())
            val ssmSay = gattRxBuffer.feed(characteristic!!.value)
          //  L.d("sendEncryptCommand","parceNotifyPayloadfirst:"+ssmSay?.first)
            if (ssmSay?.first == DeviceSegmentType.cipher) {
                parceNotifyPayload(cipher!!.decrypt(ssmSay.second))
            }
            if (ssmSay?.first == DeviceSegmentType.plain) {
                parceNotifyPayload(ssmSay.second)
            }
        }

        private fun parceNotifyPayload(palntext: ByteArray) {
//            L.d("hcia", "palntext:" + palntext.toHexString())

       //     L.d("sendEncryptCommand","parceNotifyPayload:"+palntext.toHexString())
            val ssm2notify = SesameNotifypayload(palntext) //1
            if (ssm2notify.notifyOpCode == SSM2OpCode.publish) {
                val ssm2pubPayload = SSM3PublishPayload(ssm2notify.payload)
                onGattSesamePublish(ssm2pubPayload)
            }
            if (ssm2notify.notifyOpCode == SSM2OpCode.response) {
                val ssm2responsePayload = SSM2ResponsePayload(ssm2notify.payload)
//                L.d("hcia", "🀄Command:<==:" + ssm2responsePayload.cmdOPCode + "-" + ssm2responsePayload.cmdItCode + "-" + ssm2responsePayload.cmdResultCode)
                CoroutineScope(IO).launch {
                    semaphore.receive().invoke(ssm2responsePayload)
                }
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
//                L.d("hcia", "service.uuid:" + service.uuid)
                if (service.uuid == Sesame2Chracs.uuidService01) {
                    for (charc in service.characteristics) {
//                        L.d("hcia", "BluetoothGatt 特徵:" + charc.uuid)
//                        L.d("hcia", "charc.uuid:" + charc.uuid)
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
            if (newState == BluetoothProfile.STATE_CONNECTED) { //                L.d("hcia", "ssm " + ":連接狀態＋＋" + BleBaseType.GattConnectStateDec(newState) + " 狀態:" + BleBaseType.GattConnectStatusDec(status))
                deviceStatus = CHDeviceStatus.DiscoverServices
                mBluetoothGatt = gatt
                gatt.discoverServices()
            } else {
//                L.d("hcia", "ssm " + ":連接狀態 --" + BleBaseType.GattConnectStateDec(newState) + " 狀態:" + BleBaseType.GattConnectStatusDec(status))
                gatt.close()
                advertisement = null
                mBluetoothGatt = null
                CHBleManager.connectR.remove(gatt.device.address)
            }
        }
    }


    override fun register(resultRegister: CHResult<CHEmpty>) {

        if (deviceStatus != CHDeviceStatus.ReadyToRegister) {
            resultRegister.invoke(Result.failure(CHError.BUSY.value))
            return
        }


        semaphore = Channel(capacity = 1)

        sendEncryptCommand(SSM2Payload(SSM2OpCode.read, SesameItemCode.IRER, byteArrayOf()), DeviceSegmentType.plain) { IRRes ->
            L.d("hcia", "IRER:" + IRRes.payload.toHexString())
            val ER = IRRes.payload.drop(16).toByteArray().toHexString()

            makeApiCall(resultRegister) {

                L.d("hcia", "註冊請求開始 ==> deviceStatus:" + deviceStatus + " deviceId:" + deviceId)
             //   val registerSesame1 = CHServerAuth.getRegisterKey(KeyQues(EccKey.getRegisterAK(), mSesameToken.base64Encode(), ER))
                val registerSesame1: CHSS2RegisterRes = CHAccountManager.jpAPIClient.myDevicesRegisterSesame2Post(deviceId.toString(), CHSS2RegisterReq(CHSS2RegisterReqSig1(EccKey.getRegisterAK(), mSesameToken.base64Encode(), ER, advertisement!!.productModel!!.productType().toString())))
                deviceStatus = CHDeviceStatus.Registering

                val sig1 = registerSesame1.sig1.base64decodeByteArray().sliceArray(0..3)
                val appPubKey = EccKey.getPubK().hexStringToByteArray()
                val serverToken = registerSesame1.st.base64decodeByteArray()
                val sesamePublicKey = registerSesame1.pubkey.base64decodeByteArray()
                val ecdhSecret = EccKey.ecdh(sesamePublicKey)
                val ecdhSecretPre16 = ecdhSecret.sliceArray(0..15)
                val payload = sig1 + appPubKey + serverToken
                val cmd = SSM2Payload(SSM2OpCode.create, SesameItemCode.registration, payload)

                val sessionToken = serverToken + mSesameToken
                val registerKey = AesCmac(ecdhSecretPre16, 16).computeMac(sessionToken)
                val ownerKey = AesCmac(registerKey!!, 16).computeMac("owner_key".toByteArray())!!
                val sessionKey = AesCmac(registerKey, 16).computeMac(sessionToken)
                cipher = SesameOS2BleCipher(sessionKey!!, sessionToken)
                sendEncryptCommand(cmd, DeviceSegmentType.plain) {

                    isRegistered = true
                    mResultRegister = resultRegister
                    val candyDevice = CHDevice(deviceId.toString(), advertisement!!.productModel!!.deviceModel(), null, "0000", ownerKey.toHexString(), sesamePublicKey.toHexString())
                    sesame2KeyData = candyDevice

                    CHDB.CHSS2Model.insert(candyDevice) {}
                }
            }

        }

    }

    private fun sendEncryptCommand(payload: SSM2Payload, isCipher: DeviceSegmentType = DeviceSegmentType.cipher, onResponse: SesameOS2ResponseCallback) {


        CoroutineScope(IO).launch {
            semaphore.send(onResponse)
//            L.d("hcia", "🀄Command: ==>:" + payload.itemCode + " " + payload.opCode)
            gattTxBuffer = (SesameBleTransmit(isCipher, if (isCipher == DeviceSegmentType.cipher) cipher!!.encrypt(payload.toDataWithHeader()) else payload.toDataWithHeader()))
            transmit()
        }
    }

    private fun transmit() {
        mCharacteristic ?: return //todo check return work
        mCharacteristic?.value = gattTxBuffer?.getChunk() ?: return

//        mBluetoothGatt?.writeCharacteristic(mCharacteristic)
        val check = mBluetoothGatt?.writeCharacteristic(mCharacteristic)
//        L.d("hcia", "[app][say]:" + mCharacteristic?.value?.toHexString() + " check:" + check)

        if (check == false) {
            disconnect { }
        }
    }

    private fun onGattSesamePublish(receivePayload: SSM3PublishPayload) {
        if (receivePayload.cmdItCode == SesameItemCode.login.value) {
            val loginResponse = SSM2LoginResponsePayload(receivePayload.payload)
            sendEncryptCommand(SSM2Payload(SSM2OpCode.update, SesameItemCode.timePhone, System.currentTimeMillis().toUInt32ByteArray())) {
                mResultRegister?.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            }
            mechStatus = loginResponse.SSM2MechStatus
            mechSetting = loginResponse.SSM2MechSetting
            deviceStatus = if (!mechSetting!!.isConfigured) CHDeviceStatus.NoSettings else if (mechStatus!!.isInLockRange) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked
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
            mechStatus = CHSesame2MechStatus(receivePayload.payload)
            if ((mechStatus as CHSesame2MechStatus).retCode != 0) {

                readHistoryCommand {}
            } else if (mechStatus!!.target == Short.MIN_VALUE) {
                readHistoryCommand {}
            }
            deviceStatus = if (!mechSetting!!.isConfigured) CHDeviceStatus.NoSettings else if (mechStatus!!.isInLockRange) CHDeviceStatus.Locked else if ((mechStatus as CHSesame2MechStatus)!!.isInUnlockRange) CHDeviceStatus.Unlocked else CHDeviceStatus.Moved
        }
    }

    override fun configureLockPosition(lockTarget: Short, unlockTarget: Short, result: CHResult<CHEmpty>) {
        val payload = CHSesameLockPositionConfiguration((lockTarget.toInt() * 1024 / 360).toShort(), (unlockTarget.toInt() * 1024 / 360).toShort()).toPayload() + sesame2KeyData!!.createHistag(null)
        val cmd = SSM2Payload(SSM2OpCode.update, SesameItemCode.mechSetting, payload)
        sendEncryptCommand(cmd) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                mechSetting = CHSesame2MechSettings(CHSesameLockPositionConfiguration((lockTarget.toInt() * 1024 / 360).toShort(), (unlockTarget.toInt() * 1024 / 360).toShort()).toPayload())
                deviceStatus = if (!mechSetting!!.isConfigured) CHDeviceStatus.NoSettings else if (mechStatus!!.isInLockRange) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            } else {
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
            }
        }
    }

    override fun reset(result: CHResult<CHEmpty>) {
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
            if (!isBleAvailable(result)) return
            sendEncryptCommand(SSM2Payload(SSM2OpCode.update, SesameItemCode.enableDFU, "01".hexStringToByteArray())) { res ->
                if (res.cmdResultCode == SesameResultCode.success.value) {
                    result.invoke(Result.success(CHResultState.CHResultStateBLE(advertisement?.device!!)))
                } else {
                    result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
                }
            }
        } else {
            sendEncryptCommand(SSM2Payload(SSM2OpCode.update, SesameItemCode.enableDFU, "01".hexStringToByteArray()), DeviceSegmentType.plain) { res ->
                if (res.cmdResultCode == SesameResultCode.success.value) {
                    result.invoke(Result.success(CHResultState.CHResultStateBLE(advertisement?.device!!)))
                } else {
                    result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
                }
            }
        }
    }

    private fun readHistoryCommand(result: CHResult<CHEmpty>) {
        if (!isBleAvailable(result)) return

        L.d("sendEncryptCommand", "readHistoryCommand")
        sendEncryptCommand(
            SSM2Payload(
                SSM2OpCode.read,
                SesameItemCode.history,
                if (isInternetAvailable()) byteArrayOf(0x01) else byteArrayOf(0x00)
            )
        ) { res ->
            onHistoryReceived(res.payload)
            if (res.cmdResultCode == SesameResultCode.success.value) {
                if (res.payload.size >= 13) {  // 添加边界检查
                    val data = res.payload.toHexString()
                    if (isInternetAvailable()) {
                        CHAccountManager.postSS2History(deviceId.toString().uppercase(), data) {}
                    }
                }
            }
        }
    }
}

internal class SSM2LoginResponsePayload(loginPayload: ByteArray) {
    var systemTime = loginPayload.sliceArray(0..3).toBigLong()
    var fw_version = loginPayload[4]
    val historyCnt: Byte = loginPayload[6]
    val mech_setting_t = loginPayload.sliceArray(8..19)
    val mech_status_t = loginPayload.sliceArray(20..27)
    var SSM2MechStatus = CHSesame2MechStatus(mech_status_t)
    var SSM2MechSetting = CHSesame2MechSettings(mech_setting_t)
}
internal class CHSesameLockPositionConfiguration(private val lockTarget: Short, private val unlockTarget: Short) {
    private val range = 150
    private val lockRangeMin: Short = (lockTarget - range).toShort()
    private val lockRangeMax: Short = (lockTarget + range).toShort()
    private val unlockRangeMin: Short = (unlockTarget - range).toShort()
    private val unlockRangeMax: Short = (unlockTarget + range).toShort()

    internal fun toPayload(): ByteArray {
        return lockTarget.toReverseBytes() + unlockTarget.toReverseBytes() + lockRangeMin.toReverseBytes() + lockRangeMax.toReverseBytes() + unlockRangeMin.toReverseBytes() + unlockRangeMax.toReverseBytes()
    }
}