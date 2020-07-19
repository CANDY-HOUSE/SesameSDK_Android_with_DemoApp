package com.victor.library.wheelview.mode

import android.graphics.Paint

/**
 * Created by Victor on 2017/7/20.
 */
class WheelViewRecycleMode(override var eachItemHeight: Int,
                           override var childrenSize: Int) : IWheelViewMode {

    override fun getSelectedIndex(baseIndex: Int): Int {
        var index = baseIndex
        while (index < 0) {
            index += childrenSize
        }
        return index % childrenSize
    }

    override fun getTopMaxScrollHeight(): Int {
        return Int.MIN_VALUE
    }

    override fun getBottomMaxScrollHeight(): Int {
        return Int.MAX_VALUE
    }

    override fun getTextDrawY(height: Int, index: Int, paint: Paint): Float {
        return (getCenterY(height, paint) + index * eachItemHeight)
    }
}