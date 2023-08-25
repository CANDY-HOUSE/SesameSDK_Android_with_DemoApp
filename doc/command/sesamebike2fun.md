# Bike2 讲解
## 实现类 CHSesameBike2Device
### 接口

```agsl
fun unlock(tag: ByteArray? = null, result: CHResult<CHEmpty>)
fun reset(result: CHResult<CHEmpty>)
fun getVersionTag(result: CHResult<CHEmpty>)

```
### 接口功能字义
- [unlock](unlock.md):开锁 
- [reset](reset.md):重置设备
- [getVersionTag](ssm5version.md):获取版本号
### 循环图
![CHSesameBike2Device](../class/CHSesameBike2Device.svg)





