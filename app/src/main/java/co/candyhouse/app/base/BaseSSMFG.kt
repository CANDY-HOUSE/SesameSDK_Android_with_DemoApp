package co.candyhouse.app.base

import co.candyhouse.sesame.ble.Sesame2.CHSesame2

open class BaseSSMFG : BaseNFG() {
    companion object {
        @JvmField
        var mSesame: CHSesame2? = null
    }
}