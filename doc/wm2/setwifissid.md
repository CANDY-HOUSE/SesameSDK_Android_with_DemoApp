#  setWifiSSID 讲解

### 发送格式

|  Byte  |        N~1 |       0 |
|:------:|-----------:|--------:|
| Data   | ssid_name	 | command |

- command:指令3(固定)
- card_id:卡片ID



### 接收格式

| Byte  |    2 |   1   |     0      |
|:---:|:----:|:----:|:-----:|
| Data |  status  | command |response   |
- command:指令3(固定)
- response:响应0x07(固定)
- status:状态0x00(成功)


### 循序图
![icon](scanwifissid.svg)





### android示例
``` java
       override fun setWifiSSID(ssid: String, result: CHResult<CHEmpty>) {
        sendCommand(SesameOS3Payload(WM2ActionCode.UPDATE_WIFI_SSID.value, ssid.toByteArray())) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) { //                L.d("hcia", "設定帳號完畢:" + String(res.payload))
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            } else {
                L.d("hcia", "設定wifi錯誤:" + res.cmdResultCode.toString())
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", (res.cmdResultCode).toInt())))
            }
        }
    }
```
