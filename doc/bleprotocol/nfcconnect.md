# NFC连接讲解
### NFC（近场通讯）连接通常涉及两个主要方面：NFC标签和NFC点对点连接。以下是关于如何在Android设备上使用这些功能的简要说明：
#### NFC标签读写：
- 读取NFC标签数据： 你可以使用Android的NFC API检测到附近的NFC标签，并读取标签上存储的数据。你可以注册一个NfcAdapter.ReaderCallback以在检测到标签时处理数据。
- 写入NFC标签数据： 通过设置标签的NDEF（NFC数据交换格式）消息，你可以在标签上写入数据。这通常涉及创建一个NdefMessage，然后将其写入标签。
#### NFC点对点连接：

- NFC Beam： NFC Beam允许两个支持NFC的Android设备通过NFC进行数据传输。你可以使用NfcAdapter的setNdefPushMessage方法来设置要共享的数据，然后通过将设备放在一起来触发数据传输。
#### 处理NFC事件：
在Activity中，可以通过重写onNewIntent方法来处理接收到的NFC意图（Intent）。这是当设备检测到NFC标签或另一个NFC设备时调用的方法

### 下面是一个简单的示例，演示如何在Android中读取NFC标签数据：

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

