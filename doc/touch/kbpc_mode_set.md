# KeyboardPassword Mode Set 讲解
app 发送发送130指令，设置ssm_touch当前指纹状态
### 发送格式

|  Byte  |       1 |    0    |
|:------:|--------:|:-------:|
| Data   | pw_mode | command |

- command:指令130(固定)
- pw_mode:数字锁模式




### 接收格式

| Byte  |    3    | 2   |     1     |     0      |
|:---:|:-------:|:------:|:----:|:---------:|
| Data | pw_mode | status | command |response   |
- command:指令130(固定)
- response:响应0x07(固定)
    - status:0x00(成功)
    - pw_mode:0x00->验证模式，0x01->新增模式



### 循序图
![icon](kbpc_model_set.svg)





### android示例
``` java
  override fun keyBoardPassCodeModeSet(mode: Byte, result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_PASSCODE_MODE_SET.value, byteArrayOf(mode))) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

```
