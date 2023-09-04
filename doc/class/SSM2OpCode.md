# SSM2OpCode 列挙型
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

`SSM2OpCode`は、内部の列挙型です。

## 列挙値

- `create`: 値が `0x01`で、作成操作を指します。
- `read`: 値が  `0x02`で、読取り操作を指します。
- `update`: 値が  `0x03`で、更新操作を指します。
- `delete`: 値が  `0x04`で、削除操作を指します。
- `sync`: 値が  `0x05`で、同期操作を指します。
- `async`: 値が  `0x06`で、非同期操作を指します。
- `response`: 値が  `0x07`で、応答操作を指します。
- `publish`: 値が `0x08`で、送信操作を指します。
- `undefine`: 値が  `0x10`で、未定義操作を指します。

| SSM2OpCode | 命令名 | 説明   |
| ------ | -------- | ------ |
| 0x01   | create   | 作成  |
| 0x02   | read     | 読取り   |
| 0x03   | update   | 更新   |
| 0x04   | delete   | 削除  |
| 0x05   | sync     | 同期   |
| 0x06   | async    | 非同期   |
| 0x07   | response | 応答   |
| 0x08   | publish  | 送信   |
| 0x10   | undefine | 未定義 |


