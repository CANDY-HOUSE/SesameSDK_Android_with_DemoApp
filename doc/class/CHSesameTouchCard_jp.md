# CHSesameTouchCard クラスのドキュメント

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
`CHSesameTouchCard` クラスは、タッチカードの情報を処理および管理するためのクラスです。このクラスのコンストラクタは、ByteArray をパラメータとして受け取り、そのパラメータに関連するカードのデータを含んでいます。

## プロパティ

- `cardType`：カードのタイプを表す、データの最初のバイトです。
- `idLength`：カードのIDの長さを表す、データの2番目のバイトです。
- `cardID`：カードのIDを表す、データの3番目のバイトから始まる、長さが `idLength` のデータを16進数の文字列に変換したものです。
- `nameIndex`：カードの名前データの開始インデックスを表す、`idLength + 2` で計算されます。
- `nameLength`：カードの名前の長さを表す、データの `nameIndex` バイトです。
- `cardName`：カードの名前を表す、データの `nameIndex + 1` から始まる、長さが `nameLength` のデータを文字列に変換したものです。

## コンストラクタ

- `CHSesameTouchCard(data: ByteArray)`：このコンストラクタは、ByteArray をパラメータとして受け取り、そのパラメータに関連するカードのデータを解析し、対応するプロパティを初期化します。
