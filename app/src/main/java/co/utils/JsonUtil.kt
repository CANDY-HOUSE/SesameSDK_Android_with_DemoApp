package co.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.lang.reflect.ParameterizedType

/**
 * JSON解析工具类
 *
 * @author frey on 2025/1/21
 */
object JsonUtil {

    val gson: Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd HH:mm:ss")
        .create()

    fun <T> toJson(obj: T): String {
        return gson.toJson(obj)
    }

    inline fun <reified T> fromJson(json: String): T {
        return gson.fromJson(json, object : TypeToken<T>() {}.type)
    }

    inline fun <reified T> fromJsonList(json: String): ArrayList<T> {
        return gson.fromJson(json, object : TypeToken<ArrayList<T>>() {}.type)
    }

    fun <T> fromJson(json: String, clazz: Class<T>): T {
        return gson.fromJson(json, clazz)
    }

    inline fun <reified T> String.parseList(): ArrayList<T> {
        val type = object : ParameterizedType {
            override fun getRawType() = ArrayList::class.java
            override fun getActualTypeArguments() = arrayOf(T::class.java)
            override fun getOwnerType() = null
        }
        return gson.fromJson(this, type)
    }

}