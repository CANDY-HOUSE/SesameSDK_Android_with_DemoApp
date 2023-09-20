# Card Mode Get 説明
携帯電話で命令113を送信し、カードの新規モードまたは認証モードを取得します。
### 送信フォーマット

|  Byte  |       0 |
|:------:|-------:|
| Data   |  command |

- command:命令113(固定)




### 受信フォーマット

| Byte  |     3    |   2 |   1   |     0      |
|:---:|:-------:|:-----:|:----:|:-----:|
| Data | card_mode|  status | command |response   |
- command:命令113(固定)
- response:応答0x07(固定)
- status:0x00(成功)
- card_mode:0x00->認証モード，0x01->新規モード


### フローチャート
![icon](card_model_get.svg)





### android例
``` java
 override fun cards(result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_CARD_GET.value, byteArrayOf())) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }
```
