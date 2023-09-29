# SesameBleReceiver クラス

```svg


internal class SesameBleReceiver {
    var buffer = byteArrayOf()
    internal fun feed(input: ByteArray): Pair<DeviceSegmentType, ByteArray>? 
   
}
```



`SesameBleReceiver` は内部のクラスです。

## プロパティ

- `buffer`：受信したBluetoothデータを保存するために使用されるバイト配列です。

## メソッド

- `feed(input: ByteArray): Pair<DeviceSegmentType, ByteArray>?`：このメソッドは、バイト配列を受取り、`DeviceSegmentType`とバイト配列を含むペアの二重のタプルを返します。処理が失敗した場合は、nullを返します。
