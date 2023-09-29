
# CHSesameBotMechStatus クラス
```svg
class CHSesameBotMechStatus(override val data: ByteArray) : CHSesameProtocolMechStatus {
    private val battery = bytesToShort(data[0], data[1])
    internal val motorStatus = data[4]
    private val flags = data[7].toInt()
    override var isInLockRange: Boolean = flags and 2 > 0
    override var isInUnlockRange: Boolean = flags and 4 > 0
    override var isBatteryCritical: Boolean = flags and 32 > 0
    override var isStop: Boolean? = (flags and 1 == 0)
    override fun getBatteryVoltage(): Float 
}


```
`CHSesameBotMechStatus`クラスはSesame Botの機械状態を処理と管理します。

## プロパティ

- `data`：バイトの配列を表します。ByteArray型で、有効なペイロードデータを保存します。
- `battery`：バイトを短整数に変換することで取得するバッテリーの状態です。
- `motorStatus`：`data`配列の5番目の要素から取得するモーターの状態です。
- `flags`：`data`配列の8番目の要素を整数に変換することで取得するフラグです。
- `isInLockRange`：Boolean型の変数で、2とのビットAND演算によって、ロック範囲内にあるかどうかを判断します。
- `isInUnlockRange`：Boolean型の変数で、4とのビットAND演算によって、解錠範囲内にあるかどうかを判断します。
- `isBatteryCritical`：Boolean型の変数で、32とのビットAND演算によって、バッテリーが深刻な状態かどうかを判断します。
- `isStop`：Boolean型の変数で、1とのビットAND演算によって、結果が0であることで停止しているかどうかを判断します。

## メソッド

- `getBatteryVoltage()`：バッテリー電圧を取得し、Float型の電圧値を返します。電圧は、バッテリーの状態の値を7.2で乗算し、1023で除算することで計算されます。

## 継承

- `CHSesameProtocolMechStatus`：`CHSesameBotMechStatus`クラスは`CHSesameProtocolMechStatus`クラスを継承し、親クラスの`data`というプロパティと`getBatteryVoltage()`というメソッドを実装します。
