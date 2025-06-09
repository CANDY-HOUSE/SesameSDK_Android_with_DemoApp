package co.candyhouse.sesame.server.dto

internal data class Sesame2Shadow(
    var state: Sesame2ShadowState
)

internal data class Sesame2ShadowState(
    var reported: Sesame2Status
)

internal data class Sesame2Status(
    var mechst: String?,
    var wm2s: Map<String, String>?
)

internal data class WM2Shadow(
    var state: WM2ShadowShadowState
)

internal data class WM2ShadowShadowState(
    var reported: WM2ShadowStatus
)

internal data class WM2ShadowStatus(
    var c: Boolean?,
    var ssks: String?
)

internal data class Sesame5ShadowDocuments(
    var current: Sesame5ShadowDocumentsCurrent
)

internal data class Sesame5ShadowDocumentsCurrent(
    var state: Sesame2ShadowState
)
