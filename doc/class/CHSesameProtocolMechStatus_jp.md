# CHSesameProtocolMechStatus インターフェースのドキュメント

```svg


interface CHSesameProtocolMechStatus {

    val position: Short
       
    val target: Short?
      
    val isBatteryCritical: Boolean
        
    val isInLockRange: Boolean
   
    val isInUnlockRange: Boolean
   
    val isStop: Boolean?
     
    val data: ByteArray

    fun getBatteryVoltage(): Float

    fun getBatteryPrecentage(): Int 

}
```

`CHSesameProtocolMechStatus` は、デバイスの機械状態プロトコルのインターフェースです。デバイスの機械状態情報を取得および処理するために使用されます。

## 属性

- `position: Short` - デバイスの位置を取得します。デフォルト値は0です。
- `target: Short?` - デバイスの目標位置を取得します。デフォルト値は0です。
- `isBatteryCritical: Boolean` - デバイスのバッテリーが臨界状態にあるかどうかを判断します。デフォルト値はfalseです。
- `isInLockRange: Boolean` - デバイスがロック範囲内にあるかどうかを判断します。デフォルト値はfalseです。
- `isInUnlockRange: Boolean` - デバイスがアンロック範囲内にあるかどうかを判断します。デフォルト値はデバイスがロック範囲外にあることを示します。
- `isStop: Boolean?` - デバイスが停止しているかどうかを判断します。デフォルト値はnullです。
- `data: ByteArray` - デバイスのデータを取得します。

## メソッド

- `fun getBatteryVoltage(): Float` - デバイスのバッテリー電圧を取得します。
- `fun getBatteryPrecentage(): Int` - デバイスのバッテリー残量のパーセンテージを取得します。このメソッドは電圧値に基づいてバッテリー残量を計算します。
