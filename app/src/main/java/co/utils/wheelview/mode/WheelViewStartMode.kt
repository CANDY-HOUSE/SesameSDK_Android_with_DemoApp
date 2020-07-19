package com.victor.library.wheelview.mode

import android.graphics.Paint

/**
 * Created by Victor on 2017/7/20.
 */
class WheelViewStartMode(override var eachItemHeight: Int,
                         override var childrenSize: Int) : IWheelViewMode {

    override fun getSelectedIndex(baseIndex: Int): Int {
        return baseIndex
    }

    override fun getTopMaxScrollHeight(): Int {
        return 0
    }

    override fun getBottomMaxScrollHeight(): Int {
        return eachItemHeight * (childrenSize - 1)
    }

    override fun getTextDrawY(height: Int, index: Int, paint: Paint): Float {
        return (getCenterY(height, paint) + index * eachItemHeight)
    }
}