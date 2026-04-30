package co.candyhouse.sesame.ble.os3

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
import co.candyhouse.sesame.ble.SSM3PublishPayload
import co.candyhouse.sesame.ble.SSM3ResponsePayload
import co.candyhouse.sesame.ble.SesameNotifypayload
import co.candyhouse.sesame.ble.SesameResultCode
import co.candyhouse.sesame.ble.os2.CHError
import co.candyhouse.sesame.ble.os3.base.CHSesameOS3
import co.candyhouse.sesame.ble.os3.base.SesameOS3BleCipher
import co.candyhouse.sesame.ble.os3.base.SesameOS3Payload
import co.candyhouse.sesame.db.CHDB
import co.candyhouse.sesame.db.model.CHDevice
import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.open.CHBleManager.appContext
import co.candyhouse.sesame.open.CHBleManager.bluetoothAdapter
import co.candyhouse.sesame.open.CHScanStatus
import co.candyhouse.sesame.open.devices.CHWifiModule2
import co.candyhouse.sesame.open.devices.CHWifiModule2Delegate
import co.candyhouse.sesame.open.devices.CHWifiModule2MechSettings
import co.candyhouse.sesame.open.devices.CHWifiModule2NetWorkStatus
import co.candyhouse.sesame.open.devices.base.CHDeviceLoginStatus
import co.candyhouse.sesame.open.devices.base.CHDeviceStatus
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.open.devices.base.CHProductModel
import co.candyhouse.sesame.open.devices.base.CHSesameLock
import co.candyhouse.sesame.open.devices.base.NSError
import co.candyhouse.sesame.server.CHAPIClientBiz
import co.candyhouse.sesame.server.CHIotManager
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.CHResultState
import co.candyhouse.sesame.utils.EccKey
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.aescmac.AesCmac
import co.candyhouse.sesame.utils.base64Encode
import co.candyhouse.sesame.utils.base64decodeHex
import co.candyhouse.sesame.utils.bytesToShort
import co.candyhouse.sesame.utils.divideArray
import co.candyhouse.sesame.utils.hexStringToByteArray
import co.candyhouse.sesame.utils.noHashtoUUID
import co.candyhouse.sesame.utils.toHexString
import java.util.Locale
import java.util.Locale.getDefault
import java.util.UUID
import kotlin.experimental.and

@SuppressLint("MissingPermission") internal class CHWifiModule2Device : CHSesameOS3(), CHWifiModule2, CHDeviceUtil {
    var WM2LasItemCode: UByte = WM2ActionCode.CODE_NON.value

    override var ssm2KeysMap: MutableMap<String, String> = mutableMapOf()

//    override var networkStatus: CHWifiModule2NetWorkStatus? = null
//        set(value) {
//            field = value //            L.d("hcia", "🥝 設定networkStatus:" + networkStatus)
//            (delegate as? CHWifiModule2Delegate)?.onNetWorkStatusChanged(this, networkStatus!!)
//        }

    override var mechSetting: CHWifiModule2MechSettings? = CHWifiModule2MechSettings(null, null)
        set(value) {
            if (field != value) {
                field = value
            }
        }


