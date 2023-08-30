# timestamp タイムスタンプを更新します

### 送信フォーマット

|  Byte  | 4~1|       0 |  
|:------:|:----:|--------:|
| Data   | timestamp| command |

- command:命令8(固定)
- timestamp:現時点のスマートフォンのタイムスタンプ


### 受信フォーマット

| Byte  |    2    |   1   |     0      |  
|:---:|:-------:|:-----:|:---------:|
| Data | status  | command |response   |
- command:命令8(固定)
- response:応答0x07(固定)
- status:状態0x00(成功)

### フローチャート
![icon](timestamp.svg)





### android例
``` java
        override fun updateTime(result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.time.value, System.currentTimeMillis().toUInt32ByteArray()), DeviceSegmentType.cipher) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }
```
