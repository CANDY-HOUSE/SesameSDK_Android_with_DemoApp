package co.candyhouse.sesame.open.device.sesameBiometric.capability.card

import co.candyhouse.sesame.ble.SSM3PublishPayload
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.ble.os3.CHSesameTouchCard
import co.candyhouse.sesame.open.device.CHSesameConnector
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHEventHandler
import co.candyhouse.sesame.utils.L

class CHCardEventHandler(private val delegate: CHCardDelegate?) : CHEventHandler {

    @OptIn(ExperimentalStdlibApi::class)
    override fun handleEvent(device: CHSesameConnector, payload: SSM3PublishPayload): Boolean {
        if (delegate == null) return false
        when (payload.cmdItCode) {
            SesameItemCode.SSM_OS3_CARD_CHANGE.value -> {
                val card = CHSesameTouchCard(payload.payload)
                delegate.onCardChanged(device, card.cardID, card.cardName, card.cardType)
                return true
            }
            SesameItemCode.SSM_OS3_CARD_NOTIFY.value -> {
                var cardDataSize = 0
                var cards = payload.payload
                var card: CHSesameTouchCard
                do {
                    cards = cards.drop(cardDataSize).toByteArray()
                    if (cards.isEmpty()) break
                    card = CHSesameTouchCard(cards)
                    cardDataSize = 1 + 1 + card.idLength + 1 + card.nameLength
                    delegate.onCardReceive(device, card.cardID, card.cardName, card.cardType)
                } while ((cards.isNotEmpty()))
                return true
            }
            SesameItemCode.SSM_OS3_CARD_FIRST.value -> {
                delegate.onCardReceiveStart(device)
                return true
            }
            SesameItemCode.SSM_OS3_CARD_LAST.value -> {
                delegate.onCardReceiveEnd(device)
                return true
            }
            SesameItemCode.SSM_OS3_CARD_MODE_SET.value -> {
                L.d("hcia", "SSM_OS3_CARD_MODE_SET : " + payload.payload.toHexString())
                delegate.onCardModeChanged(device, payload.payload[0])
                return true
            }
            SesameItemCode.SSM_OS3_CARD_DELETE.value -> {
                L.d("hcia", "[0]SSM_OS3_CARD_DELETE : " + payload.payload.toHexString())
                val cardIDLen = payload.payload[2].toInt()
                val cardID = payload.payload.sliceArray(3..cardIDLen + 2).toHexString()
                L.d("hcia", "[1]SSM_OS3_CARD_DELETE : $cardID")
                delegate.onCardDelete(device, cardID)
                return true
            }
            else ->
                return false
        }
    }
}