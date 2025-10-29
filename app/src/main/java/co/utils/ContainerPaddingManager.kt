package co.utils

import androidx.fragment.app.Fragment

/**
 * 引用计数
 *
 * @author frey on 2025/10/27
 */
object ContainerPaddingManager {
    private var clearPaddingRefCount = 0

    fun requestClearPadding(fragment: Fragment) {
        synchronized(this) {
            clearPaddingRefCount++
            if (clearPaddingRefCount == 1) {
                fragment.clearContainerTopPadding()
            }
        }
    }

    fun releaseClearPadding(fragment: Fragment) {
        synchronized(this) {
            clearPaddingRefCount--
            if (clearPaddingRefCount == 0) {
                fragment.restoreContainerTopPadding()
            }
            // 防止计数错误
            if (clearPaddingRefCount < 0) {
                clearPaddingRefCount = 0
            }
        }
    }
}