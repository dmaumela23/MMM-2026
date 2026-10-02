package com.maumela.magnummanagement.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.maumela.magnummanagement.MmmApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Receives Firebase events.
 *  - onNewToken: Firebase issued a new device token. It is sent to the MMM server (PUT /api/settings)
 *    only when someone is logged in and notifications are switched on.
 *  - onMessageReceived: a message arrived while the app is open (when the app is in the background,
 *    Android shows Console "Notification" messages itself).
 *
 * The prototype only receives test messages sent from the Firebase Console. Sending
 * notifications automatically from the server (Firebase Admin SDK) is deliberately not implemented.
 */
class MmmMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val container = (application as MmmApplication).container
        scope.launch {
            val loggedIn = container.tokenStore.load() != null
            val enabled = container.preferencesStore.notificationsEnabled.first()
            if (loggedIn && enabled) container.settingsRepository.registerFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "MMM"
        val body = message.notification?.body ?: message.data["body"] ?: return
        NotificationChannels.show(this, title, body)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
