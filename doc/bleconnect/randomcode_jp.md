# RandomCode の取得に関する説明

####  スマートフォンと BLE デバイスとの接続を確立し、通知を有効にすると、BLE デバイスから 4 バイトのランダムコードが返されます


## 受信形式

| Byte |    5~2     | 1 | 0 |
|:----:|:----------:|:----:|:----:|
| Data | randomcode | command | response  |
- command:指令14(固定)
- response:响应0x08(固定)
- randomcode:ランダムコード

## シーケンス図

![v](randomcode.svg)





## Androidの例

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
