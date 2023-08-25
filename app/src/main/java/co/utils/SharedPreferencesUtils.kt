package co.utils

import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object SharedPreferencesUtils {
    lateinit var preferences: SharedPreferences

    fun init(sharedPreferences: SharedPreferences) {
        preferences = sharedPreferences
    }

    var nickname by SharedPreferenceDelegates.string(null)
    var isNeedFreshFriend by SharedPreferenceDelegates.boolean(false)
    var isNeedFreshDevice by SharedPreferenceDelegates.boolean(false)
    var deviceToken by SharedPreferenceDelegates.string()
    var isUploadDeveceToken by SharedPreferenceDelegates.boolean()
}

object SharedPreferenceDelegates {

    fun int(defaultValue: Int = 0) = object : ReadWriteProperty<SharedPreferencesUtils, Int> {
        override fun getValue(thisRef: SharedPreferencesUtils, property: KProperty<*>): Int {
            return thisRef.preferences.getInt(property.name, defaultValue)
        }

        override fun setValue(thisRef: SharedPreferencesUtils, property: KProperty<*>, value: Int) {
            thisRef.preferences.edit().putInt(property.name, value).apply()
        }
    }


    fun boolean(defaultValue: Boolean = false) =
        object : ReadWriteProperty<SharedPreferencesUtils, Boolean> {
            override fun getValue(thisRef: SharedPreferencesUtils, property: KProperty<*>): Boolean {
                return thisRef.preferences.getBoolean(property.name, defaultValue)
            }

            override fun setValue(thisRef: SharedPreferencesUtils, property: KProperty<*>, value: Boolean) {
                thisRef.preferences.edit().putBoolean(property.name, value).apply()
            }
        }


    fun string(defaultValue: String? = null) =
        object : ReadWriteProperty<SharedPreferencesUtils, String?> {
            override fun getValue(thisRef: SharedPreferencesUtils, property: KProperty<*>): String? {
                return thisRef.preferences.getString(property.name, defaultValue)
            }

            override fun setValue(thisRef: SharedPreferencesUtils, property: KProperty<*>, value: String?) {
                thisRef.preferences.edit().putString(property.name, value).apply()
            }
        }


}