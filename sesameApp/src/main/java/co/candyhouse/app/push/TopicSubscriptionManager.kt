package co.candyhouse.app.push

import android.content.Context
import androidx.core.content.edit
import co.candyhouse.app.util.AppIdentity
import co.candyhouse.sesame.utils.L
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Exposes the FCM token; Biz owns registration and subscription requests. */
class TopicSubscriptionManager(context: Context) {
    private val prefs = context.getSharedPreferences("push_subscription", Context.MODE_PRIVATE)
    val installationId = AppIdentity.get(context)
    private val events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val changes = events.asSharedFlow()

    fun refreshToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener(::onNewToken)
    }

    fun onNewToken(token: String) {
        L.d("SesamePush", "appIdentifyId=$installationId")
        L.d("SesamePush", "FCM token=$token")
        val changed = prefs.getString("token", null) != token
        prefs.edit { putString("token", token) }
        if (changed) events.tryEmit(Unit)
    }

    suspend fun token(): String = suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            if (continuation.isActive) continuation.resume(it)
        }.addOnFailureListener {
            if (continuation.isActive) continuation.resumeWithException(it)
        }
    }

    fun notifyChange() {
        events.tryEmit(Unit)
    }
}
