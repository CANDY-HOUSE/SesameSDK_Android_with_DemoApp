package co.candyhouse.app.shortcut

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SesameShortcutContractTest {

    @Test
    fun `fromWireName resolves every declared action by its stable wire string`() {
        assertEquals(SesameShortcutAction.LOCK, SesameShortcutAction.fromWireName("lock"))
        assertEquals(SesameShortcutAction.UNLOCK, SesameShortcutAction.fromWireName("unlock"))
        assertEquals(SesameShortcutAction.TOGGLE, SesameShortcutAction.fromWireName("toggle"))
        assertEquals(SesameShortcutAction.CLICK, SesameShortcutAction.fromWireName("click"))
    }

    @Test
    fun `fromWireName rejects unknown, blank, and null values`() {
        assertNull(SesameShortcutAction.fromWireName("unknown"))
        assertNull(SesameShortcutAction.fromWireName(""))
        assertNull(SesameShortcutAction.fromWireName(null))
        assertNull(SesameShortcutAction.fromWireName("LOCK")) // wire names are lowercase-only
    }

    @Test
    fun `wireName round trips through fromWireName for every action`() {
        SesameShortcutAction.entries.forEach { action ->
            assertEquals(action, SesameShortcutAction.fromWireName(action.wireName))
        }
    }

    @Test
    fun `shortcutId is stable and distinct per device and action`() {
        val deviceId = "AABBCCDD-1234-5678-9900-AABBCCDDEEFF"

        val lockId = SesameShortcutContract.shortcutId(deviceId, SesameShortcutAction.LOCK)
        val unlockId = SesameShortcutContract.shortcutId(deviceId, SesameShortcutAction.UNLOCK)

        assertEquals(lockId, SesameShortcutContract.shortcutId(deviceId, SesameShortcutAction.LOCK))
        assertNotEquals("shortcut ids for different actions on the same device must differ", lockId, unlockId)
    }

    @Test
    fun `shortcutId is case insensitive on the device id`() {
        val lower = SesameShortcutContract.shortcutId("aabbccdd", SesameShortcutAction.TOGGLE)
        val upper = SesameShortcutContract.shortcutId("AABBCCDD", SesameShortcutAction.TOGGLE)

        assertEquals(lower, upper)
    }

    @Test
    fun `shortcutId differs across devices for the same action`() {
        val first = SesameShortcutContract.shortcutId("device-1", SesameShortcutAction.CLICK)
        val second = SesameShortcutContract.shortcutId("device-2", SesameShortcutAction.CLICK)

        assertNotEquals(first, second)
    }

    @Test
    fun `tokenMessage is stable and case insensitive on the device id`() {
        val lower = SesameShortcutContract.tokenMessage("aabbccdd", SesameShortcutAction.LOCK)
        val upper = SesameShortcutContract.tokenMessage("AABBCCDD", SesameShortcutAction.LOCK)

        assertArrayEquals(lower, upper)
    }

    @Test
    fun `tokenMessage differs across devices and actions`() {
        val deviceId = "device-1"

        val byDevice = SesameShortcutContract.tokenMessage(deviceId, SesameShortcutAction.LOCK)
        val otherDevice = SesameShortcutContract.tokenMessage("device-2", SesameShortcutAction.LOCK)
        val otherAction = SesameShortcutContract.tokenMessage(deviceId, SesameShortcutAction.UNLOCK)

        assertFalse(byDevice.contentEquals(otherDevice))
        assertFalse(byDevice.contentEquals(otherAction))
    }
}
