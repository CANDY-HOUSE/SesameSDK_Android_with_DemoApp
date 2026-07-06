package co.candyhouse.app.ext

import android.os.Handler
import android.os.Looper
import co.candyhouse.sesame.server.CHAPIClientBiz
import co.candyhouse.sesame.server.dto.AppPromotion
import co.candyhouse.sesame.utils.L
import java.util.concurrent.CopyOnWriteArraySet

/**
 * App推广活动红点状态
 *
 * @author frey on 2026/7/6
 */
object AppPromotionManager {

    private const val tag = "AppPromotionManager"
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArraySet<(AppPromotion?) -> Unit>()

    @Volatile
    private var currentPromotion: AppPromotion? = null

    fun addListener(listener: (AppPromotion?) -> Unit) {
        listeners.add(listener)
        listener(currentPromotion)
    }

    fun removeListener(listener: (AppPromotion?) -> Unit) {
        listeners.remove(listener)
    }

    fun refresh(onComplete: ((AppPromotion?) -> Unit)? = null) {
        CHAPIClientBiz.getActivePromotion { result ->
            result.fold(
                onSuccess = { state ->
                    currentPromotion = state.data
                    notifyChanged()
                    runOnMain { onComplete?.invoke(state.data) }
                },
                onFailure = { error ->
                    L.e(tag, "refresh failed: ${error.message}")
                    runOnMain { onComplete?.invoke(currentPromotion) }
                }
            )
        }
    }

    fun markRead(
        promotionId: String,
        targetUrl: String?,
        onComplete: ((AppPromotion?) -> Unit)? = null
    ) {
        val basePromotion = currentPromotion?.takeIf { it.promotionId == promotionId }
            ?: AppPromotion(promotionId = promotionId, targetUrl = targetUrl.orEmpty())
        val hiddenPromotion = basePromotion.copy(
            visible = false,
            targetUrl = targetUrl ?: basePromotion.targetUrl
        )
        currentPromotion = hiddenPromotion
        notifyChanged()

        CHAPIClientBiz.markPromotionRead(promotionId, targetUrl) { result ->
            result.fold(
                onSuccess = { state ->
                    currentPromotion = state.data
                    notifyChanged()
                    runOnMain { onComplete?.invoke(state.data) }
                },
                onFailure = { error ->
                    L.e(tag, "markRead failed: ${error.message}")
                    runOnMain { onComplete?.invoke(hiddenPromotion) }
                }
            )
        }
    }

    private fun notifyChanged() {
        val promotion = currentPromotion
        runOnMain {
            listeners.forEach { it.invoke(promotion) }
        }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }
}
