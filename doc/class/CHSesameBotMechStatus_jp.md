# CHSesameBotMechStatus クラスのドキュメント
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
`CHSesameBotMechStatus` クラスは、Sesame Bot の機械状態を処理および管理するためのクラスです。

## プロパティ

- `data`：有効なペイロードデータを格納するバイト配列（ByteArray）です。
- `battery`：電池の状態を表す短い整数（Short）です。バイトをショートに変換して取得します。
- `motorStatus`：モーターの状態を表すバイト（Byte）で、`data` 配列の5番目の要素から取得されます。
- `flags`：フラグを表す整数（Int）で、`data` 配列の8番目の要素を整数に変換して取得します。
- `isInLockRange`：ロック範囲内にあるかどうかを表すブール値（Boolean）で、2とのビット論理積演算を行って判断します。
- `isInUnlockRange`：アンロック範囲内にあるかどうかを表すブール値（Boolean）で、4とのビット論理積演算を行って判断します。
- `isBatteryCritical`：電池がクリティカルな状態にあるかどうかを表すブール値（Boolean）で、32とのビット論理積演算を行って判断します。
- `isStop`：停止しているかどうかを表すブール値（Boolean）で、1とのビット論理積演算を行って0かどうかを判断します。

## メソッド

- `getBatteryVoltage()`：このメソッドは、電池の電圧を取得するためのもので、電池の状態を元に計算された電圧値を Float 型で返します。電圧は電池の状態に7.2を掛けて1023で割った値です。

## 継承

- `CHSesameProtocolMechStatus`：`CHSesameBotMechStatus` クラスは `CHSesameProtocolMechStatus` クラスを継承しており、親クラスの `data` プロパティと `getBatteryVoltage()` メソッドを実装する必要があります。
