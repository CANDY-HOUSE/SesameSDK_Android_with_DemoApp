# BLEデータ受信

### onCharacteristicChanged によるBLEデータ受信
```svg

  override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            val ssmSay = gattRxBuffer.feed(characteristic.value)
            val c=ssmSay?.first?:"null"
            L.l("onCharacteristicChanged",c.toString(),byToString(characteristic.value))
            if (ssmSay?.first == DeviceSegmentType.plain) {
                parseNotifyPayload(ssmSay.second)
            } else if (ssmSay?.first == DeviceSegmentType.cipher) {
                parseNotifyPayload(cipher!!.decrypt(ssmSay.second))
            }

        }
```
### SesameBleReceiverオブジェクトは、データコンテナで、データ転送が完了した場合に暗号化タイプとバイト配列を返します。
- buffer: データを保持するバッファ
- segmentFlag: データが最初のものであるか、唯一のものであるかを判断する
- parsingType: データが終了したかを判断する
```svg
 internal class SesameBleReceiver {
        var buffer = byteArrayOf()
        internal fun feed(input: ByteArray): Pair<DeviceSegmentType, ByteArray>? {
        val segmentFlag = input[0]
        val isStartFlag = segmentFlag.toInt() and 1
        val parsingType = segmentFlag.toInt() shr 1
        //        L.d("hcia", "isStartFlag:" + isStartFlag)
        //        L.d("hcia", "parsingType:" + parsingType)
        if (isStartFlag > 0) {
        buffer = input.drop(1).toByteArray()
        } else {
        buffer += input.drop(1).toByteArray()
        }
        if (parsingType > 0) {
        val buf = buffer
        buffer = byteArrayOf()
        val type = DeviceSegmentType.getByValue(parsingType)
        return Pair(type, buf)
        } else {
        return null
        }
        }
        }

```
### parseNotifyPayload関数は、データが応答データであるか、BLEからのプッシュデータであるかを判断します。
- palntext: 復号化されたデータ

```svg

  private fun parseNotifyPayload(palntext: ByteArray) {
            L.l("parse data  rx",byToString(palntext))
            val ssm2notify = SesameNotifypayload(palntext)
//            L.d("hcia", "[ss5] ssm2notify.notifyOpCode:" + ssm2notify.notifyOpCode)
            if (ssm2notify.notifyOpCode == SSM2OpCode.response) {
                onGattSesameResponse(SSM3ResponsePayload(ssm2notify.payload))
            } else if (ssm2notify.notifyOpCode == SSM2OpCode.publish) {
                onGattSesamePublish(SSM3PublishPayload(ssm2notify.payload))
            }
        }
```
### onGattSesameResponse Bleデータ受信処理

- cmdCallBack: 送信データの応答と受信データの処理を行うためのマップオブジェクト

```
   private fun onGattSesameResponse(ssm2ResponsePayload: SSM3ResponsePayload) {
            cmdCallBack.get(ssm2ResponsePayload.cmdItCode)?.invoke(ssm2ResponsePayload)
            cmdCallBack.remove(ssm2ResponsePayload.cmdItCode)
//            L.d("hcia", "[ss5] 🀄Command: <==:" + (ssm2ResponsePayload.cmdItCode) + " " + (ssm2ResponsePayload.cmdResultCode))
        }
```
### ループ図

![send data](data_receive.svg)