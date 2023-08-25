# Sesame5 功能指令
## 实现类 CHSesame5Device 
### 接口
<!-- 
这是单行注释
- # var mechSetting: CHSesame5MechSettings?
-->
```agsl
fun lock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
fun toggle(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
fun magnet(result: CHResult<CHEmpty>)
fun configureLockPosition(lockTarget: Short, unlockTarget: Short, result: CHResult<CHEmpty>)
fun autolock(delay: Int, result: CHResult<Int>)
fun history(cursor: Long?, uuid: UUID, result: CHResult<Pair<List<CHSesame5History>, Long?>>)
fun reset(result: CHResult<CHEmpty>)
fun getVersionTag(result: CHResult<CHEmpty>)
```
### 接口功能字义
#### 1. [lock](lock.md):关锁 
#### 2. [unlock](unlock.md):开锁 
#### 3. toggle:开关锁切换
#### 4. [magnet](magnet.md):角度校正 
#### 5. [configureLockPosition](configureLockPosition.md):配置锁开关和锁位置 
#### 6. [ autolock ](autolock.md):自动关锁
- delay:延迟时长
#### 7. [history](history.md):历史记录
#### 8. [reset](reset.md):重置设备
#### 9. [getVersionTag](ssm5version.md):获取版本号

### 循环图
![CHSesame5Device](../class/CHSesame5Device.svg)





