package co.candyhouse.sesame.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData

/**
 * 一次性事件包装类
 * 用于解决 LiveData 粘性问题，确保事件只被消费一次
 */
class Event<out T>(private val content: T) {
    private var hasBeenHandled = false

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    fun peekContent(): T = content
}

/**
 * LiveData<Event<T>> 扩展函数
 */
inline fun <T> LiveData<Event<T>>.observeEvent(
    owner: LifecycleOwner,
    crossinline onEventUnhandled: (T) -> Unit
) {
    observe(owner) { event ->
        event.getContentIfNotHandled()?.let(onEventUnhandled)
    }
}