package space.livedigital.example.calls

sealed interface ScreenEvent {

    data class CreateContact(val callerName: String, val phone: String) : ScreenEvent
}
