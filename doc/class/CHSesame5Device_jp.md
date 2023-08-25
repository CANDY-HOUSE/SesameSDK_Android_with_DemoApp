# CHSesame5Device クラスのドキュメント
```svg
internal class CHSesame5Device : CHSesameOS3(), CHSesame5, CHDeviceUtil {
    private var currentDeviceUUID: UUID? = null
    private var historyCallback: CHResult<Pair<List<CHSesame5History>, Long?>>? = null
    var isHistory: Boolean = false
      

    override var mechSetting: CHSesame5MechSettings? = null
    override var advertisement: CHadv? = null
     
    var isConnectedByWM2: Boolean = false

    override fun goIOT() {
        // Implementation
    }

    override fun configureLockPosition(lockTarget: Short, unlockTarget: Short, result: CHResult<CHEmpty>) {
        // Implementation
    }

    override fun autolock(delay: Int, result: CHResult<Int>) {
        // Implementation
    }

    override fun magnet(result: CHResult<CHEmpty>) {
        // Implementation
    }

    private fun eventToHistory(historyType: Sesame2HistoryTypeEnum?, ts: Long, recordID: Int, mechStatus: CHSesame5MechStatus?, histag: ByteArray?): CHSesame5History? {
        // Implementation
    }

    override fun history(cursor: Long?, uuid: UUID, result: CHResult<Pair<List<CHSesame5History>, Long?>>) {
        // Implementation
    }

    override fun toggle(historytag: ByteArray?, result: CHResult<CHEmpty>) {
        // Implementation
    }

    override fun unlock(historytag: ByteArray?, result: CHResult<CHEmpty>) {
        // Implementation
    }

    override fun lock(historytag: ByteArray?, result: CHResult<CHEmpty>) {
        // Implementation
    }

    override fun register(result: CHResult<CHEmpty>) {
        // Implementation
    }

    override fun login(token: String?) {
        // Implementation
    }

    private fun readHistoryCommand() {
        // Implementation
    }

    override fun onGattSesamePublish(receivePayload: SSM3PublishPayload) {
        // Implementation
    }
}


```


`CHSesame5Device` クラスは `CHSesameOS3` クラスを継承し、`CHSesame5` および `CHDeviceUtil` インターフェースを実装したクラスです。これは Sesame 5 デバイスを表します。

## プロパティ

- `currentDeviceUUID`：現在のデバイスの UUID。

- `historyCallback`：履歴コールバック関数。

- `isHistory`：履歴があるかどうかを示すブール値。

- `mechSetting`：デバイスのメカニズム設定。

- `advertisement`：デバイスの広告データ。

- `isConnectedByWM2`：WM2 を介して接続されているかどうかを示すブール値。

## メソッド

- `goIOT`：IoT 操作を処理するメソッド。

- `configureLockPosition`：ロック位置を設定するメソッド。

- `autolock`：自動ロックを行うメソッド。

- `magnet`：磁気操作を処理するメソッド。

- `eventToHistory`：イベントを履歴に変換するメソッド。

- `history`：履歴を取得するメソッド。

- `toggle`：デバイスの状態を切り替えるメソッド。

- `unlock`：デバイスを解錠するメソッド。

- `lock`：デバイスを施錠するメソッド。

- `register`：デバイスを登録するメソッド。

- `login`：デバイスにログインするメソッド。

- `readHistoryCommand`：履歴コマンドを読み取るメソッド。

- `onGattSesamePublish`：GATT Sesame パブリッシュを処理するメソッド。

以上が `CHSesame5Device` クラスの基本的な説明です。これは Sesame 5 デバイスに対して完全な操作と管理メソッドを提供します。
