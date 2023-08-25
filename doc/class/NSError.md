
# NSError 类
```svg
class NSError(message: String, var domain: String, var code: Int) : Error(message)

```
`NSError`类继承自`Error`类，用于表示具有特定域的错误码。

## 构造函数参数

- `message`：错误信息，继承自`Error`类。
- `domain`：表示错误所在的域，这是一个`String`类型的变量。
- `code`：表示特定的错误码，这是一个`Int`类型的变量。

在处理异常或错误时，可以创建`NSError`的实例，并提供具体的错误信息、域和错误码，这有助于更准确地识别和处理错误。
