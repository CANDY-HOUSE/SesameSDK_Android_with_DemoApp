# CHSesameTouchProDevice クラスのドキュメント
```svg
internal class CHSesameTouchProDevice : CHSesameOS3(), CHSesameTouchPro, CHDeviceUtil {

    ssm2KeysMap: MutableMap<String, ByteArray> = mutableMapOf()
    advertisement: CHadv? = null
    keyBoardPassCodeModeGet(result: CHResult<Byte>)
    keyBoardPassCodeModeSet(mode: Byte, result: CHResult<CHEmpty>)
    keyBoardPassCodeDelete(ID: String, result: CHResult<CHEmpty>)
    keyBoardPassCode(result: CHResult<CHEmpty>)
    keyBoardPassCodeChange(ID: String, name: String, result: CHResult<CHEmpty>)
    fingerPrintModeGet(result: CHResult<Byte>)
    fingerPrintModeSet(mode: Byte, result: CHResult<CHEmpty>)
    fingerPrintDelete(ID: String, result: CHResult<CHEmpty>)
    fingerPrints(result: CHResult<CHEmpty>)
    fingerPrintsChange(ID: String, name: String, result: CHResult<CHEmpty>)
    cardModeGet(result: CHResult<Byte>)
    cardModeSet(mode: Byte, result: CHResult<CHEmpty>)
    cardDelete(ID: String, result: CHResult<CHEmpty>)
    cardChange(ID: String, name: String, result: CHResult<CHEmpty>)
    cards(result: CHResult<CHEmpty>)
    login(token: String?)
    register(result: CHResult<CHEmpty>)
    insertSesame(sesame: CHDevices, result: CHResult<CHEmpty>)
    removeSesame(tag: String, result: CHResult<CHEmpty>)
    onGattSesamePublish(receivePayload: SSM3PublishPayload)
                                                                    
}                                                                    


```
`CHSesameTouchProDevice` クラスは `CHSesameOS3` クラスを継承し、`CHSesameTouchPro` および `CHDeviceUtil` インターフェースを実装したクラスです。これは主に Sesame Touch Pro デバイスを管理および制御するためのものです。

## 属性

- `ssm2KeysMap`：SSM2 キーを格納するマップ。
- `advertisement`：デバイスの広告データ。

## メソッド

- `keyBoardPassCodeModeGet(result: CHResult<Byte>)`：デバイスからパスコードモードを取得するメソッド。
- `keyBoardPassCodeModeSet(mode: Byte, result: CHResult<CHEmpty>)`：デバイスのパスコードモードを設定するメソッド。
- `keyBoardPassCodeDelete(ID: String, result: CHResult<CHEmpty>)`：デバイスからパスコードを削除するメソッド。
- `keyBoardPassCodeChange(ID: String, name: String, result: CHResult<CHEmpty>)`：デバイスのパスコードを変更するメソッド。
- `fingerPrintModeGet(result: CHResult<Byte>)`：デバイスから指紋モードを取得するメソッド。
- `fingerPrintModeSet(mode: Byte, result: CHResult<CHEmpty>)`：デバイスの指紋モードを設定するメソッド。
- `fingerPrintDelete(ID: String, result: CHResult<CHEmpty>)`：デバイスから指紋を削除するメソッド。
- `fingerPrintsChange(ID: String, name: String, result: CHResult<CHEmpty>)`：デバイスの指紋を変更するメソッド。
- `cardModeGet(result: CHResult<Byte>)`：デバイスからカードモードを取得するメソッド。
- `cardModeSet(mode: Byte, result: CHResult<CHEmpty>)`：デバイスのカードモードを設定するメソッド。
- `cardDelete(ID: String, result: CHResult<CHEmpty>)`：デバイスからカードを削除するメソッド。
- `cardChange(ID: String, name: String, result: CHResult<CHEmpty>)`：デバイスのカードを変更するメソッド。
- `login(token: String?)`：デバイスにログインするメソッド。
- `register(result: CHResult<CHEmpty>)`：デバイスを登録するメソッド。
- `insertSesame(sesame: CHDevices, result: CHResult<CHEmpty>)`：Sesame デバイスを挿入するメソッド。
- `removeSesame(tag: String, result: CHResult<CHEmpty>)`：Sesame デバイスを削除するメソッド。
- `onGattSesamePublish(receivePayload: SSM3PublishPayload)`：GATT Sesame パブリッシュを処理するメソッド。

## 継承

- `CHSesameOS3`
- `CHSesameTouchPro`
- `CHDeviceUtil`

![CHSesameTouchProDevice](CHSesameTouchProDevice.svg)
