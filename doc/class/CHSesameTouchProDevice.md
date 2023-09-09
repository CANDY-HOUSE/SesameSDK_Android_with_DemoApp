



# CHSesameTouchProDevice

## プロパティ
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
- `ssm2KeysMap`：SSM2 のキーの保存
- `advertisement`：デバイスのadvertisement

## メソッド

- `keyBoardPassCodeModeGet(result: CHResult<Byte>)`：デバイスからパスワードモードを取得します。
- `keyBoardPassCodeModeSet(mode: Byte, result: CHResult<CHEmpty>)`：デバイスのパスワードモードを設置します。
- `keyBoardPassCodeDelete(ID: String, result: CHResult<CHEmpty>)`：デバイスからパスワードを取得します。
- `keyBoardPassCodeChange(ID: String, name: String, result: CHResult<CHEmpty>)`：デバイスのパスワードを変更します。
- `fingerPrintModeGet(result: CHResult<Byte>)`：デバイスから指紋モードを取得します。
- `fingerPrintModeSet(mode: Byte, result: CHResult<CHEmpty>)`：デバイスの指紋モードを設置します。
- `fingerPrintDelete(ID: String, result: CHResult<CHEmpty>)`：デバイスから指紋モードを削除します。
- `fingerPrintsChange(ID: String, name: String, result: CHResult<CHEmpty>)`：デバイスの指紋を変更します。
- `cardModeGet(result: CHResult<Byte>)`：デバイスからカードモードを取得します。
- `cardModeSet(mode: Byte, result: CHResult<CHEmpty>)`：デバイスのカードモード設置します。
- `cardDelete(ID: String, result: CHResult<CHEmpty>)`：デバイスからカードを削除します。
- `cardChange(ID: String, name: String, result: CHResult<CHEmpty>)`：デバイスのカードを変更します。
- `login(token: String?)`：デバイスにログインします。
- `register(result: CHResult<CHEmpty>)`：デバイスを登録します。
- `insertSesame(sesame: CHDevices, result: CHResult<CHEmpty>)`：Sesameデバイスを挿入します。
- `removeSesame(tag: String, result: CHResult<CHEmpty>)`：Sesameデバイスを取り外します。
- `onGattSesamePublish(receivePayload: SSM3PublishPayload)`：デバイスのリリースイベントを処理します。

## 継承

- `CHSesameOS3`
- `CHSesameTouchPro`
- `CHDeviceUtil`

![CHSesameTouchProDevice](CHSesameTouchProDevice.svg)