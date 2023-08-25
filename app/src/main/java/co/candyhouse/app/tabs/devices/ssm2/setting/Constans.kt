package co.candyhouse.app.tabs.devices.ssm2.setting

import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG

val secondSettingValue = arrayOf(0, 3, 5, 7, 10, 15, 30, 60, 60 * 2, 60 * 5, 60 * 10, 60 * 15, 60 * 30, 60 * 60)
fun BaseDeviceFG.getSeconds(): List<String> {
    return listOf(getString(R.string.Off), getString(R.string.sec3), getString(R.string.sec5), getString(R.string.sec7), getString(R.string.sec10), getString(R.string.sec15), getString(R.string.sec30), getString(R.string.min1), getString(R.string.min2), getString(R.string.min5), getString(R.string.min10), getString(R.string.min15), getString(R.string.min30), getString(R.string.hr1))
}


fun BaseDeviceFG.findSettingIndexByValue(index:Int): Int {
    secondSettingValue.forEachIndexed { i, it ->
        if (it == index) {
            return i
        }
    }
    return -1
}

fun BaseDeviceFG.findSettinStringByValue(index:Int): String {
  return  getSeconds()[findSettingIndexByValue(index)]
}