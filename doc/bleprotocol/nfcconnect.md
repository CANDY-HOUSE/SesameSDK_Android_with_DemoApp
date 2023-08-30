# NFC接続に関する説明
### NFC（近距離無線通信）接続は通常、主にNFCタグとNFCピア・ツー・ピア接続を指します。以下は、これらの機能をAndroidデバイスでの使用方法について、簡単に説明します。
#### NFCタグの読書き：
- NFCタグデータの読取り：AndroidのNFC APIを使用して近くのNFCタグを検出し、タグに保存されているデータを読取ることができます。タグを検出した時にデータを処理するために、NfcAdapter.ReaderCallbackを登録します。
- NFCタグデータの書込み：タグのNDEF（NFCデータ交換形式）メッセージを設定することで、タグにデータを書込むことができます。NdefMessageを作成し、タグに書込みます。
#### NFCピア・ツー・ピア接続：

- NFC Beam：NFC Beamは、NFC対応可能のAndroidデバイス2台がNFCによって、データを転送することができるようにします。NfcAdapterのsetNdefPushMessageを使用して共有するデータを設定し、デバイスを近づけることでデータ転送を開始できます。
#### NFCイベントの処理：
Activityにおいて、onNewIntentをオーバーライドすることで、受信したNFC Intentを処理します。デバイスがNFCタグや他のNFCデバイスを検出した時に呼び出されることです。

### 以下は、AndroidでNFCタグデータを読取ることを示す例です。

