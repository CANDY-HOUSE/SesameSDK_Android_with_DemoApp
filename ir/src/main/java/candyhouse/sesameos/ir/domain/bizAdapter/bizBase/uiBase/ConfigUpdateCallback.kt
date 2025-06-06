package candyhouse.sesameos.ir.domain.bizAdapter.bizBase.uiBase

import candyhouse.sesameos.ir.models.IrControlItem

interface ConfigUpdateCallback {
    fun onItemUpdate(item: IrControlItem)
}