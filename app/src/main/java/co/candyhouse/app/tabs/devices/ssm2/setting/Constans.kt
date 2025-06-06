package co.candyhouse.app.tabs.devices.ssm2.setting

import androidx.viewbinding.ViewBinding
import co.candyhouse.app.R
import co.candyhouse.sesame.BaseFG

val secondSettingValue = arrayOf(0, 3, 5, 7, 10, 15, 30, 60, 60 * 2,60 * 3,60 * 4, 60 * 5, 60 * 10, 60 * 15, 60 * 30, 60 * 60)

fun <T : ViewBinding> BaseFG<T>.getSeconds(): List<String> {
    return try {
        listOf(
                getString(R.string.Off), getString(R.string.sec3), getString(R.string.sec5),
                getString(R.string.sec7), getString(R.string.sec10), getString(R.string.sec15),
                getString(R.string.sec30), getString(R.string.min1), getString(R.string.min2),
                getString(R.string.min3), getString(R.string.min4), getString(R.string.min5),
                getString(R.string.min10), getString(R.string.min15), getString(R.string.min30),
                getString(R.string.hr1)
        )
    } catch (e: IllegalStateException) {
        // 返回一个默认列表或空列表
        emptyList()
    }
}

fun findSettingIndexByValue(index: Int): Int {
    secondSettingValue.forEachIndexed { i, it ->
        if (it == index) {
            return i
        }
    }
    return -1
}


fun <T : ViewBinding> BaseFG<T>.findSettinStringByValue(index: Int): String {
    val settingIndex = findSettingIndexByValue(index)
    return try {
        getSeconds().getOrNull(settingIndex) ?: getString(R.string.Off)
    } catch (e: IllegalStateException) {
        // 返回一个默认值
        "Off"
    }
}


val opsSecondSettingValue = arrayOf(65535, 0, 1,2,3,4, 5, 7, 10, 15, 30, 60, 60 * 2,60 * 3,60 * 4, 60 * 5, 60 * 10, 60 * 15, 60 * 30, 60 * 60)

val remotenanoSecondSettingValue = arrayOf(0, 10).toList().toTypedArray()

fun<T : ViewBinding> BaseFG<T>.opsGetSeconds(): List<String> {
    return listOf(getString(R.string.immediately), getString(R.string.Off), getString(R.string.sec1),getString(R.string.sec2),getString(R.string.sec3),getString(R.string.sec4), getString(R.string.sec5), getString(R.string.sec7), getString(R.string.sec10), getString(R.string.sec15), getString(R.string.sec30), getString(R.string.min1), getString(R.string.min2),getString(R.string.min3),getString(R.string.min4), getString(R.string.min5), getString(R.string.min10), getString(R.string.min15), getString(R.string.min30), getString(R.string.hr1))
}

fun opsFindSettingIndexByValue(index: Int): Int {
    opsSecondSettingValue.forEachIndexed { i, it ->
        if (it == index) {
            return i
        }
    }
    return -1
}

fun<T : ViewBinding> BaseFG<T>.opsFindSettinStringByValue(index: Int): String {
    val settingIndex = opsFindSettingIndexByValue(index)
    val secondsList = opsGetSeconds()
    return if (settingIndex in secondsList.indices) {
        secondsList[settingIndex]
    } else {
        secondsList[0]
    }
}