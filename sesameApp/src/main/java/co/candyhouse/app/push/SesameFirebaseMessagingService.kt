package co.candyhouse.app.push

import co.candyhouse.app.R
import co.candyhouse.app.SesameApp
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class SesameFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        (application as SesameApp).subscriptionManager.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        (application as SesameApp).subscriptionManager.notifyChange()
        val data = message.data
        val announcement = data["messageType"] == "announcement"
        val title = if (announcement) data["title"] ?: message.notification?.title ?: getString(R.string.Sesame)
        else data["alertTitle"] ?: data["title"] ?: message.notification?.title ?: return
        PushNotification.show(
            this, title, data["body"] ?: message.notification?.body, data["url"],
            if (announcement) data["imageUrl"] ?: message.notification?.imageUrl?.toString() else null,
            data["messageAction"], announcement
        )
    }
}
