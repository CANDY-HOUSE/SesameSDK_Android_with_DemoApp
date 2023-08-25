#  setWifiPassword 讲解

### 发送格式

|  Byte  |      N~1 |       0 |
|:------:|---------:|--------:|
| Data   | ssid_pw	 | command |

- command:指令4(固定)
- card_id:卡片ID



### 接收格式

| Byte  |    2 |   1   |     0      |
|:---:|:----:|:----:|:-----:|
| Data |  status  | command |response   |
- command:指令4(固定)
- response:响应0x07(固定)
- status:状态0x00(成功)


### 循序图
![icon](scanwifissid.svg)





### android示例
``` java
       override fun setWifiPassword(password: String, result: CHResult<CHEmpty>) {
        sendCommand(SesameOS3Payload(WM2ActionCode.UPDATE_WIFI_PASSWORD.value, password.toByteArray())) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) { //                L.d("hcia", "設定密碼完畢:" + String(res.payload))
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            }
        }
    }
```
