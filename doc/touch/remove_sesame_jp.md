# Remove Sesame の説明
アプリはSesameデバイスを削除するために103コマンドを送信します。

### 送信形式

|  バイト  |          16~1 |       0 |
|:------:|--------------:|--------:|
| データ   |  device_uuid	 | command |

- command: 103コマンド (固定)
- device_uuid: デバイスの一意の識別UUID

### 受信形式

| バイト  |    2 |   1   |     0      |  
|:---:|:----:|:----:|:-----:|
| データ |  status  | command |response   |
- command: 103コマンド (固定)
- response: 0x07応答 (固定)
- status: 状態 0x00 (成功)

### シーケンス図
![icon](remove_sesame.svg)

### Androidの例

```java
@Override
public void removeSesame(String tag, CHResult<CHEmpty> result) {
    if (checkBle(result)) return;
    if (ssm2KeysMap.get(tag).get(0).toInt() == 0x04) {
        // SSM4.0の場合
        String noDashUUID = tag.replace("-", "");
        String b64k = Base64.getEncoder().encodeToString(Hex.decode(noDashUUID)).replace("=", "");
        byte[] ssmIRData = b64k.getBytes(StandardCharsets.UTF_8);
        sendCommand(new SesameOS3Payload(SesameItemCode.REMOVE_SESAME.value, ssmIRData), ssm2ResponsePayload -> {
            result.invoke(Result.success(new CHResultState.CHResultStateBLE(new CHEmpty())));
        });
    } else {
        // SSM5.0の場合
        String noDashUUID = tag.replace("-", "");
        sendCommand(new SesameOS3Payload(SesameItemCode.REMOVE_SESAME.value, Hex.decode(noDashUUID)), ssm2ResponsePayload -> {
            result.invoke(Result.success(new CHResultState.CHResultStateBLE(new CHEmpty())));
        });
    }
}
```