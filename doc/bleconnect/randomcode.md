# RandomCode 获取随机Code讲解
手机和Ble设备建立连接后并开启nofity，Ble设备回传 4Bytes random code
#### 手机连接连接设备后开启notify,设备会回传4位随机码

## 接收格式
| Byte |    5~2     | 1 | 0 |
|:----:|:----------:|:----:|:----:|
| Data | randomcode | command | response  |
- command:指令14(固定)
- response:响应0x08(固定)
- randomcode:随机code

## 循序图
![v](randomcode.svg)





## android示例
``` java
    override fun onGattSesamePublish(receivePayload: SSM3PublishPayload) {
        if (receivePayload.cmdItCode == SesameItemCode.initial.value) {
            mSesameToken = receivePayload.payload
            L.d("hcia", "isNeedAuthFromServer:" + isNeedAuthFromServer)
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

```