    override fun goIOT() {
        L.d("hcia", "[wm2] goIOT:" )
        CHIotManager.subscribeWifiModule2(this) {
            it.onSuccess {
                val isConnectIOT = it.data.state.reported.c //               L.d("hcia", "🥝 wm2回傳收到影子消息 isConnectIOT:" + isConnectIOT)
                isConnectIOT?.apply { //                    CHWifiModule2NetWorkStatus(false, false, isConnectIOT, false,false,false)
                    mechStatus = CHWifiModule2NetWorkStatus((mechStatus as? CHWifiModule2NetWorkStatus)?.isAPWork, (mechStatus as? CHWifiModule2NetWorkStatus)?.isNetWork, isConnectIOT, false, false, false, null)
                }

                val ssks = it.data.state.reported.ssks

                val keysListData = ssks?.hexStringToByteArray() //6e4f547250375a5137464d486868516c73385367347701 //                L.d("hcia", "🐖 keysListData?.size:" + keysListData?.size)//23
                val ssm2KeysMapFromIOT: MutableMap<String, String> = mutableMapOf()

                val keyDatas = keysListData?.divideArray(23)
                keyDatas?.forEach {
                    val ss2_ir_22 = it.sliceArray(IntRange(0, 21))
                    val device_status = it[22]
                    try {
                        val ssmID = (String(ss2_ir_22) + "==").base64decodeHex().noHashtoUUID() //                        L.d("hcia", "ssmID:" + ssmID)
                        ssm2KeysMapFromIOT.put(ssmID!!.toString(), device_status.toString())
                    } catch (e: Exception) {
                        L.d("hcia", "🐖 IOT e:" + e)
                    } //                        L.d("hcia", "🐖 IOT 收到鑰匙上鎖狀態 device_status:" + device_status)//ss2_device_status;// 0:沒連上 ,1:藍芽連上 ,2:登入成功
                }

                //                L.d("hcia", "ssm2KeysMapFromIOT:" + ssm2KeysMapFromIOT)
                if (deviceStatus.value == CHDeviceLoginStatus.unlogined) {
                    ssm2KeysMap.clear()
                    ssm2KeysMap.putAll(ssm2KeysMapFromIOT)
                    (delegate as? CHWifiModule2Delegate)?.onSSM2KeysChanged(this, ssm2KeysMapFromIOT)
                }

            } //end success
        }

    }


    override var advertisement: CHadv? = null
        set(value) { //            L.d("hcia", "adv:" + field + "😷 -> " + value + " " + deviceId.toString()+ deviceStatus)

            field = value
            if (value == null) {
                deviceStatus = CHDeviceStatus.NoBleSignal
                return
            }
            rssi = advertisement!!.rssi
            deviceId = advertisement!!.deviceID

            isRegistered = advertisement?.isRegistered == true

            if (deviceStatus == CHDeviceStatus.NoBleSignal || deviceStatus == CHDeviceStatus.Busy) { //                L.d("hcia", "advertisement?.isConnecable:" + advertisement?.isConnecable)
                if (advertisement?.isConnecable == true) {
                    deviceStatus = CHDeviceStatus.ReceivedAdV
                } else {
                    deviceStatus = CHDeviceStatus.Busy
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
        result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty()))) //        L.d("hcia", "wm2 cmd connect:")


        if (CHBleManager.connectR.indexOf(advertisement?.device?.address) == -1) { //            L.d("hcia", "連接紀錄:" + advertisement!!.device.address)
            CHBleManager.connectR.add(advertisement!!.device.address)
        } else { //            L.d("hcia", "我已經連接過了：" + advertisement!!.device.address)
            return
        }

