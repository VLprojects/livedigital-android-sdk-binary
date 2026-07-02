package space.livedigital.example.calls.entities

data class AudioState(
    val availableEndpoints: List<GeneralCallEndpoint> = emptyList(),
    val currentEndpoint: GeneralCallEndpoint? = null
)