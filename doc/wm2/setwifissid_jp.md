# setWifiSSID 説明

### 送信フォーマット

|  バイト  |        N~1 |       0 |
|:------:|-----------:|--------:|
| データ   | SSIDの名前	 | コマンド |

- コマンド: 指令3（固定）
- SSIDの名前: SSIDの名前

### 受信フォーマット
| バイト  |    2 |   1   |     0      |
|:---:|:----:|:----:|:-----:|
| データ | ステータス  | コマンド |レスポンス   |
- コマンド: 指令3（固定）
- レスポンス: 応答0x07（固定）
- ステータス: 状態0x00（成功）

### シーケンス図
![アイコン](scanwifissid.svg)

### Androidの例
```java
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
