# Finger Delete 説明
アプリが116コマンドを送信して、指定したIDの指紋を削除します。

### 送信フォーマット

|  バイト  | 1|    0     |
|:------:|---:|:--------:|
| データ   | 指紋ID|  コマンド |

- コマンド: 指令116（固定）
- 指紋ID: 削除する指紋のID

### 受信フォーマット
| バイト  |        2   |     1     |     0      |
|:---:|:-----------:|:----:|:---------:|
| データ |  ステータス | コマンド |レスポンス   |
- コマンド: 指令116（固定）
- レスポンス: 応答0x07（固定）
  - ステータス: 0x00（成功）

### シーケンス図
![アイコン](finger_delete.svg)

### Androidの例
```java
 override fun fingerPrintDelete(ID: String, result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_FINGERPRINT_DELETE.value, ID.hexStringToByteArray())) {
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }
