package co.candyhouse.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import android.provider.Settings
import co.utils.L


object NfcHandler {
    fun  ishasNfc(context:Context){
      val b=  context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)
        L.d("hcia", "Nfc功能:$b")
        isEnableNfc(context)

    }
    fun  isEnableNfc(context:Context){
        var defaultAdapter = NfcAdapter.getDefaultAdapter(context)

        val b=     defaultAdapter.isEnabled
        L.d("hcia", "Nfc开启状态:$b")
        if (!b){
            context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
        }

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

}