package co.candyhouse.app.shortcut

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import co.candyhouse.app.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ShortcutExecuteActivity : ComponentActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observerJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val executeIntent = intent?.takeIf { it.action == SesameShortcutContract.ACTION_EXECUTE }
        val deviceId = executeIntent?.getStringExtra(SesameShortcutContract.EXTRA_DEVICE_ID)
        val action = SesameShortcutAction.fromWireName(
            executeIntent?.getStringExtra(SesameShortcutContract.EXTRA_ACTION),
        )
        val token = executeIntent?.getStringExtra(SesameShortcutContract.EXTRA_AUTH_TOKEN)

        // The token, not the exported flag, is what stops other apps from forging a (deviceId, action) pair.
        if (deviceId.isNullOrBlank() || action == null || !SesameShortcutAuth.verify(deviceId, action, token)) {
            finish()
            return
        }

        val viewModel = ViewModelProvider(this)[ShortcutExecutionViewModel::class.java]
        val execution = viewModel.executionOrStart(
            deviceId = deviceId,
            action = action,
            // After process recreation the previous physical outcome is unknown; never resend.
            allowStart = savedInstanceState == null,
        )

        if (execution == null) {
            showFailureToast()
            finish()
            return
        }

        observerJob = scope.launch {
            val result = try {
                execution.await()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                SesameShortcutResult.Failed
            }

            when (result) {
                SesameShortcutResult.Success -> Unit

                SesameShortcutResult.DeviceNotFound,
                SesameShortcutResult.DeviceLookupFailed,
                SesameShortcutResult.UnsupportedAction,
                SesameShortcutResult.PreparationTimedOut,
                SesameShortcutResult.CommandOutcomeUnknown,
                SesameShortcutResult.Failed,
                -> showFailureToast()
            }
            finish()
        }
    }

    override fun onDestroy() {
        // The ViewModel owns execution; only stop this Activity's observer.
        observerJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun showFailureToast() {
        Toast.makeText(applicationContext, R.string.shortcut_execution_failed, Toast.LENGTH_SHORT).show()
    }
}
