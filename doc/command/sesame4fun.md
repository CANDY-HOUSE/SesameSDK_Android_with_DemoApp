# Sesame5 功能指令
## 实现类 CHSesame2Device
### 接口

```agsl
  fun lock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
    fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
    fun toggle(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
    fun configureLockPosition(lockTarget: Short, unlockTarget: Short, result: CHResult<CHEmpty>)
    fun getAutolockSetting(result: CHResult<Int>)
    fun enableAutolock(delay: Int, historytag: ByteArray? = null, result: CHResult<Int>)
    fun disableAutolock(historytag: ByteArray? = null, result: CHResult<Int>)
    fun getHistories(cursor: Long?, result: CHResult<Pair<List<CHSesame2History>, Long?>>)
    fun reset(result: CHResult<CHEmpty>)
    fun getVersionTag(result: CHResult<CHEmpty>)
```
### 接口功能字义
#### 1. [lock](lock.md):关锁 
#### 2. [unlock](unlock.md):开锁 
#### 3. toggle:开关锁切换
#### 4. [configureLockPosition](configureLockPosition.md):配置锁开关和锁位置 
#### 5. [enableAutolock](autolock.md):自动关锁
#### 6. [disableAutolock](autolock.md):禁用自动关锁
- delay:延迟时长
#### 7. [getHistories](history.md):历史记录
#### 8. [reset](reset.md):重置设备
#### 9. [getVersionTag](ssm5version.md):获取版本号





