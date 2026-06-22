package space.livedigital.example.calls.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import space.livedigital.example.calls.entities.CallType

internal fun Context.hasPermission(permission: String): Boolean =
    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

/**
 * Initial mute state for a call that is about to enter a media-capable state.
 * Without [Manifest.permission.RECORD_AUDIO] the call cannot capture audio, so it must start muted.
 */
internal fun Context.initialIsMuted(): Boolean =
    !hasPermission(Manifest.permission.RECORD_AUDIO)

/**
 * Initial camera state: on only for a video call that is actually allowed to use the camera.
 */
internal fun Context.initialIsCameraOn(callType: CallType): Boolean =
    callType == CallType.VIDEO && hasPermission(Manifest.permission.CAMERA)
