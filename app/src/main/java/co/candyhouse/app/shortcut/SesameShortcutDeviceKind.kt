package co.candyhouse.app.shortcut

import co.candyhouse.sesame.open.devices.CHSesame2
import co.candyhouse.sesame.open.devices.CHSesame5
import co.candyhouse.sesame.open.devices.CHSesameBike
import co.candyhouse.sesame.open.devices.CHSesameBike2
import co.candyhouse.sesame.open.devices.CHSesameBot
import co.candyhouse.sesame.open.devices.CHSesameBot2
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.app.shortcut.SesameShortcutAction.CLICK
import co.candyhouse.app.shortcut.SesameShortcutAction.LOCK
import co.candyhouse.app.shortcut.SesameShortcutAction.TOGGLE
import co.candyhouse.app.shortcut.SesameShortcutAction.UNLOCK

enum class SesameShortcutDeviceKind {
    SESAME5,
    SESAME2,
    BIKE,
    BIKE2,
    BOT,
    BOT2,
}

fun SesameShortcutDeviceKind.supportedActions(): Set<SesameShortcutAction> = when (this) {
    SesameShortcutDeviceKind.SESAME5, SesameShortcutDeviceKind.SESAME2 -> setOf(LOCK, UNLOCK, TOGGLE)
    SesameShortcutDeviceKind.BIKE, SesameShortcutDeviceKind.BIKE2 -> setOf(UNLOCK)
    SesameShortcutDeviceKind.BOT, SesameShortcutDeviceKind.BOT2 -> setOf(CLICK)
}

fun SesameShortcutDeviceKind.supports(action: SesameShortcutAction): Boolean =
    action in supportedActions()

fun CHDevices.toShortcutDeviceKindOrNull(): SesameShortcutDeviceKind? = when (this) {
    is CHSesame5 -> SesameShortcutDeviceKind.SESAME5
    is CHSesame2 -> SesameShortcutDeviceKind.SESAME2
    is CHSesameBike2 -> SesameShortcutDeviceKind.BIKE2
    is CHSesameBike -> SesameShortcutDeviceKind.BIKE
    is CHSesameBot2 -> SesameShortcutDeviceKind.BOT2
    is CHSesameBot -> SesameShortcutDeviceKind.BOT
    else -> null
}
