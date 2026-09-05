package co.candyhouse.app.shortcut

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

/** Keeps one physical execution alive across Activity recreation. */
class ShortcutExecutionViewModel : ViewModel() {

    private var execution: Deferred<SesameShortcutResult>? = null

    fun executionOrStart(
        deviceId: String,
        action: SesameShortcutAction,
        allowStart: Boolean,
    ): Deferred<SesameShortcutResult>? {
        execution?.let { return it }
        if (!allowStart) return null

        return viewModelScope.async {
            SesameShortcutExecutor.execute(deviceId, action)
        }.also {
            execution = it
        }
    }
}
