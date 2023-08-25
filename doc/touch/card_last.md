# Card last 讲解

### 发送格式

|  Byte  |        0 |
|:------:|:----:|
| Data   |	 | command |

- command:指令111(固定)


### 接收格式

| Byte  |    2 |   1   |     0      |
|:---:|:----:|:----:|:-----:|
| Data |  status  | command |response   |
- command:指令111(固定)
- response:响应0x07(固定)
- status:状态0x00(成功)  


### 循序图






### android示例
``` java
  override fun onGattSesamePublish(receivePayload: SSM3PublishPayload) {
        super.onGattSesamePublish(receivePayload)
      if (receivePayload.cmdItCode == SesameItemCode.SSM_OS3_CARD_LAST.value) {
            (delegate as? CHSesameTouchProDelegate)?.onCardReceiveEnd(this)
        }

    }
```
