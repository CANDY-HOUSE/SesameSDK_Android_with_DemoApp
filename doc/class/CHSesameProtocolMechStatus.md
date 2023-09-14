# CHSesameProtocolMechStatus インターフェイス
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
`CHSesameProtocolMechStatus` はデバイスの機械状態プロトコルのインターフェースで、デバイスの機械状態情報を取得および処理します。

## プロパティ

- `position: Short` - デバイスの位置を取得します。デフォルト値は0です。
- `target: Short?` - デバイスの目標位置を取得します。デフォルト値は0です。
- `isBatteryCritical: Boolean` - デバイスのバッテリーが臨界状態にあるかどうかを判断します。デフォルト値はfalseです。
- `isInLockRange: Boolean` - デバイスがロック範囲内にあるかどうかを判断します。デフォルト値はfalseです。
- `isInUnlockRange: Boolean` - デバイスがロック範囲内にあるかどうかを判断します。デフォルト値はロック範囲外です。
- `isStop: Boolean?` - デバイスが停止しているかどうかを判断します。デフォルト値はnullです。
- `data: ByteArray` - デバイスのデータを取得します。

## メソッド

- `fun getBatteryVoltage(): Float` - デバイスのバッテリー電圧を取得します。
- `fun getBatteryPrecentage(): Int` - デバイスのバッテリー電圧に基づいて、電池の残量をパーセントで取得します。


