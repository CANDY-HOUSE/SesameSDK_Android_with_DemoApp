# Android 项目结构示例

![SesameOs3](bleconnect/SesameOs3.jpg)
以下是一个常见的 Android 项目结构示例，用于组织代码和资源文件。

在这个示例中，主要的项目结构包括：
- `libs/`：文件夹通常用于存放第三方库（libraries）的二进制文件（如.jar、.aar等）。
- `java/`：存放 Java 代码。按照功能模块划分子包，例如 `activities/` 存放活动类，`adapters/` 存放适配器类等。

- `res/`：存放资源文件，包括布局文件、字符串、图标等。按照资源类型分目录存放，例如 `layout/` 存放布局文件，`drawable/` 存放图片资源，`values/` 存放字符串和样式资源。

- `build.gradle`：应用级别的 Gradle 配置文件，用于配置依赖项、插件和构建设置。

- `proguard-rules.pro`：ProGuard 配置文件，用于代码混淆和优化。这是可选的，适用于发布版本时。
- co.candyhouse.sesame.ble: 主要存放os2、os3 Ble设备连接、功能 
- co.candyhouse.sesame.db: 本地化存储 
- co.candyhouse.sesame.open: ble 设备管理、配置、参数
- co.candyhouse.sesame.utils: ble 传输加密算法 、log日志

