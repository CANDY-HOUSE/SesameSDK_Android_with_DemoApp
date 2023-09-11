# CHSesameTouchProDelegate インターフェース
```svg


interface CHSesameTouchProDelegate : CHDeviceStatusDelegate {
    fun onSSM2KeysChanged(device: CHSesameConnector, ssm2keys: Map<String, ByteArray>) {}

    fun onKeyBoardReceive(device: CHSesameConnector, ID: String, name: String, type: Byte) {}
    fun onKeyBoardChanged(device: CHSesameConnector, ID: String, name: String, type: Byte) {}
    fun onKeyBoardReceiveEnd(device: CHSesameConnector) {}
    fun onKeyBoardReceiveStart(device: CHSesameConnector) {}

    fun onCardReceive(device: CHSesameConnector, ID: String, name: String, type: Byte) {}
    fun onCardChanged(device: CHSesameConnector, ID: String, name: String, type: Byte) {}
    fun onCardReceiveEnd(device: CHSesameConnector) {}
    fun onCardReceiveStart(device: CHSesameConnector) {}

    fun onFingerPrintReceive(device: CHSesameConnector, ID: String, name: String, type: Byte) {}
    fun onFingerPrintChanged(device: CHSesameConnector, ID: String, name: String, type: Byte) {}
    fun onFingerPrintReceiveEnd(device: CHSesameConnector) {}
    fun onFingerPrintReceiveStart(device: CHSesameConnector) {}
}
```
 `CHSesameTouchProDelegate`は、デバイスの状態を扱うためのDelegateインターフェースであり、`CHDeviceStatusDelegate`インターフェースを継承しています。デバイスの状態の変化に加えて、Sesame Touch Proデバイスに対する操作も含まれています。


## メソッド

- `fun onSSM2KeysChanged(device: CHSesameConnector, ssm2keys: Map<String, ByteArray>) {}` - Sesame2のキーが変更された時に呼び出されます。
- `fun onKeyBoardReceive(device: CHSesameConnector, ID: String, name: String, type: Byte) {}` - キーボードの入力を受け取った時に呼び出されます。
- `fun onKeyBoardChanged(device: CHSesameConnector, ID: String, name: String, type: Byte) {}` - キーボードの状態が変化した時に呼び出されます。
- `fun onKeyBoardReceiveEnd(device: CHSesameConnector) {}` - キーボードの入力が受け終わった時に呼び出されます。
- `fun onKeyBoardReceiveStart(device: CHSesameConnector) {}` - キーボードの入力が受け始まる時に呼び出されます。
- `fun onCardReceive(device: CHSesameConnector, ID: String, name: String, type: Byte) {}` - カードの情報を受信した時に呼び出されます。
- `fun onCardChanged(device: CHSesameConnector, ID: String, name: String, type: Byte) {}` - カードの状態が変化した時に呼び出されます。
- `fun onCardReceiveEnd(device: CHSesameConnector) {}` - カードの受信が終了した時に呼び出されます。
- `fun onCardReceiveStart(device: CHSesameConnector) {}` - カード情報の受信を開始する時に呼び出されます。
- `fun onFingerPrintReceive(device: CHSesameConnector, ID: String, name: String, type: Byte) {}` - 指紋情報を受け取った時に呼び出されます。
- `fun onFingerPrintChanged(device: CHSesameConnector, ID: String, name: String, type: Byte) {}` - 指紋の状態が変化した時に呼び出されます。
- `fun onFingerPrintReceiveEnd(device: CHSesameConnector) {}` - 指紋情報の受信が終了した時に呼び出されます。
- `fun onFingerPrintReceiveStart(device: CHSesameConnector) {}` - 指紋情報の受信を開始する時に呼び出されます。

