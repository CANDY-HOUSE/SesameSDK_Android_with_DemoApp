package co.candyhouse.app.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import co.candyhouse.app.SesameApp
import co.candyhouse.app.ble.BleController
import co.candyhouse.sesame.ble.CHDeviceLoginStatus
import co.candyhouse.sesame.utils.L
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds

/** NFC tag binding for a device settings page, and tap-to-toggle for bound tags. */
class NfcTags(
    private val activity: Activity, private val scope: CoroutineScope, private val ble: BleController,
    private val requestBle: () -> Unit, private val emit: (String, Any) -> Unit
) {
    private val app = activity.application as SesameApp

    /** Device whose settings page is waiting for a tag to bind; null means taps toggle bound devices. */
    var binding: String? = null

    fun enable() {
        NfcAdapter.getDefaultAdapter(activity)?.enableForegroundDispatch(
            activity,
            PendingIntent.getActivity(
                activity,
                0,
                Intent(activity, activity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            ), null, null
        )
    }

    fun disable() {
        NfcAdapter.getDefaultAdapter(activity)?.disableForegroundDispatch(activity)
    }

    fun handle(intent: Intent) {
        if (intent.action !in setOf(
                NfcAdapter.ACTION_TAG_DISCOVERED,
                NfcAdapter.ACTION_NDEF_DISCOVERED,
                NfcAdapter.ACTION_TECH_DISCOVERED
            )
        ) return
        @Suppress("DEPRECATION") val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return
        val tagId = tag.id.joinToString("") { "%02x".format(it.toInt() and 255) }
        val newlyBound = binding?.takeUnless { app.devicePreferences.nfc(it).equals(tagId, true) }
        binding?.let { id ->
            app.devicePreferences.setNfc(id, tagId); emit("devicePreferences", app.devicePreferences.snapshot(id))
            if (intent.action == NfcAdapter.ACTION_TAG_DISCOVERED) scope.launch(Dispatchers.IO) { format(tag) }
            if (intent.action != NfcAdapter.ACTION_NDEF_DISCOVERED) return
        }
        scope.launch {
            ble.restoreSaved(); requestBle()
            ble.allDevices().filter {
                val id = it.deviceId.toString()
                !id.equals(newlyBound, true) && app.devicePreferences.nfc(id).equals(tagId, true)
            }.forEach { device ->
                launch {
                    runCatching {
                        withTimeout(12000.milliseconds) {
                            while (device.deviceStatus.value != CHDeviceLoginStatus.logined && device.deviceId.toString()
                                    .uppercase() !in app.bleBackend.gatewayDevices
                            ) delay(500.milliseconds)
                            ble.quickCommand(device.deviceId.toString(), "toggle")
                        }
                    }.onFailure { L.e("SesameNfc", "Tag operation failed", it) }
                }
            }
        }
    }

    private fun format(tag: Tag) {
        runCatching {
            val message = NdefMessage(arrayOf(NdefRecord.createTextRecord(null, "candynfc")))
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                try {
                    ndef.connect(); ndef.writeNdefMessage(message)
                } finally {
                    ndef.close()
                }
            } else NdefFormatable.get(tag)?.let { format ->
                try {
                    format.connect(); format.format(message)
                } finally {
                    format.close()
                }
            }
        }.onFailure { L.e("SesameNfc", "Tag formatting failed", it) }
    }
}
