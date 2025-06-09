package co.candyhouse.sesame.open.device.sesameBiometric.capability.passcode

import co.candyhouse.sesame.ble.SSM3PublishPayload
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.ble.os3.CHSesameTouchCard
import co.candyhouse.sesame.open.device.CHSesameConnector
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHEventHandler
import co.candyhouse.sesame.utils.L

class CHPassCodeEventHandler(private val delegate: CHPassCodeDelegate?) : CHEventHandler {
    @OptIn(ExperimentalStdlibApi::class)
    override fun handleEvent(device: CHSesameConnector, payload: SSM3PublishPayload): Boolean {
        if (delegate == null) return false

        when (payload.cmdItCode) {
            SesameItemCode.SSM_OS3_PASSCODE_CHANGE.value -> {
                val card = CHSesameTouchCard(payload.payload)
                delegate.onKeyBoardChanged(device, card.cardID, card.cardName, card.cardType)
                return true
            }
            SesameItemCode.SSM_OS3_PASSCODE_NOTIFY.value -> {
                var cardDataSize = 0
                var cards = payload.payload
                var card: CHSesameTouchCard
                do {
                    cards = cards.drop(cardDataSize).toByteArray()
                    if (cards.isEmpty()) break
                    card = CHSesameTouchCard(cards)
                    cardDataSize = 1 + 1 + card.idLength + 1 + card.nameLength
                    delegate.onKeyBoardReceive(device, card.cardID, card.cardName, card.cardType)
                } while ((cards.isNotEmpty()))
                return true
            }
            SesameItemCode.SSM_OS3_PASSCODE_FIRST.value -> {
                delegate.onKeyBoardReceiveStart(device)
                return true
            }
            SesameItemCode.SSM_OS3_PASSCODE_LAST.value -> {
                delegate.onKeyBoardReceiveEnd(device)
                return true
            }
            SesameItemCode.SSM_OS3_PASSCODE_MODE_SET.value -> {
                L.d("hcia", "SSM_OS3_PASSCODE_MODE_SET : " + payload.payload.toHexString())
                delegate.onKeyBoardModeChange(device, payload.payload[0])
                return true
            }
            SesameItemCode.SSM_OS3_PASSCODE_DELETE.value -> {
                L.d("hcia", "[0]SSM_OS3_PASSCODE_DELETE : " + payload.payload.toHexString())
                val pwdIDLen = payload.payload[2].toInt()
                val pwdID = payload.payload.sliceArray(3..pwdIDLen + 2).toHexString()
                L.d("hcia", "[1]SSM_OS3_PASSCODE_DELETE : $pwdID")
                delegate.onKeyBoardDelete(device, pwdID)
                return true
            }
            else -> return false
        }
    }
}