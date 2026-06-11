package space.livedigital.example.calls.entities

sealed interface CallCommand {

    data class ChangeEndpoint(val endpoint: GeneralCallEndpoint) : CallCommand
}