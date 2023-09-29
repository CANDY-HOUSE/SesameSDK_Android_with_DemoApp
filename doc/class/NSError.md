
# NSError クラス
```svg
class NSError(message: String, var domain: String, var code: Int) : Error(message)

```
`NSError`クラスは`Error`クラスを継承し、特定のドメインを持つエラーコードを表します。

## コンストラクタのパラメータ

- `message`：エラーメッセージを表し、`Error`クラスを継承します。
- `domain`：エラーが発生したドメインを表し、`String`型のの変数です。
- `code`：特定のエラーコードを表し、`Int`型の変数です。

例外やエラーの処理において、`NSError`のインスタンスを作成し、具体的なエラーメッセージ、ドメイン、およびエラーコードを提供することができます。エラーをより正確に特定し、処理することに役立ちます。
