# Card Delete 讲解

### 发送格式

|  Byte  |     16~1 |       0 |
|:------:|---------:|--------:|
| Data   | card_id	 | command |

- command:指令108(固定)
- card_id:卡片ID



### 接收格式

| Byte  |    2 |   1   |     0      |
|:---:|:----:|:----:|:-----:|
| Data |  status  | command |response   |
- command:指令108(固定)
- response:响应0x07(固定)
- status:状态0x00(成功)  


### 循序图
![icon](card_delete.svg)





### android示例
``` java
    override fun cardDelete(ID: String, result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_CARD_DELETE.value, ID.hexStringToByteArray())) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }
```
