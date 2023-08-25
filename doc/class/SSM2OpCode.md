# SSM2OpCode 枚举类
```svg
internal enum class SSM2OpCode(val value: Byte) {
        create(0x01) 
        read(0x02) 
        update(0x03) 
        delete(0x04) 
        sync(0x05)
        async(0x06) 
        response(0x07)
        publish(0x08) 
        undefine(0x10) 

}

```

`SSM2OpCode` 是一个内部枚举类。

## 枚举值

- `create`: 值为 `0x01`，表示创建操作。
- `read`: 值为 `0x02`，表示读取操作。
- `update`: 值为 `0x03`，表示更新操作。
- `delete`: 值为 `0x04`，表示删除操作。
- `sync`: 值为 `0x05`，表示同步操作。
- `async`: 值为 `0x06`，表示异步操作。
- `response`: 值为 `0x07`，表示响应操作。
- `publish`: 值为 `0x08`，表示发布操作。
- `undefine`: 值为 `0x10`，表示未定义的操作。

| SSM2OpCode | 指令名称 | 描述   |
| ------ | -------- | ------ |
| 0x01   | create   | 创建   |
| 0x02   | read     | 读取   |
| 0x03   | update   | 更新   |
| 0x04   | delete   | 删除   |
| 0x05   | sync     | 同步   |
| 0x06   | async    | 异步   |
| 0x07   | response | 响应   |
| 0x08   | publish  | 推送   |
| 0x10   | undefine | 未定义 |


