package space.livedigital.example.calls.entities

import space.livedigital.example.calls.constants.CallEndpointType

data class GeneralCallEndpoint(
    val id: String,
    val name: String,
    val type: CallEndpointType,
    val rawEndpoint: Any
)
