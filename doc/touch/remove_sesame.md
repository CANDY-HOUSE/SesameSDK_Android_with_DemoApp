# Remove Sesame 讲解
app发送103指令，删除Sesame
### 发送格式

|  Byte  |          16~1 |       0 |
|:------:|--------------:|--------:|
| Data   |  device_uuid	 | command |

- command:指令103(固定)
- device_uuid:设备唯一标识UUID


### 接收格式

| Byte  |    2 |   1   |     0      |  
|:---:|:----:|:----:|:-----:|
| Data |  status  | command |response   |
- command:指令103(固定)
- response:响应0x07(固定)
- status:状态0x00(成功)  


### 循序图
![icon](remove_sesame.svg)





### android示例
``` java
  override fun removeSesame(tag: String, result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        if (ssm2KeysMap.get(tag)!!.get(0).toInt() == 0x04) {// ss4
            val noDashUUID = tag.replace("-", "")
            val b64k = noDashUUID.hexStringToByteArray().base64Encode().replace("=", "")
            val ssmIRData = b64k.toByteArray()
            sendCommand(SesameOS3Payload(SesameItemCode.REMOVE_SESAME.value, ssmIRData)) { ssm2ResponsePayload ->
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            }
        } else {//ss5
            val noDashUUID = tag.replace("-", "")
            sendCommand(SesameOS3Payload(SesameItemCode.REMOVE_SESAME.value, noDashUUID.hexStringToByteArray())) { ssm2ResponsePayload ->
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            }
        }
    }
```
