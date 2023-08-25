# Card Delete の説明

### 送信形式


|  Byte  |     16~1 |       0 |
|:------:|---------:|--------:|
| Data   | card_id	 | command |

- command:108コマンド (固定)
- card_id:カードID



### 受信形式


| Byte  |    2 |   1   |     0      |
|:---:|:----:|:----:|:-----:|
| Data |  status  | command |response   |
- command:108コマンド (固定)
- response:0x07応答 (固定)
- status:状态0x00(成功)  


### シーケンス図

![icon](card_delete.svg)





### Androidの例

``` java
    override fun cardDelete(ID: String, result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_CARD_DELETE.value, ID.hexStringToByteArray())) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }
```
