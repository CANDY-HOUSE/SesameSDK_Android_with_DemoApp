package co.candyhouse.app.base

import co.candyhouse.sesame.ble.CHSesame2

open class BaseSSMFG : BaseNFG() {
    companion object {
        @JvmField
        var mSesame: CHSesame2? = null
    }

    override fun onResume() {
        super.onResume()
//        CHBleManager.delegate = object : CHBleManagerDelegate {
//            override fun didDiscoverSesame(device: CHSesameBleInterface) {
//                if (device.bleIdStr == mSesame?.bleIdStr) {
//                    device.delegate = mSesame?.delegate
//                    mSesame = device
//                    mSesame?.connnect()
//                    return
//                }
//            }
//        }
    }
}