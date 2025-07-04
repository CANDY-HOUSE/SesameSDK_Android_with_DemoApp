package candyhouse.sesameos.ir.ext

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RawRes
import candyhouse.sesameos.ir.R
import candyhouse.sesameos.ir.adapter.BeanIcon
import candyhouse.sesameos.ir.base.IrCompanyCode
import candyhouse.sesameos.ir.base.EmIconType
import candyhouse.sesameos.ir.base.IrRemote
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.Serializable
import java.util.ArrayList
import java.util.UUID

object Ext {

    fun Float.dpToSp(resources: Resources): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this,
            resources.displayMetrics
        ) / resources.displayMetrics.scaledDensity
    }

    fun drawImg(item: BeanIcon, tvName: TextView, imgView: ImageView) {
        if (item.type == EmIconType.SVG) {
            tvName.visibility = View.GONE
            imgView.visibility = View.VISIBLE
            imgView.setImageResource(item.svg!!)
        } else {
            tvName.visibility = View.VISIBLE
            tvName.text = item.msg
            if (item.msg!!.length > 2) {
                val size = 22f - item.msg.length * 1.4f
                tvName.textSize = size.dpToSp(tvName.resources)
            } else {
                tvName.textSize = 22f.dpToSp(tvName.resources)
            }
            imgView.visibility = View.GONE
        }
    }

    private fun Char.isLetter(): Boolean {
        return this in 'A'..'Z' || this in 'a'..'z'
    }

    private fun extractLetter(input: String): String {
        val splits = input.split("(")
        for (split in splits) {
            if (split.isNotEmpty() && split[0].isLetter()) {
                return split[0].toUpperCase().toString()
            }
        }
        return "#"
    }

    fun parseXmlToDeviceList(context: Context,resId:Int, type:Int = 0): List<IrRemote> {
        val devices = mutableListOf<IrRemote>()

        try {
            // 从raw资源中读取完整的XML文件内容
            val inputStream = context.resources.openRawResource(resId)
            val xmlContent = inputStream.bufferedReader().use { it.readText() }

            // 使用正则表达式匹配每个完整的item标签
            val itemPattern = """<item\s+code="(\d+)">([^<]+)</item>""".toRegex()
            val matches = itemPattern.findAll(xmlContent)

            matches.forEach { match ->
                try {
                    val code = match.groupValues[1].toInt()
                    val fullName = match.groupValues[2].trim()

                    // 分割品牌和设备名
                    val parts = fullName.split(" ", limit = 2)
                    if (parts.size >= 2) {
                        val brand = parts[0]
                        val deviceName = parts[1]
                        val direction = brand.firstOrNull()?.toString() ?: ""
                        val item = IrRemote(
                            model = fullName,
                            alias = brand,
                            uuid = UUID.randomUUID().toString(),
                            state = "",
                            timestamp = 0L,
                            type = type,
                            code = code,

                            direction = direction,

                        )
                        devices.add(item)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return devices.sortedWith(compareBy<IrRemote> { it.direction }.thenBy { it.model })
    }

    fun parseJsonToDeviceList(context: Context, resId: Int, type: Int = 0): List<IrRemote> {
        val devices = mutableListOf<IrRemote>()
        try {
            val jsonString = context.resources.openRawResource(resId)
                .bufferedReader()
                .use { it.readText() }
            val gson = Gson()
            val listType = object : TypeToken<List<IrRemote>>() {}.type
            val irRemoteList: List<IrRemote> = gson.fromJson(jsonString, listType)

            irRemoteList.forEach { item ->
                try {
                    val newItem = item.copy(
                        uuid = UUID.randomUUID().toString().uppercase(),
                        timestamp = 0L,
                        type = type,
                    )
                    devices.add(newItem)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return devices
    }


    inline fun <reified T : Serializable> Bundle.getSerializableCompat(key: String): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getSerializable(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getSerializable(key) as? T
        }
    }

    inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelable(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelable(key)
        }
    }

    inline fun <reified T : Parcelable> Bundle.getParcelableArrayListCompat(key: String): ArrayList<T>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableArrayList(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableArrayList(key)
        }
    }

    fun parseCompanyTableToList(context: Context,resId: Int): List<IrCompanyCode> {
        val jsonString = context.resources.openRawResource(resId)
            .bufferedReader()
            .use { it.readText() }
        val gson = Gson()
        val listType = object : TypeToken<List<IrCompanyCode>>() {}.type
        return gson.fromJson(jsonString, listType)
    }

}