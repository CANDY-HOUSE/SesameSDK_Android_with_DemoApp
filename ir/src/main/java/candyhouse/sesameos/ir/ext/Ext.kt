package candyhouse.sesameos.ir.ext

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.TypedValue
import candyhouse.sesameos.ir.base.IrRemote
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

}