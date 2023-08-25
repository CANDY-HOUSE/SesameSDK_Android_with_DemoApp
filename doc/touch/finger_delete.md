# Finger Delete 讲解
app 发送发送116指令，删除ID指纹
### 发送格式

|  Byte  | 1|    0     |
|:------:|---:|:--------:|
| Data   | finger_id|  command |

- command:指令116(固定)
- finger_id:指纹ID




### 接收格式

| Byte  |        2   |     1     |     0      |
|:---:|:-----------:|:----:|:---------:|
| Data |  status | command |response   |
- command:指令116(固定)
- response:响应0x07(固定)
    - status:0x00(成功)




### 循序图
![icon](finger_delete.svg)





### android示例
``` java
 override fun fingerPrintDelete(ID: String, result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_FINGERPRINT_DELETE.value, ID.hexStringToByteArray())) {
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }
```
