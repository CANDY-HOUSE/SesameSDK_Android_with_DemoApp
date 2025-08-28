package co.utils
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 点击防抖管理器
 */
object ClickDebouncedManager {
    private val lastClickTimestamps = ConcurrentHashMap<Int, Long>()
    private val debounceJobs = ConcurrentHashMap<Int, Job>()
    private val viewReferences = ConcurrentHashMap<Int, WeakReference<View>>()
    private val lifecycleOwnerReferences = ConcurrentHashMap<Int, WeakReference<LifecycleOwner>>()
    private val stateChangeListeners = ConcurrentHashMap<Int, View.OnAttachStateChangeListener>()
    private var cleanupJob: Job? = null
    private val isCleanupRunning = AtomicBoolean(false)

    private fun startCleanupTaskIfNeeded() {
        if (viewReferences.isEmpty()) return

        if (isCleanupRunning.compareAndSet(false, true)) {
            cleanupJob = CoroutineScope(Dispatchers.Default).launch {
                try {
                    while (isActive && !viewReferences.isEmpty()) {
                        cleanupGarbageCollectedViews()
                        delay(60000)
                    }
                } finally {
                    isCleanupRunning.set(false)
                }
            }
        }
    }

    private fun stopCleanupTaskIfEmpty() {
        if (viewReferences.isEmpty() && isCleanupRunning.get()) {
            cleanupJob?.cancel()
            cleanupJob = null
            isCleanupRunning.set(false)
        }
    }

    private fun cleanupGarbageCollectedViews() {
        val viewIdsToRemove = mutableListOf<Int>()
        viewReferences.forEach { (viewId, viewRef) ->
            if (viewRef.get() == null) {
                viewIdsToRemove.add(viewId)
            }
        }
        viewIdsToRemove.forEach { viewId ->
            cleanUpViewResources(viewId)
        }
        stopCleanupTaskIfEmpty()
    }

    private fun cleanUpViewResources(viewId: Int) {
        debounceJobs[viewId]?.cancel()
        debounceJobs.remove(viewId)
        lastClickTimestamps.remove(viewId)
        viewReferences.remove(viewId)
        lifecycleOwnerReferences.remove(viewId)
        stateChangeListeners.remove(viewId)
    }

    private fun checkAndRecordClick(viewId: Int, debounceTime: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        val lastTime = lastClickTimestamps[viewId] ?: 0L
        return if (currentTime - lastTime > debounceTime) {
            lastClickTimestamps[viewId] = currentTime
            true
        } else {
            false
        }
    }

    internal fun setupClickDebounce(
        view: View,
        debounceTime: Long,
        lifecycleOwner: LifecycleOwner?,
        onDebounceEnd: (() -> Unit)?,
        action: (View) -> Unit
    ) {
        if (view.id == View.NO_ID) {
            view.id = View.generateViewId()
        }

        val viewId = view.id
        viewReferences[viewId] = WeakReference(view)
        startCleanupTaskIfNeeded()

        lifecycleOwner?.let {
            lifecycleOwnerReferences[viewId] = WeakReference(it)
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY) {
                    cleanUpViewResources(viewId)
                    stopCleanupTaskIfEmpty()
                }
            }
            it.lifecycle.addObserver(observer)
        }

        val stateChangeListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}
            override fun onViewDetachedFromWindow(v: View) {
                cleanUpViewResources(viewId)
                v.removeOnAttachStateChangeListener(this)
                stopCleanupTaskIfEmpty()
            }
        }

        stateChangeListeners[viewId]?.let { view.removeOnAttachStateChangeListener(it) }
        view.addOnAttachStateChangeListener(stateChangeListener)
        stateChangeListeners[viewId] = stateChangeListener

        view.setOnClickListener { v ->
            if (checkAndRecordClick(viewId, debounceTime)) {
                action(v)
                onDebounceEnd?.let { callback ->
                    debounceJobs[viewId]?.cancel()
                    debounceJobs[viewId] = CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
                        delay(debounceTime)
                        if (viewReferences[viewId]?.get() != null) {
                            callback()
                        }
                    }
                }
            }
        }
    }
}

/**
 * View的防抖点击扩展函数
 */
fun View.setDebouncedClickListener(
    debounceTime: Long = 400L,
    lifecycleOwner: LifecycleOwner? = null,
    onDebounceEnd: (() -> Unit)? = null,
    action: (View) -> Unit
) {
    ClickDebouncedManager.setupClickDebounce(
        view = this,
        debounceTime = debounceTime,
        lifecycleOwner = lifecycleOwner,
        onDebounceEnd = onDebounceEnd,
        action = action
    )
}

fun View.setDebouncedClickListener(
    view: View = this,
    debounceTime: Long = 400L,
    lifecycleOwner: LifecycleOwner? = null,
    onDebounceEnd: (() -> Unit)? = null,
    action: (View) -> Unit
) {
    ClickDebouncedManager.setupClickDebounce(
        view = view,
        debounceTime = debounceTime,
        lifecycleOwner = lifecycleOwner,
        onDebounceEnd = onDebounceEnd,
        action = action
    )
}

/**
 * View的防抖点击扩展函数 (简化版)
 */
fun View.setDebouncedClick(
    debounceTime: Long = 400L,
    action: (View) -> Unit
) {
    setDebouncedClickListener(
        debounceTime = debounceTime,
        action = action
    )
}
