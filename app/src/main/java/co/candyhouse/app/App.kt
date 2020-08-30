package co.candyhouse.app

import android.animation.ValueAnimator
import android.app.Application
import co.candyhouse.sesame.ble.CHBleManager
import co.candyhouse.sesame.ble.CHConfiguration
import co.utils.L
import co.utils.SharedPreferencesUtils


class CandyHouseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        L.d("hcia", "🌱:" + BuildConfig.BUILD_TYPE + BuildConfig.VERSION_NAME + BuildConfig.GIT_HASH)
        CHBleManager(this)
        CHConfiguration("SNUrRpqs9P8fFQJfmX94e1if1XriRw7G3uLVMqkK", "ap-northeast-1:0a1820f1-dbb3-4bca-9227-2a92f6abf0ae")
        SharedPreferencesUtils.init(this)
    }
}

