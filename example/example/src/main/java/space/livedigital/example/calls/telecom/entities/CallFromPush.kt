package space.livedigital.example.calls.telecom.entities

data class CallFromPush(
    val id: String,
    val caller: String,
    val callerNumber: String,
    val roomAlias: String,
)