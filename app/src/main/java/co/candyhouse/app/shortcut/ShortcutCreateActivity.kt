package co.candyhouse.app.shortcut

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import co.candyhouse.app.R
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.devices.base.CHDevices

class ShortcutCreateActivity : AppCompatActivity() {

    private var activeDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action != Intent.ACTION_CREATE_SHORTCUT) {
            cancelAndFinish()
            return
        }

        loadDevices()
    }

    override fun onDestroy() {
        activeDialog?.dismiss()
        activeDialog = null
        super.onDestroy()
    }

    private fun loadDevices() {
        CHDeviceManager.getCandyDevices { result ->
            if (isFinishing || isDestroyed) return@getCandyDevices

            result.onSuccess { state ->
                val devices = state.data.filter { isShortcutSupported(it) }
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    if (devices.isEmpty()) {
                        showMessageAndFinish(getString(R.string.shortcut_no_supported_devices))
                        return@runOnUiThread
                    }
                    showDeviceChooser(devices)
                }
            }
            result.onFailure {
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    showMessageAndFinish(getString(R.string.shortcut_load_devices_failed))
                }
            }
        }
    }

    private fun showDeviceChooser(devices: List<CHDevices>) {
        val labels = devices.map { it.getNickname() }.toTypedArray()

        activeDialog = AlertDialog.Builder(this)
            .setTitle(R.string.shortcut_choose_device)
            .setItems(labels) { _, index -> showActionChooser(devices[index]) }
            .setNegativeButton(android.R.string.cancel) { _, _ -> cancelAndFinish() }
            .setOnCancelListener { cancelAndFinish() }
            .show()
    }

    private fun showActionChooser(device: CHDevices) {
        val kind = device.toShortcutDeviceKindOrNull()
        if (kind == null) {
            showMessageAndFinish(getString(R.string.shortcut_invalid_device))
            return
        }

        val actions = kind.supportedActions().sortedBy { it.ordinal }
        val labels = actions.map { getString(it.labelRes()) }.toTypedArray()

        activeDialog = AlertDialog.Builder(this)
            .setTitle(device.getNickname())
            .setItems(labels) { _, index -> returnShortcut(device, actions[index]) }
            .setNegativeButton(android.R.string.cancel) { _, _ -> cancelAndFinish() }
            .setOnCancelListener { cancelAndFinish() }
            .show()
    }

    private fun returnShortcut(device: CHDevices, shortcutAction: SesameShortcutAction) {
        val deviceId = device.deviceId?.toString()
        if (deviceId.isNullOrBlank()) {
            showMessageAndFinish(getString(R.string.shortcut_invalid_device))
            return
        }

        val executeIntent = Intent(this, ShortcutExecuteActivity::class.java).apply {
            action = SesameShortcutContract.ACTION_EXECUTE
            putExtra(SesameShortcutContract.EXTRA_DEVICE_ID, deviceId)
            putExtra(SesameShortcutContract.EXTRA_ACTION, shortcutAction.wireName)
            putExtra(SesameShortcutContract.EXTRA_AUTH_TOKEN, SesameShortcutAuth.sign(deviceId, shortcutAction))
        }

        val label = getString(
            R.string.shortcut_label_format,
            device.getNickname(),
            getString(shortcutAction.labelRes()),
        )

        val shortcut = ShortcutInfoCompat.Builder(
            this,
            SesameShortcutContract.shortcutId(deviceId, shortcutAction),
        )
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(IconCompat.createWithResource(this, R.drawable.app_icon))
            .setIntent(executeIntent)
            .build()

        val resultIntent = ShortcutManagerCompat.createShortcutResultIntent(this, shortcut)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun isShortcutSupported(device: CHDevices): Boolean =
        device.deviceId != null && device.toShortcutDeviceKindOrNull() != null

    private fun showMessageAndFinish(message: String) {
        activeDialog = AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ -> cancelAndFinish() }
            .setOnCancelListener { cancelAndFinish() }
            .show()
    }

    private fun cancelAndFinish() {
        if (isFinishing) return
        setResult(Activity.RESULT_CANCELED)
        finish()
    }
}

private fun SesameShortcutAction.labelRes(): Int = when (this) {
    SesameShortcutAction.LOCK -> R.string.shortcut_action_lock
    SesameShortcutAction.UNLOCK -> R.string.shortcut_action_unlock
    SesameShortcutAction.TOGGLE -> R.string.shortcut_action_toggle
    SesameShortcutAction.CLICK -> R.string.shortcut_action_click
}
