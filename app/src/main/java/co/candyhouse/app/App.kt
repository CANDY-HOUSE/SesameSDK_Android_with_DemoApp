package co.candyhouse.app

import android.app.Application
import android.widget.Toast
import androidx.preference.PreferenceManager
import co.candyhouse.sesame.open.CHBleManager
import co.utils.CrashUtils
import co.utils.L
import co.utils.SharedPreferencesUtils


class CandyHouseApp : Application() {
    companion object {
        @JvmStatic
        private var baseApp: CandyHouseApp? = null
        val app by lazy {
            baseApp!!
        }
    }
    override fun onCreate() {
        super.onCreate()
        L.d("hcia", "🌱:" + BuildConfig.BUILD_TYPE +":"+ BuildConfig.VERSION_NAME +":")
        CHBleManager(this)
        CrashUtils.instance?.init(this)

        SharedPreferencesUtils.init(PreferenceManager.getDefaultSharedPreferences(applicationContext))
        baseApp=this


    }
    fun  ts(msg:String){
        Toast.makeText(this,msg, Toast.LENGTH_SHORT).show()

    }
}
