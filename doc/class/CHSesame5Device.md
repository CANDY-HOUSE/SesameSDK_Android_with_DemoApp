



 
# CHSesame5Device 类
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
`CHSesame5Device`类继承了`CHSesameOS3`类，并实现了`CHSesame5`和`CHDeviceUtil`接口。它代表了一个设备，预计是一个Sesame 5设备。

## 属性

- `currentDeviceUUID`：当前设备的UUID。
- `historyCallback`：历史记录的回调函数。
- `isHistory`：布尔变量，用于检查是否有历史记录。
- `mechSetting`：设备机制的设置。
- `advertisement`：设备广告数据。
- `isConnectedByWM2`：布尔变量，用于检查是否通过WM2连接。

## 方法

- `goIOT`：处理IoT操作的函数。
- `configureLockPosition`：配置锁定位置的函数。
- `autolock`：自动锁定的函数。
- `magnet`：处理磁性操作的函数。
- `eventToHistory`：将事件转换为历史记录的函数。
- `history`：获取历史记录的函数。
- `toggle`：切换设备状态的函数。
- `unlock`：解锁设备的函数。
- `lock`：锁定设备的函数。
- `register`：注册设备的函数。
- `login`：登录设备的函数。
- `readHistoryCommand`：读取历史命令的函数。
- `onGattSesamePublish`：处理GATT Sesame发布的函数。




  ![CHSesame5Device](CHSesame5Device.svg)