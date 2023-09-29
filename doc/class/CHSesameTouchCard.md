
# CHSesameTouchCard クラス
```svg
class CHSesameTouchCard(data: ByteArray) {
val cardType = data[0]
val idLength = data[1]
val cardID = data.sliceArray(2..idLength + 1).toHexString()
val nameIndex = idLength + 2
val nameLength = data[nameIndex]
val cardName = String(data.sliceArray(nameIndex + 1..nameIndex + nameLength))
}

```
`CHSesameTouchCard`クラスは、タッチカードの情報を処理および管理します。このクラスのコンストラクタは、ByteArrayを引数として受取り、タッチカードの関連データを含みます。

## プロパティ

- `cardType`：カードタイプです。データの最初のバイトで表されます。
- `idLength`：カードIDの長さです。データの2番目のバイトで表されます。
- `cardID`：カードIDです。データの3番目のバイトから始まり、長さが`idLength`であるデータセグメントで表されて、16進数の文字列に変換されます。
- `nameIndex`：カード名のデータの始点インデックスです。`idLength + 2`で計算されます。
- `nameLength`：カード名の長さです。データの`nameIndex`バイトで表されます。
- `cardName`：カード名です。データの`nameIndex + 1`バイトから始まり、長さが`nameLength`であるデータバイト列で表されて、文字列に変換されます。

## コンストラクタ

- `CHSesameTouchCard(data: ByteArray)`：コンストラクタは、ByteArrayをパラメータとして受取り、パラメータにはタッチカードの関連データが含まれています。コンストラクタではデータを解析し、対応する属性を初期化します。
