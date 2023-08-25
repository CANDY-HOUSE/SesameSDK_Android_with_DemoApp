## CHDeviceUtil 接口
```svg
internal interface CHDeviceUtil {
    var advertisement: CHadv?
    var sesame2KeyData: CHDevice?
    fun goIOT() {}
    fun login(token: String? = null)
}
```



### 描述

`CHDeviceUtil` 是一个内部接口，用于定义处理设备相关操作的方法和属性。

### 属性

- `advertisement`: 类型为 `CHadv`，是广播相关的数据，如果没有广播则可能为 `null`。

- `sesame2KeyData`: 类型为 `CHDevice` 的实例变量，存放设备（可能是指"鑰匙"）的相关数据。

### 方法

- `goIOT` 方法：这个方法用于订阅 IoT (Internet of Things) 的相关服务。此方法在接口中已经有默认的空实现，但可以在具体的实现类中覆写。

- `login` 方法：这个方法用于登录操作，匿名参数 `token` 默认值为 `null`.

### 使用

要使用这个接口，你可以创建一个实现了 `CHDeviceUtil` 接口的类，然后实例化那个类。在那个类中，你需要根据实际需求