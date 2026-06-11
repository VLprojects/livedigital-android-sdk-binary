package space.livedigital.example.calls.entities

import android.os.Build
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.telecom.Connection
import android.telecom.DisconnectCause
import androidx.annotation.RequiresApi
import space.livedigital.example.calls.utils.CallConverter

internal class CallConnection(private val call: Call) : Connection() {

    private val listeners = mutableListOf<CallStateListener>()

    fun addListener(listener: CallStateListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: CallStateListener) {
        listeners.remove(listener)
    }

    override fun onDisconnect() {
        super.onDisconnect()
        listeners.forEach {
            it.onDisconnect(
                call = call,
                disconnectCause = DisconnectCause(DisconnectCause.LOCAL)
            )
        }
    }

    override fun onAnswer() {
        super.onAnswer()
        listeners.forEach {
            it.onAnswer(
                call = call
            )
        }
    }

    override fun onAnswer(videoState: Int) {
        super.onAnswer(videoState)
        listeners.forEach {
            it.onAnswer(
                call = call
            )
        }
    }

    override fun onReject() {
        super.onReject()
        listeners.forEach {
            it.onDisconnect(
                call = call,
                disconnectCause = DisconnectCause(DisconnectCause.REJECTED)
            )
        }
    }

    override fun onReject(rejectReason: Int) {
        super.onReject(rejectReason)
        listeners.forEach {
            it.onDisconnect(
                call = call,
                disconnectCause = DisconnectCause(DisconnectCause.REJECTED)
            )
        }
    }

    override fun onReject(replyMessage: String?) {
        super.onReject(replyMessage)
        listeners.forEach {
            it.onDisconnect(
                call = call,
                disconnectCause = DisconnectCause(DisconnectCause.REJECTED)
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onCallAudioStateChanged(state: CallAudioState?) {
        super.onCallAudioStateChanged(state)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // In that case onAvailableCallEndpointsChanged and onCallEndpointChanged will do the
            // work
            return
        }

        state?.let { state ->
            val mask = state.supportedRouteMask
            val availableRoutes = mutableListOf<Int>()

            if (mask and CallAudioState.ROUTE_EARPIECE != 0) {
                availableRoutes.add(CallAudioState.ROUTE_EARPIECE)
            }
            if (mask and CallAudioState.ROUTE_SPEAKER != 0) {
                availableRoutes.add(CallAudioState.ROUTE_SPEAKER)
            }
            if (mask and CallAudioState.ROUTE_WIRED_HEADSET != 0) {
                availableRoutes.add(CallAudioState.ROUTE_WIRED_HEADSET)
            }
            if (mask and CallAudioState.ROUTE_BLUETOOTH != 0) {
                availableRoutes.add(CallAudioState.ROUTE_BLUETOOTH)
            }

            val endpoints = availableRoutes.map { route ->
                CallConverter.convertRouteToGeneralEndpoint(route)
            }
            listeners.forEach {
                it.onCallEndpointsChanged(endpoints)
            }

            val currentEndpoint = endpoints.find {
                (it.rawEndpoint as Int) == state.route
            } ?: return
            listeners.forEach {
                it.onCallEndpointChanged(currentEndpoint)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCallEndpointChanged(callEndpoint: CallEndpoint) {
        super.onCallEndpointChanged(callEndpoint)
        val endpoint = CallConverter.convertCallEndpointToGeneralEndpoint(callEndpoint)
        listeners.forEach {
            it.onCallEndpointChanged(endpoint)
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onAvailableCallEndpointsChanged(availableEndpoints: List<CallEndpoint?>) {
        super.onAvailableCallEndpointsChanged(availableEndpoints)
        val endpoints = availableEndpoints.mapNotNull {
            it ?: return@mapNotNull null
            CallConverter.convertCallEndpointToGeneralEndpoint(it)
        }
        listeners.forEach {
            it.onCallEndpointsChanged(endpoints)
        }
    }

    interface CallStateListener {

        fun onAnswer(call: Call)

        fun onDisconnect(call: Call, disconnectCause: DisconnectCause)

        fun onCallEndpointChanged(callEndpoint: GeneralCallEndpoint)

        fun onCallEndpointsChanged(callEndpoints: List<GeneralCallEndpoint>)
    }
}