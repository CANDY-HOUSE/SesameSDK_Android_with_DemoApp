# Ble receive data
### onCharacteristicChanged 接收Ble数据
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
### SesameBleReceiver对象 接收数据容器，当数据传输完整返回数据加密类型和字节数组
- buffer:存放数据
- segmentFlag:判断数据是第一条或仅此一条
- parsingType:判断数据是结束
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
### parseNotifyPayload 函数主要判断数据是响应数据还是ble推送过来数据
- palntext:解密后数据

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
### onGattSesameResponse Ble数据接收处理
- cmdCallBack:map对象，用来处理发送数据响应接收数据处理
```
   private fun onGattSesameResponse(ssm2ResponsePayload: SSM3ResponsePayload) {
            cmdCallBack.get(ssm2ResponsePayload.cmdItCode)?.invoke(ssm2ResponsePayload)
            cmdCallBack.remove(ssm2ResponsePayload.cmdItCode)
//            L.d("hcia", "[ss5] 🀄Command: <==:" + (ssm2ResponsePayload.cmdItCode) + " " + (ssm2ResponsePayload.cmdResultCode))
        }
```
### 循环图
![send data](data_receive.svg)