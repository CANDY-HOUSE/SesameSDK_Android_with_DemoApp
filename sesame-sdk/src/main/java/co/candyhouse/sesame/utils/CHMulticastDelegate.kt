package co.candyhouse.sesame.utils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Collections

open class CHMulticastDelegate<T> {
    private val delegates = Collections.synchronizedSet(HashSet<T>())
    private val defaultDispatcher = Dispatchers.Default

    constructor(strongReference: Boolean = false) {}

    fun addDelegate(delegate: T, dispatcher: CoroutineDispatcher = defaultDispatcher) {
        CoroutineScope(dispatcher).launch {
            delegates.add(delegate)
        }
    }

    fun removeDelegate(delegate: T, dispatcher: CoroutineDispatcher = defaultDispatcher) {
        CoroutineScope(dispatcher).launch {
            delegates.remove(delegate)
        }
    }

    fun removeAll(dispatcher: CoroutineDispatcher = defaultDispatcher) {
        CoroutineScope(dispatcher).launch {
            delegates.clear()
        }
    }

    fun invokeDelegates(invocation: (T) -> Unit, dispatcher: CoroutineDispatcher = defaultDispatcher) {
        CoroutineScope(dispatcher).launch {
            delegates.forEach { delegate ->
                invocation(delegate)
            }
        }
    }
    fun invokeAll(action: suspend T.() -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            delegates.forEach { delegate ->
                delegate.action()
            }
        }
    }

    fun containsDelegate(delegate: T, dispatcher: CoroutineDispatcher = defaultDispatcher): Boolean {
        var contains = false
        runBlocking(dispatcher) {
            contains = delegates.contains(delegate)
        }
        return contains
    }
}
