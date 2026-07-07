package space.livedigital.example.calls.push

/**
 * Optional hook for reacting to FCM token refreshes from [PushNotificationService], which must stay
 * decoupled from any backend. An app binds an implementation in Koin (e.g. calls-push re-registers
 * the device); apps that don't bind one get no-op behaviour.
 */
fun interface PushTokenListener {
    fun onNewToken(token: String)
}
