package co.candyhouse.sesame.server.dto

/**
 * 名称统一修改
 *
 * @author frey on 2025/11/27
 */
sealed class CHAuthenticationNameRequest {
    data class Card(val request: CHCardNameRequest) : CHAuthenticationNameRequest()
    data class Face(val request: CHFaceNameRequest) : CHAuthenticationNameRequest()
    data class FingerPrint(val request: CHFingerPrintNameRequest) : CHAuthenticationNameRequest()
    data class Palm(val request: CHPalmNameRequest) : CHAuthenticationNameRequest()
    data class KeyBoardPassCode(val request: CHKeyBoardPassCodeNameRequest) : CHAuthenticationNameRequest()

    companion object {
        fun card(
            cardType: Byte,
            cardNameUUID: String,
            subUUID: String,
            stpDeviceUUID: String,
            name: String,
            cardID: String,
            op: String = "nfc_card_putname"
        ) = Card(CHCardNameRequest(cardType, cardNameUUID, subUUID, stpDeviceUUID, name, cardID, System.currentTimeMillis(), op))

        fun face(
            type: Byte,
            faceNameUUID: String,
            subUUID: String,
            stpDeviceUUID: String,
            name: String,
            faceID: String,
            op: String = "face_putname"
        ) = Face(CHFaceNameRequest(type, faceNameUUID, subUUID, stpDeviceUUID, name, faceID, System.currentTimeMillis(), op))

        fun fingerPrint(
            type: Byte,
            fingerPrintNameUUID: String,
            subUUID: String,
            stpDeviceUUID: String,
            name: String,
            fingerPrintID: String,
            op: String = "fingerprint_putname"
        ) = FingerPrint(
            CHFingerPrintNameRequest(
                type,
                fingerPrintNameUUID,
                subUUID,
                stpDeviceUUID,
                name,
                fingerPrintID,
                System.currentTimeMillis(),
                op
            )
        )

        fun palm(
            type: Byte,
            palmNameUUID: String,
            subUUID: String,
            stpDeviceUUID: String,
            name: String,
            palmID: String,
            op: String = "palm_putname"
        ) = Palm(CHPalmNameRequest(type, palmNameUUID, subUUID, stpDeviceUUID, name, palmID, System.currentTimeMillis(), op))

        fun keyBoardPassCode(
            type: Byte,
            keyBoardPassCodeNameUUID: String,
            subUUID: String,
            stpDeviceUUID: String,
            name: String,
            keyBoardPassCode: String,
            op: String = "passcode_putname"
        ) = KeyBoardPassCode(
            CHKeyBoardPassCodeNameRequest(
                type,
                keyBoardPassCodeNameUUID,
                subUUID,
                stpDeviceUUID,
                name,
                keyBoardPassCode,
                System.currentTimeMillis(),
                op
            )
        )
    }
}