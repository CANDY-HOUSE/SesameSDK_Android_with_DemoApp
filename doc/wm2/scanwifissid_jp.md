# scanWifiSSID 説明
指令19を送信して、WiFiモジュール2に対してWiFiのスキャンを実行します。

### 送信フォーマット

|  バイト  |        0 |
|:------:|---------:|
| データ   |  コマンド |

- コマンド: 指令19（固定）

### 受信フォーマット
| バイト  |    2 |   1   |     0      |
|:---:|:----:|:----:|:-----:|
| データ | ステータス  | コマンド |レスポンス   |
- コマンド: 指令19（固定）
- レスポンス: 応答0x07（固定）
- ステータス: 状態0x00（成功）

### シーケンス図
![アイコン](scanwifissid.svg)

### Androidの例
```java
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