```agsl

 override fun onNewIntent(intent: Intent?) {//系統方法 有系統的 Intent 進來 ,預期處理 ＮＦＣ操作
        super.onNewIntent(intent)
      L.d("hcia", "intent:" + intent)
        if (intent?.action == "android.intent.action.MAIN") {
            return
        }

        // nfc狀態有三種 1. 沒格式化過 2.格式化過 有資料 3 格式化沒資料
        if (intent?.action == NfcAdapter.ACTION_TAG_DISCOVERED) {// 3 格式化沒資料 ?? 不確定了。有點忘了 還是ＡＰＰ在前景時近入？？tse
            val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)!!
            val nfcHexID = tag.id.toHexString()
            supportFragmentManager.fragments.firstOrNull()?.childFragmentManager?.fragments?.forEach {
                if (it is NfcSetting) {//查找當前頁面fragment 如果是設定頁面。調用設定ＮＦＣ方法 onNfcId（）
                    (it as NfcSetting).onNfcId(nfcHexID)//tse 寫的拉基  onNfcId 兩次
                }
            }
        }
        if (intent?.action == NfcAdapter.ACTION_NDEF_DISCOVERED) {// 2.格式化過 有資料??  還是ＡＰＰ在背景時近入？？tse

            val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)!!
            val nfcHexID = tag.id.toHexString()
//            L.d("hcia", "NFCid:" + tag.id.toHexString())//048e3032e26c80
            CHDeviceManager.getCandyDevices {//查出設備
                it.onSuccess {
                    it.data.filter { (it.getNFC() == nfcHexID) }.forEach { device ->//根據設備紀錄的 SharedPreferencesUtils 看看有沒有對應的nfcID
//                        L.d("hcia", "device:" + device)
                        when (device) {//check 砍掉這個 when (device)
                            is CHSesameLock -> {
                                if (device.deviceShadowStatus == null) {//如果此設備沒有被wm2 連上？
                                    CHBleManager.enableScan(true) { } // 沒有聯網就開藍芽。
                                }
                                device.connect { }// 下連接指令 可能成功？可能失敗  ？？？？ APP近來的其他生命週期有設定自動重連，理論上這行也可以不需要，但是如果有可能會連快一點？看能不能刪掉
                                when (device) {
                                    is CHSesameBot -> {
                                        var checkBLELock = true
                                        CoroutineScope(IO).launch {
                                            for (index in 0 until 5) {
//                                                L.d("hcia", "checkBLELock:" + checkBLELock + " index:" + index)
                                                if (checkBLELock) {
                                                    device.click {
                                                        it.onSuccess {
                                                            checkBLELock = false
                                                        }
                                                    }
                                                    delay(2000)
                                                }
                                            }
                                        }

                                    }
                                    is CHSesame2 -> {//sesameOS2 ==> model--> sesame4  sesame2(客服認知sesame3)
                                        var checkBLELock = true// 記錄開鎖成功沒？
                                        CoroutineScope(IO).launch {
                                            for (index in 0 until 5) {//每隔兩秒開一次開開
                                                if (checkBLELock) {
                                                    device.toggle {
                                                        it.onSuccess {
                                                            checkBLELock = false
                                                        }
                                                        it.onFailure {}
                                                    }
                                                    delay(2000)
                                                }
                                            }
                                        }
                                    }
                                    is CHSesame5 -> {
                                        var checkBLELock = true// 記錄開鎖成功沒？
                                        CoroutineScope(IO).launch {
                                            for (index in 0 until 5) {//每隔兩秒開一次開開
                                                if (checkBLELock) {
                                                    device.toggle {
                                                        it.onSuccess {
                                                            checkBLELock = false
                                                        }
                                                        it.onFailure {}
                                                    }
                                                    delay(2000)
                                                }
                                            }
                                        }
                                    }
                                    is CHSesameBike -> {
                                        device.unlock {
                                            it.onFailure {
                                                GlobalScope.launch {
                                                    repeat(8) {
                                                        delay(1000)
                                                        if (device.deviceStatus.value == CHDeviceLoginStatus.Login) {
                                                            device.unlock {}
                                                            return@launch
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    is CHSesameBike2 -> {
                                        device.unlock {
                                            it.onFailure {
                                                GlobalScope.launch {
                                                    repeat(8) {
                                                        delay(1000)
                                                        if (device.deviceStatus.value == CHDeviceLoginStatus.Login) {
                                                            device.unlock {}
                                                            return@launch
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }//end when (device)
                            }
                        }
                    }
                }
            }
            supportFragmentManager.fragments.firstOrNull()?.childFragmentManager?.fragments?.forEach {
                if (it is NfcSetting) {
                    (it as NfcSetting).onNfcId(nfcHexID)//tse 寫了拉基  onNfcId兩次？？
                }
            }

        } else {//沒格式化或是沒有寫入資料。android 背景讀取會有問題
            NfcHandler.nfcCheckInetent(intent)//格式化 nfc tag ，並且要寫入資料
        }
        
        
          fun nfcCheckInetent(intent: Intent?) {
        L.d("hcia", "intent.action:" + intent?.action + " extras:" + intent?.extras)
        if (intent?.action == NfcAdapter.ACTION_TAG_DISCOVERED) {
            //   寫入 candynfc 這個無用字段到nfc 貼紙裡面。背景感應會沒有作用
            val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)!!
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                val ndefRecord = NdefRecord.createTextRecord(null, "candynfc")
                val records = arrayOf(ndefRecord)
                val ndefMessage = NdefMessage(records)
                try {
                    L.d("hcia", "寫入 candynfc:")
                    ndef.writeNdefMessage(ndefMessage)
                } catch (e: Exception) {
                    L.d("hcia", "寫入失敗 decode exception" + e)
                }
            } else {
                L.d("hcia", "intent:" + intent)
                L.d("hcia", "tag:" + tag)
                val ndefFormatable = NdefFormatable.get(tag)
                L.d("hcia", "ndefFormatable:" + ndefFormatable)
                try {
                    ndefFormatable.connect()
                    val ndefRecord = NdefRecord.createTextRecord(null, "candynfc")
                    val records = arrayOf(ndefRecord)
                    val newNdefMessage = NdefMessage(records)
                    ndefFormatable.format(newNdefMessage)
                    L.d("hcia", "格式化 Ndef:F")

                } catch (err: Exception) {
                    L.d("hcia", "格式化 Ndef:F 失敗 err:" + err)
                } finally {
                    try {
                        ndefFormatable.close()
                    } catch (e: java.lang.Exception) {
                    }
                }
            }
            //   nfc 格式化 不然有些廠商的nfc 貼紙會不作用
            val nfcA = NfcA.get(tag)
            if (nfcA != null) {
                try {
                    nfcA.connect()
                    nfcA.transceive(
                        byteArrayOf(
                            0xA2.toByte(),  // WRITE
                            0x03.toByte(),  // page = 3
                            0xE1.toByte(),
                            0x10.toByte(),
                            0x06.toByte(),
                            0x00.toByte() // capability container (mapping version 1.0, 48 bytes for data available, read/write allowed)
                        )
                    )
                    nfcA.transceive(
                        byteArrayOf(
                            0xA2.toByte(),  // WRITE
                            0x04.toByte(),  // page = 4
                            0x03.toByte(),
                            0x00.toByte(),
                            0xFE.toByte(),
                            0x00.toByte() // empty NDEF TLV, Terminator TLV
                        )
                    )
                    L.d("hcia", "格式化 Nfca:A")
                } catch (err: java.lang.Exception) {
                    L.d("hcia", "格式化  Nfca:A 失敗 err:" + err)
                } finally {
                    try {
                        nfcA.close()
                    } catch (e: java.lang.Exception) {
                    }
                }
            }
        }
    }
        
```

