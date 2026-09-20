package co.candyhouse.app.shortcut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SesameShortcutDeviceKindTest {

    @Test
    fun `sesame 5 and sesame 2 support lock, unlock and toggle only`() {
        val expected = setOf(SesameShortcutAction.LOCK, SesameShortcutAction.UNLOCK, SesameShortcutAction.TOGGLE)

        assertEquals(expected, SesameShortcutDeviceKind.SESAME5.supportedActions())
        assertEquals(expected, SesameShortcutDeviceKind.SESAME2.supportedActions())
    }

    @Test
    fun `bike and bike2 support unlock only`() {
        assertEquals(setOf(SesameShortcutAction.UNLOCK), SesameShortcutDeviceKind.BIKE.supportedActions())
        assertEquals(setOf(SesameShortcutAction.UNLOCK), SesameShortcutDeviceKind.BIKE2.supportedActions())
    }

    @Test
    fun `bot and bot2 support click only`() {
        assertEquals(setOf(SesameShortcutAction.CLICK), SesameShortcutDeviceKind.BOT.supportedActions())
        assertEquals(setOf(SesameShortcutAction.CLICK), SesameShortcutDeviceKind.BOT2.supportedActions())
    }

    @Test
    fun `supports reflects the per-kind action matrix`() {
        assertTrue(SesameShortcutDeviceKind.SESAME5.supports(SesameShortcutAction.LOCK))
        assertFalse(SesameShortcutDeviceKind.SESAME5.supports(SesameShortcutAction.CLICK))

        assertTrue(SesameShortcutDeviceKind.BIKE2.supports(SesameShortcutAction.UNLOCK))
        assertFalse(SesameShortcutDeviceKind.BIKE2.supports(SesameShortcutAction.LOCK))
        assertFalse(SesameShortcutDeviceKind.BIKE2.supports(SesameShortcutAction.TOGGLE))

        assertTrue(SesameShortcutDeviceKind.BOT2.supports(SesameShortcutAction.CLICK))
        assertFalse(SesameShortcutDeviceKind.BOT2.supports(SesameShortcutAction.UNLOCK))
    }

    @Test
    fun `every device kind supports at least one action`() {
        SesameShortcutDeviceKind.entries.forEach { kind ->
            assertTrue("$kind must support at least one shortcut action", kind.supportedActions().isNotEmpty())
        }
    }
}
