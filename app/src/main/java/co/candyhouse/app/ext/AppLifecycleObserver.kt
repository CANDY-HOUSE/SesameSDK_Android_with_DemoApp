package co.candyhouse.app.ext

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import co.candyhouse.sesame.utils.L

/**
 * APP应用状态监听
 *
 * @author frey on 2025/2/17
 */
class AppLifecycleObserver : DefaultLifecycleObserver {
    var isAppForeground = false
        private set

    override fun onStart(owner: LifecycleOwner) {
        // 应用进入前台
        L.d("sf", "App is in the foreground")
        super.onStart(owner)
        isAppForeground = true
    }

    override fun onStop(owner: LifecycleOwner) {
        // 应用进入后台
        L.d("sf", "App is in the background")
        super.onStop(owner)
        isAppForeground = false
    }

}