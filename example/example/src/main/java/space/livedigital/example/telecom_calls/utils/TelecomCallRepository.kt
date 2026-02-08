package space.livedigital.example.telecom_calls.utils

object TelecomCallRepository {

    private val observers = mutableListOf<CallObserver>()
    private var currentConnection: CallConnection? = null

    fun addObserver(observer: CallObserver) {
        observers.add(observer)
    }

    fun clearObservers() {
        observers.clear()
    }

    fun initializeConnection(connection: CallConnection) {
        currentConnection = connection
        currentConnection?.setInitializing()
    }

    fun endCall() {
        observers.forEach {
            it.onCallEnded()
        }
        currentConnection?.destroy()
        currentConnection = null
    }

    interface CallObserver {

        fun onCallEnded()
    }
}