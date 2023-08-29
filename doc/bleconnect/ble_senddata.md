# Ble send data 
### sendCommand関数は、BLEデータ転送をカプセル化します。
- payload:伝える情報の準備
- isEncryt:暗号化検査
- onResponse:コールバック応答
```svg
fun sendCommand(payload: SesameOS3Payload, isEncryt: DeviceSegmentType = DeviceSegmentType.cipher, onResponse: SesameOS3ResponseCallback) {
        val tmp = cmdCallBack[payload.itemCode]
        cmdCallBack[payload.itemCode] = onResponse
        if (tmp != null) {
              return
        }
        val  isChipher=if (isEncryt == DeviceSegmentType.cipher) "cipher" else "no cipher"
        L.l("parse data send",payload.itemCode.toString(),isChipher,byToString(payload.toDataWithHeader()),payload.toDataWithHeader().toHexString())
        CoroutineScope(IO).launch {
            semaphore.acquire()
            val say2ssm = if (isEncryt == DeviceSegmentType.cipher) {
                cipher?.encrypt(payload.toDataWithHeader())
            } else {
                payload.toDataWithHeader()
            }
            gattTxBuffer = SesameBleTransmit(isEncryt, say2ssm!!)
            transmit()
        }

```
### DeviceSegmentType の列挙を使用して、伝送されたデータが暗号化されているかどうかを判断します。
-  plain(1) : データは暗号化されていない
-  cipher(2)  : データは暗号化されている
### SesameBleTransmit関数は主にbytesのデータ分割を行い、最大転送バイト数は20です。
- type:DeviceSegmentTypeタイプ
- input:BLEデバイスにバイト配列を送信する
- isStart:データが完全に転送されたかどうかをマークし、完了した場合は-1を返す
```コード例
  
internal class SesameBleTransmit(var type: DeviceSegmentType, var input: ByteArray) {
    var isStart = 1
    internal fun getChunk(): ByteArray? {
        if (isStart == -1) {

            return null
        } else if (input.size <= 19) {
            val segmentHeader = ((type.value shl 1) or isStart).toByte()
            isStart = -1
            return byteArrayOf(segmentHeader) + input
        } else {
            val payload = input.copyOf(19)
            val segmentHeader = isStart.toByte()
            input = input.drop(19).toByteArray()
            isStart = 0
            return byteArrayOf(segmentHeader) + payload
        }
    }
}

```
### 関数transmitは、最大20バイト数のBLEデータを書込みます。
- semaphore:並行アクセスを制限する
```svg
  fun transmit() {
        mCharacteristic?.value = gattTxBuffer?.getChunk()
        if (mCharacteristic?.value == null) {
            semaphore.release()
            return
        }
        val check = mBluetoothGatt?.writeCharacteristic(mCharacteristic)
//        L.d("hcia", "[ss5][app][say]:" + mCharacteristic?.value?.toHexString() + " check:" + check)
        if (check == false) {
            semaphore.release()
            disconnect { }
        }
    }
```
### フローチャート
![send data](data_send.svg)