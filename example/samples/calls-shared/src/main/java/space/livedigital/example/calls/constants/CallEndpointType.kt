package space.livedigital.example.calls.constants

enum class CallEndpointType(val rank: Int) {
    UNKNOWN(0),
    SPEAKER(1),
    EARPIECE(2),
    BLUETOOTH(3),
    WIRED_HEADSET(4);
}
