# NSError クラスのドキュメント
```svg
class NSError(message: String, var domain: String, var code: Int) : Error(message)

```
`NSError` クラスは、特定のドメインとエラーコードを持つエラーを表すためのクラスで、`Error` クラスを継承しています。

## コンストラクタの引数

- `message`：エラーメッセージです。`Error` クラスから継承されたプロパティです。
- `domain`：エラーが発生したドメインを示す、文字列（String）型の変数です。
- `code`：特定のエラーコードを示す、整数（Int）型の変数です。

例外やエラーの処理時に、`NSError` クラスのインスタンスを作成し、具体的なエラーメッセージ、ドメイン、およびエラーコードを提供することで、エラーをより正確に識別して処理することができます。
