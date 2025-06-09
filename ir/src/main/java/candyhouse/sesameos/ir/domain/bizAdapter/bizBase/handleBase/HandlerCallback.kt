package candyhouse.sesameos.ir.domain.bizAdapter.bizBase.handleBase

import candyhouse.sesameos.ir.models.IrControlItem

interface HandlerCallback {
    fun onItemUpdate(item: IrControlItem)
}