# CHDeviceLoginStatus 列挙型クラス
```svg

enum class CHDeviceLoginStatus {
    Login, UnLogin,
}
```



`CHDeviceLoginStatus` は、デバイスのログイン状態を表す列挙型クラスです。この列挙型には、2つの列挙定数が含まれています。

以下は2つの列挙定数の意味です。

| 列挙定数 | 意味 |
| :----- | :----- |
| `Login` | デバイスはログインしている。 |
| `UnLogin` | デバイスはログインしていない。 |

この列挙型クラスは通常、デバイスの具体的な状態（`CHDeviceStatus`）と関連しており、デバイスが特定の状態でのログイン状況を表します。
