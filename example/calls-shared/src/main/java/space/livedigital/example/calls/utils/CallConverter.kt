package space.livedigital.example.calls.utils

import android.os.Build
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallEndpointCompat
import space.livedigital.example.calls.constants.CallEndpointType
import space.livedigital.example.calls.entities.CallType
import space.livedigital.example.calls.entities.GeneralCallEndpoint

object CallConverter {
    fun convertToGeneralEndpointType(callEndpointType: Int): CallEndpointType {
        return when (callEndpointType) {
            CallEndpointCompat.TYPE_EARPIECE -> CallEndpointType.EARPIECE
            CallEndpointCompat.TYPE_SPEAKER -> CallEndpointType.SPEAKER
            CallEndpointCompat.TYPE_WIRED_HEADSET -> CallEndpointType.WIRED_HEADSET
            CallEndpointCompat.TYPE_BLUETOOTH -> CallEndpointType.BLUETOOTH
            else -> CallEndpointType.UNKNOWN
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun convertCallEndpointToGeneralEndpoint(callEndpoint: CallEndpoint): GeneralCallEndpoint {
        val type = when (callEndpoint.endpointType) {
            CallEndpoint.TYPE_EARPIECE -> CallEndpointType.EARPIECE
            CallEndpoint.TYPE_SPEAKER -> CallEndpointType.SPEAKER
            CallEndpoint.TYPE_WIRED_HEADSET -> CallEndpointType.WIRED_HEADSET
            CallEndpoint.TYPE_BLUETOOTH -> CallEndpointType.BLUETOOTH
            else -> CallEndpointType.UNKNOWN
        }

        return GeneralCallEndpoint(
            id = callEndpoint.identifier.toString(),
            name = callEndpoint.endpointName.toString(),
            type = type,
            rawEndpoint = callEndpoint
        )
    }

    fun convertRouteToGeneralEndpoint(route: Int): GeneralCallEndpoint {
        val (name, type) = when (route) {
            CallAudioState.ROUTE_EARPIECE -> "Phone Earpiece" to CallEndpointType.EARPIECE
            CallAudioState.ROUTE_SPEAKER -> "Speakerphone" to CallEndpointType.SPEAKER
            CallAudioState.ROUTE_WIRED_HEADSET -> "Wired Headset" to CallEndpointType.WIRED_HEADSET
            CallAudioState.ROUTE_BLUETOOTH -> "Bluetooth Device" to CallEndpointType.BLUETOOTH
            else -> "Unknown" to CallEndpointType.UNKNOWN
        }

        return GeneralCallEndpoint(
            id = "route_$route",
            name = name,
            type = type,
            rawEndpoint = route
        )
    }

    fun convertToTelecomCallType(callType: CallType): Int {
        return if (callType == CallType.VIDEO) {
            CallAttributesCompat.CALL_TYPE_VIDEO_CALL
        } else {
            CallAttributesCompat.CALL_TYPE_AUDIO_CALL
        }
    }
}