        deviceStatus = CHDeviceStatus.BleConnecting
        result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))

        //            withContext(Main){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //                L.d("hcia", "wm2" + ":連接 O:")
            bluetoothAdapter.getRemoteDevice(advertisement!!.device.address).connectGatt(CHBleManager.appContext, false, mBluetoothGattCallback, BluetoothDevice.TRANSPORT_LE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //                L.d("hcia", "wm2" + ":連接 M:")
            bluetoothAdapter.getRemoteDevice(advertisement!!.device.address).connectGatt(CHBleManager.appContext, false, mBluetoothGattCallback, BluetoothDevice.TRANSPORT_LE)
        } else { //                L.d("hcia", "wm2" + ":主動連接 old:")
            bluetoothAdapter.getRemoteDevice(advertisement!!.device.address).connectGatt(CHBleManager.appContext, false, mBluetoothGattCallback)
        }

    }

    val mBluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic) //            L.d("hcia", "收到廣播--->:")
            val wm2Say = gattRxBuffer.feed(characteristic!!.value)
            if (wm2Say?.first == DeviceSegmentType.cipher) { //                L.d("hcia", "收到加密==>:" + wm2Say.second.toHexString())
                val pl = cipher!!.decrypt(wm2Say.second) //                L.d("hcia", "解密<==:" + pl.toHexString())
                parseNotifyPayload(pl)
            }

            if (wm2Say?.first == DeviceSegmentType.plain) { //                L.d("hcia", "明文:")
                //                L.d("hcia", "wm2Say.second:" + wm2Say.second.toHexString())
                parseNotifyPayload(wm2Say.second)
            }

        }

        private fun parseNotifyPayload(palntext: ByteArray) {
            val ssm2notify = SesameNotifypayload(palntext)
//            L.d("hcia", "[bk2] ssm2notify.notifyOpCode:" + ssm2notify.notifyOpCode)
            if (ssm2notify.notifyOpCode == SSM2OpCode.response) {
                onGattSesameResponse(SSM3ResponsePayload(ssm2notify.payload))
            } else if (ssm2notify.notifyOpCode == SSM2OpCode.publish) {
                onGattWM2Publish(SSM3PublishPayload(ssm2notify.payload))
            }
        }

        private fun onGattSesameResponse(wm2RespPl: SSM3ResponsePayload) {
            L.d("hcia", "🀄Command:<==:" + wm2RespPl.cmdItCode + " - " + wm2RespPl.cmdResultCode + " " + String(wm2RespPl.payload) + "<--")
            cmdCallBack[wm2RespPl.cmdItCode]?.invoke(wm2RespPl)
            cmdCallBack.remove(wm2RespPl.cmdItCode)
            L.d("hcia", "1wm2RespPl.cmdItCode:" + wm2RespPl.cmdItCode)
//            L.d("hcia", "2cmdCallBack:" + cmdCallBack)

            /* 解决 bug： APP 发 3， WM2 回 3； 如此循环超过六七次后， APP 发 3， WM2 回 4 。
               bug 复现的具体操作： 在选wifi SSID 的页面， 点任一 SSID，弹窗选取消，如此循环 直至出现 “APP 发 3 ， WM2 回 4” 的现象。
2024-05-07 10:28:49.934 14582-14582 harry                   co.candyhouse.sesame2                D  📡 setWifiSSID sendCommand start !!!  (CHWifiModule2Device.kt:294) 主線成
2024-05-07 10:28:49.935 14582-14582 harry                   co.candyhouse.sesame2                D  [sendCommand：3]  (CHSesameOS3.kt:217) 主線成
2024-05-07 10:28:49.935 14582-14582 harry                   co.candyhouse.sesame2                D  📡 setWifiSSID sendCommand end !!!  (CHWifiModule2Device.kt:305) 主線成
2024-05-07 10:28:50.549 14582-14596 hcia                    co.candyhouse.sesame2                D  🀄Command:<==:4 - 5 <--  (CHWifiModule2Device.kt:179) 副線成
             */
            if ((wm2RespPl.cmdItCode == WM2ActionCode.UPDATE_WIFI_PASSWORD.value) && (WM2LasItemCode == WM2ActionCode.UPDATE_WIFI_SSID.value)) {
                cmdCallBack.remove(WM2ActionCode.UPDATE_WIFI_SSID.value)
                WM2LasItemCode = WM2ActionCode.CODE_NON.value
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status) //            L.d("hcia", "3☂️ 藍芽系統寫出成功")
            transmit()

        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status) //            L.d("hcia", "BluetoothGatt " + ":發現服務:")

            for (service in gatt?.services!!) { //                L.d("hcia", "BluetoothGatt 服務:" + service.uuid)
                if (service.uuid == Wm2Chracs.uuidService01) {
                    for (charc in service.characteristics) {
                        if (charc.uuid == Wm2Chracs.writeChrac) {
                            mCharacteristic = charc
                        }

                        if (charc.uuid == Wm2Chracs.receiveChr) { //                            L.d("hcia", "設定收聽通道:" + charc.uuid)
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
            if (newState == BluetoothProfile.STATE_CONNECTED) { //                L.d("hcia", "📡 wm2" + ":連接狀態＋＋" + BleBaseType.GattConnectStateDec(newState) + " 狀態:" + BleBaseType.GattConnectStatusDec(status))
                deviceStatus = CHDeviceStatus.DiscoverServices
                mBluetoothGatt = gatt
                gatt.discoverServices()
            } else { //                L.d("hcia", "📡 wm2 :" + ":連接狀態 --" + BleBaseType.GattConnectStateDec(newState) + " 狀態:" + BleBaseType.GattConnectStatusDec(status))
                gatt.disconnect()
                gatt.close()
                advertisement = null
                mBluetoothGatt = null
                mCharacteristic = null
                CHBleManager.connectR.remove(gatt.device.address)
                cmdCallBack.clear()
            } //end if

        }
    }


    override fun register(result: CHResult<CHEmpty>) {
        if (isRegistered) {
            result.invoke(Result.failure(CHError.BleInvalidAction.value))
            return
        }
        if (deviceStatus != CHDeviceStatus.ReadyToRegister) {
            result.invoke(Result.failure(CHError.BleInvalidAction.value))
            return
        }
        deviceStatus = CHDeviceStatus.Registering

        sendCommand(SesameOS3Payload(WM2ActionCode.REGISTER_WM2.value, EccKey.getPubK().hexStringToByteArray()), DeviceSegmentType.plain) { res -> //            L.d("hcia", "res.cmdResultCode:" + res.cmdResultCode)
            if (res.cmdResultCode == SesameResultCode.success.value) {
                isRegistered = true
                deviceStatus = CHDeviceStatus.WaitApConnect
                L.d("hcia", "res.payload.toHexString():" + res.payload.toHexString())
                val ecdhSecret_pre16 = EccKey.ecdh(res.payload.sliceArray(0..63)).sliceArray(0..15)
                val wm2Key = ecdhSecret_pre16.toHexString()
                L.d("hcia", "存下 wm_2 wm2Key:" + wm2Key)

                val candyDevice = CHDevice(deviceId.toString(), CHProductModel.WM2.deviceModel(), null, "", wm2Key, "")
                sesame2KeyData = candyDevice

                cipher = SesameOS3BleCipher("customDeviceName", ecdhSecret_pre16, mSesameToken!!) //                L.d("hcia", "設定好加密Wm2BleCipher:")

                CHDB.CHSS2Model.insert(candyDevice) {
                    result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
                }


            } else {
                result.invoke(Result.failure(CHError.BleInvalidAction.value))
            }
        }
    }

    override fun login(token: String?) {
        deviceStatus = CHDeviceStatus.BleLogining
        val loginTag = AesCmac(sesame2KeyData!!.secretKey.hexStringToByteArray(), 16).computeMac(mSesameToken)
        cipher = SesameOS3BleCipher("customDeviceName", sesame2KeyData!!.secretKey.hexStringToByteArray(), mSesameToken)
        sendCommand(SesameOS3Payload(WM2ActionCode.LOGIN_WM2.value, loginTag!!), DeviceSegmentType.plain) { res ->
            L.d("hcia", "📡 登入成功:" + res.payload.toHexString())
        }
    }

    override fun scanWifiSSID(result: CHResult<CHEmpty>) {
        sendCommand(SesameOS3Payload(WM2ActionCode.SCAN_WIFI_SSID.value, byteArrayOf())) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                L.d("hcia", "掃描wifi完畢:" + String(res.payload))
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))

            } else {
                L.d("hcia", "掃描wifi錯誤:" + res.cmdResultCode.toString())
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", (res.cmdResultCode).toInt())))
            }
        }
    }

    override fun setWifiSSID(ssid: String, result: CHResult<CHEmpty>) {
        sendCommand(SesameOS3Payload(WM2ActionCode.UPDATE_WIFI_SSID.value, ssid.toByteArray())) { res ->
            WM2LasItemCode = WM2ActionCode.UPDATE_WIFI_SSID.value
            if (res.cmdResultCode == SesameResultCode.success.value) {
                L.d("hcia", "設定帳號完畢:" + res.payload.toHexString())
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            } else {
                L.d("hcia", "設定wifi錯誤:" + res.cmdResultCode.toString())
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", (res.cmdResultCode).toInt())))
            }
        }
    }


    override fun setWifiPassword(password: String, result: CHResult<CHEmpty>) {
        sendCommand(SesameOS3Payload(WM2ActionCode.UPDATE_WIFI_PASSWORD.value, password.toByteArray())) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) { //                L.d("hcia", "設定密碼完畢:" + String(res.payload))
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            }
        }
    }

    override fun connectWifi(result: CHResult<CHEmpty>) {
        val company = co.candyhouse.sesame.BuildConfig.API_GATEWAY_CLIENT_ID!!.replace(":", "").replace("-", "")
        //        L.d("hcia", "company.toByteArray():" + company.toByteArray().toHexString())
        val verification = company + ":" + deviceId.toString().uppercase().split('-').last()
        L.d("wm2", "verification: $verification")
        sendCommand(SesameOS3Payload(WM2ActionCode.CONNECT_WIFI.value, verification.toByteArray())) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            } else {
                result.invoke(Result.failure(CHError.NotfoundError.value))
            }
        }
    }

    override fun insertSesames(sesame: CHDevices, result: CHResult<CHEmpty>) {

        if (!(sesame is CHSesameLock)) {
            result.invoke(Result.failure(CHError.BleInvalidAction.value))
            return
        }
//        L.d("hcia", "送出鑰匙sesame.getKey():" + sesame.getKey())
        //        L.d("hcia", "sesame.getKey().toByteArray():" + sesame.getKey().toByteArray().size)//392
        val ssm = sesame as CHDeviceUtil
        val noHashUUID = ssm.sesame2KeyData!!.deviceUUID.replace("-", "")
        val b64k = noHashUUID.hexStringToByteArray().base64Encode().replace("=", "")
        val ssmIRData = b64k.toByteArray()

        val ssmPKData = if ((ssm.sesame2KeyData!!.deviceModel == "sesame_5")
                || (ssm.sesame2KeyData!!.deviceModel == "sesame_5_pro")
                || (ssm.sesame2KeyData!!.deviceModel == "sesame_5_us")

                || (ssm.sesame2KeyData!!.deviceModel == "bike_2"))
        {
            "41B6D190EBBC1E9FA49E62710D80092784E998649FCA150419D2C70C6573BCA4666481EA47FDD755BB0761AB95EF95C9BD24016D54B14606EB5835541E45F27E".hexStringToByteArray()
        } else {
            ssm.sesame2KeyData!!.sesame2PublicKey.hexStringToByteArray()
        }

//        val ssmPKData = ssm.sesame2KeyData!!.sesame2PublicKey.hexStringToByteArray()
//        val ss5_pubk = "41B6D190EBBC1E9FA49E62710D80092784E998649FCA150419D2C70C6573BCA4666481EA47FDD755BB0761AB95EF95C9BD24016D54B14606EB5835541E45F27E"

        val ssmSecKa = ssm.sesame2KeyData!!.secretKey.hexStringToByteArray()
        val ssmUUid = ssm.sesame2KeyData!!.deviceUUID.uppercase(Locale.getDefault()).toByteArray()
        val allKey = ssmIRData + ssmPKData + ssmSecKa + ssmUUid
//        L.d("hcia", "allKey:" + allKey.toHexString())

        sendCommand(SesameOS3Payload(WM2ActionCode.ADD_SESAME.value, allKey)) { res ->
            L.d("hcia", "ADD_SESAME:ok")
            if (res.cmdResultCode == SesameResultCode.success.value) {
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            }
        }

    }

    override fun removeSesame(sesameKeyTag: String, result: CHResult<CHEmpty>) {
        L.d("hcia", "removeSesame:" + sesameKeyTag)
        sendCommand(SesameOS3Payload(WM2ActionCode.DELETE_SESAME.value, sesameKeyTag.uppercase(getDefault()).toByteArray())) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            }
        }
    }


    override fun getVersionTag(result: CHResult<String>) {
        if (deviceStatus.value == CHDeviceLoginStatus.unlogined) {
            return
        }
        sendCommand(SesameOS3Payload(WM2ActionCode.VERSION_TAG.value, byteArrayOf())) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                val versionTag = String(res.payload)
                result.invoke(Result.success(CHResultState.CHResultStateBLE(versionTag)))
                CHAPIClientBiz.updateDeviceFirmwareVersion(deviceId.toString().uppercase(), versionTag) {}
            }
        }

    }

    override fun reset(result: CHResult<CHEmpty>) {
        L.d("hcia", "resetWifiModule2 deviceStatus:" + deviceStatus)
        if (deviceStatus.value == CHDeviceLoginStatus.unlogined) {
            result.invoke(Result.failure(CHError.BleInvalidAction.value))
        }

        sendCommand(SesameOS3Payload(WM2ActionCode.RESET_WM2.value, byteArrayOf())) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                dropKey(result)
            }
        }
    }

    override fun updateFirmware(onResponse: CHResult<BluetoothDevice>) {
        sendCommand(SesameOS3Payload(WM2ActionCode.OPEN_OTA_SERVER.value, byteArrayOf())) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                onResponse.invoke(Result.success(CHResultState.CHResultStateBLE(advertisement!!.device!!)))
            } else {
                onResponse.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
            }
        }
    }


    private fun onGattWM2Publish(receivePayload: SSM3PublishPayload) {

        //        L.d("hcia", "🩰 收到wm2推送 receivePayload.actionCode:" + receivePayload.actionCode)

        if (receivePayload.cmdItCode == WM2ActionCode.OPEN_OTA_SERVER.value) { //            L.d("hcia", "OTA!->" + receivePayload.payload.toHexString())
            (delegate as? CHWifiModule2Delegate)?.onOTAProgress(this, receivePayload.payload.first())
        }
        if (receivePayload.cmdItCode == WM2ActionCode.SESAME_KEYS.value) { //            L.d("hcia", "🩰  收到 wm2 推送過來ssm鑰匙:" + receivePayload.payload.toHexString())
            ssm2KeysMap.clear()

            val keyDatas = receivePayload.payload.divideArray(23)
            keyDatas.forEach { //                L.d("hcia", "it.toHexString():" + it.toHexString())
                val ss2_ir_22 = it.sliceArray(IntRange(0, 21))
                val lock_status = it[22] //                L.d("hcia", "🩰  ss2_ir_22:" + String(ss2_ir_22)) //                L.d("hcia", "🩰 收到鑰匙上鎖狀態 lock_status:" + lock_status) //                val ss2_pub_key_64 = it.sliceArray(IntRange(22, 85)) //                val ss2_secretKey_16 = it.sliceArray(IntRange(86, 101)) //                L.d("hcia", "🩰  ss2_pub_key_64:" + ss2_pub_key_64.toHexString()) //                L.d("hcia", "🩰  ss2_secretKey_16:" + ss2_secretKey_16.toHexString())
                try {
                    val ssmID = (String(ss2_ir_22) + "==").base64decodeHex().noHashtoUUID()
                    ssm2KeysMap.put(ssmID.toString(), lock_status.toString())
//                    L.d("hcia", "🩰  ssmID:" + ssmID)
                } catch (e: Exception) {
                    L.d("hcia", "🩰  e:" + e)
                }
            } //            L.d("hcia", "🩰  delegate wm2 推送過來鑰匙給ＵＩ:" + delegate)
            (delegate as? CHWifiModule2Delegate)?.onSSM2KeysChanged(this, ssm2KeysMap)

        }
        if (receivePayload.cmdItCode == WM2ActionCode.SCAN_WIFI_SSID.value) {
            val ssidRssi = bytesToShort(receivePayload.payload[0], receivePayload.payload[1])
            val ssidStr = String(receivePayload.payload.drop(2).toByteArray()) //            L.d("hcia", "[sdk] ssidRssi:" + ssidRssi + "[sdk] sdkssidStr:" + ssidStr)
            (delegate as? CHWifiModule2Delegate)?.onScanWifiSID(this, ssidStr, ssidRssi)
        }
        if (receivePayload.cmdItCode == WM2ActionCode.UPDATE_WIFI_SSID.value) {
            val ssidStr = String(receivePayload.payload) //            L.d("hcia", "🩰 1 pubilsh  當前wifi ID:" + ssidStr)
            mechSetting?.wifiSSID = ssidStr
            (delegate as? CHWifiModule2Delegate)?.onAPSettingChanged(this, mechSetting!!)
        }
        if (receivePayload.cmdItCode == WM2ActionCode.UPDATE_WIFI_PASSWORD.value) {
            val passStr = String(receivePayload.payload) //            L.d("hcia", "🩰 2 pubilsh 當前wifi 密碼:" + passStr)
            mechSetting?.wifiPassWord = passStr
            (delegate as? CHWifiModule2Delegate)?.onAPSettingChanged(this, mechSetting!!)
        }

        if (receivePayload.cmdItCode == WM2ActionCode.NETWORK_STATUS.value) { //            L.d("hcia", "🩰 3 收到網路狀態" + receivePayload.payload.toHexString() + " byte:" + receivePayload.payload[0])
            val isAp: Boolean = (receivePayload.payload[0] and 2) > 0
            val isNEt: Boolean = (receivePayload.payload[0] and 4) > 0
            val isIot: Boolean = (receivePayload.payload[0] and 8) > 0
            val isAPCheck: Boolean = (receivePayload.payload[0] and 16) > 0
            val isAPConnecting: Boolean = (receivePayload.payload[0] and 32) > 0
            val isNETConnecting: Boolean = (receivePayload.payload[0] and 64) > 0
            val isIOTConnecting: Boolean = receivePayload.payload[0] < 0
            mechStatus = CHWifiModule2NetWorkStatus(isAp, isNEt, isIot, isAPConnecting, isNETConnecting, isIOTConnecting, isAPCheck)

            //            L.d("hcia", "isRegistered:" + isRegistered)
            if (isRegistered) {
                if (isAPCheck) {
                    deviceStatus = if (isIot) CHDeviceStatus.IotConnected else CHDeviceStatus.IotDisconnected
                } else {
                    deviceStatus = CHDeviceStatus.WaitApConnect
                }
            }
        }
        if (receivePayload.cmdItCode == WM2ActionCode.INITIAL.value) {
            mSesameToken = receivePayload.payload //            L.d("hcia", "🩰 " + "藍芽連線後初始化 mSesameToken:" + mSesameToken?.toHexString())
            if (isRegistered) {
                login()
            } else { //                L.d("hcia", "🩰 " + "delegate:" + delegate)
                deviceStatus = CHDeviceStatus.ReadyToRegister
            }
        }
    }

}

internal object Wm2Chracs {
    val uuidService01 = UUID.fromString("1b7e8251-2877-41c3-b46e-cf057c562524")
    val writeChrac = UUID.fromString("aca0ef7c-eeaa-48ad-9508-19a6cef6b356")
    val receiveChr = UUID.fromString("8ac32d3f-5cb9-4d44-bec2-ee689169f626")
}

internal enum class WM2ActionCode(val value: UByte) {
    CODE_NON(0U), REGISTER_WM2(1U), LOGIN_WM2(2U), UPDATE_WIFI_SSID(3U), UPDATE_WIFI_PASSWORD(4U), CONNECT_WIFI(5U), NETWORK_STATUS(6U), DELETE_SESAME(7U), ADD_SESAME(8U), INITIAL(13U), CCCD(14U), SESAME_KEYS(16U), RESET_WM2(18U), SCAN_WIFI_SSID(19U), OPEN_OTA_SERVER(126U), VERSION_TAG(127U), ;
}

