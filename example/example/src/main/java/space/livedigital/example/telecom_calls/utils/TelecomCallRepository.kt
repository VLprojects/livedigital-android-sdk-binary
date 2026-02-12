package space.livedigital.example.telecom_calls.utils

object TelecomCallRepository {

    private val observers = mutableListOf<CallObserver>()
    private var currentConnection: CallConnection? = null

    fun addObserver(observer: CallObserver) {
        observers.add(observer)
    }

    fun removeObserver(observer: CallObserver) {
        observers.remove(observer)
    }

    fun clearObservers() {
        observers.clear()
    }

    fun initializeConnection(connection: CallConnection) {
        currentConnection = connection
        currentConnection?.setInitializing()
    }

    fun endCall() {
        val call = currentConnection?.call ?: return
        observers.forEach {
            it.onCallEnded(call)
        }
        currentConnection?.destroy()
        currentConnection = null
    }

    interface CallObserver {

        fun onCallEnded(call: Call)
    }
}