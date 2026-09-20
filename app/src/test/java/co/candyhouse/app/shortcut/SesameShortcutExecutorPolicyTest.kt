package co.candyhouse.app.shortcut

import org.junit.Assert.assertEquals
import org.junit.Test

class SesameShortcutExecutorPolicyTest {

    @Test
    fun `device kinds with SDK transport fallback wait for a possible later success`() {
        val waitForSuccess = setOf(
            SesameShortcutDeviceKind.SESAME5,
            SesameShortcutDeviceKind.BIKE2,
            SesameShortcutDeviceKind.BOT,
            SesameShortcutDeviceKind.BOT2,
        )

        waitForSuccess.forEach { kind ->
            assertEquals(
                "$kind may report a transport failure before an IoT callback",
                ShortcutCommandFailurePolicy.WAIT_FOR_SUCCESS,
                shortcutCommandFailurePolicy(kind),
            )
        }
    }

    @Test
    fun `OS2 lock and bike failures are terminal`() {
        listOf(
            SesameShortcutDeviceKind.SESAME2,
            SesameShortcutDeviceKind.BIKE,
        ).forEach { kind ->
            assertEquals(
                "$kind should use terminal failure semantics",
                ShortcutCommandFailurePolicy.TERMINAL,
                shortcutCommandFailurePolicy(kind),
            )
        }
    }
}
