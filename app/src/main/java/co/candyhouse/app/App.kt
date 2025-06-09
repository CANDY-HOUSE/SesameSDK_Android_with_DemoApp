package co.candyhouse.app

import android.content.Context
import co.candyhouse.app.base.BaseApp
import co.candyhouse.sesame.open.CHDeviceManager

class CandyHouseApp : BaseApp() {

    override fun onCreate() {
        super.onCreate()
        CHDeviceManager.app = this

        // 启动IoT连接
        initIoTConnection()
    }
}

val Context.candyHouseApplication: CandyHouseApp
    get() = applicationContext as CandyHouseApp
