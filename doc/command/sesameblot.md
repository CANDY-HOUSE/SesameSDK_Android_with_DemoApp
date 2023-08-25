# Bot1 讲解
## 实现类 CHSesameBotDevice
### 接口
<!--     fun toggle(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)
fun lock(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)
fun unlock(historyTag: ByteArray? = null, result: CHResult<CHEmpty>) -->
```agsl
    fun updateSetting(setting: CHSesameBotMechSettings, historyTag: ByteArray? = null, result: CHResult<CHEmpty>)

    fun click(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)
    fun reset(result: CHResult<CHEmpty>)
    fun getVersionTag(result: CHResult<CHEmpty>)
```
### 接口功能字义

- [updateSetting](updatasetting.md):模式设置
- [click](click.md):点击复原
- [getVersionTag](ssm5version.md):获取版本号
- [reset](reset.md):重置设备






