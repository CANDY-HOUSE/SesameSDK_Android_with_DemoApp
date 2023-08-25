#  scanWifiSSID 讲解
发送指令19wifi module 2 执行扫描WIFI
### 发送格式

|  Byte  |        0 |
|:------:|---------:|
| Data   |  command |

- command:指令19(固定)




### 接收格式

| Byte  |    2 |   1   |     0      |
|:---:|:----:|:----:|:-----:|
| Data |  status  | command |response   |
- command:指令19(固定)
- response:响应0x07(固定)
- status:状态0x00(成功)


### 循序图
![icon](scanwifissid.svg)





### android示例
``` java
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
```
