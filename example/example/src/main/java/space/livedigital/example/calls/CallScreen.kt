package space.livedigital.example.calls

import android.Manifest
import android.telecom.DisconnectCause
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay
import space.livedigital.example.R
import space.livedigital.example.calls.entities.CallAction
import space.livedigital.example.calls.entities.CallState

@Composable
internal fun CallScreen(
    state: State<ScreenState>,
    onCallFinished: () -> Unit
) {
    when (val callState = state.value.callState) {
        is CallState.Unregistered, CallState.None -> {
            // If there is no call invoke finish after a small delay
            LaunchedEffect(Unit) {
                delay(1500)
                onCallFinished()
            }
            // Show call ended when there is no active call
            NoCallScreen()
        }

        is CallState.Registered -> {
            CallScreenContent(
                name = callState.callAttributes.displayName.toString(),
                info = callState.callAttributes.address.schemeSpecificPart,
                incoming = callState.isIncoming(),
                isActive = callState.isActive,
                isMuted = callState.isMuted,
                errorCode = callState.errorCode,
                onCallAction = callState::processAction,
            )
        }
    }
}

@Composable
private fun NoCallScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Call ended", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun CallScreenContent(
    name: String,
    info: String,
    incoming: Boolean,
    isActive: Boolean,
    isMuted: Boolean,
    errorCode: Int?,
    onCallAction: (CallAction) -> Unit,
) {
    if(errorCode != null) {
        Toast.makeText(LocalContext.current, "errorCode=($errorCode)", Toast.LENGTH_SHORT).show()
    }

    Column(
        Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CallInfoCard(name, info, isActive)
        if (incoming && !isActive) {
            IncomingCallActions(onCallAction)
        } else {
            OngoingCallActions(
                isMuted = isMuted,
                onCallAction = onCallAction,
            )
        }
    }
}

@Composable
private fun CallInfoCard(name: String, info: String, isActive: Boolean) {
    Column(
        Modifier
            .fillMaxSize(0.5f),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(imageVector = Icons.Rounded.Person, contentDescription = null)
        Text(text = name, style = MaterialTheme.typography.titleMedium)
        Text(text = info, style = MaterialTheme.typography.bodyMedium)

        if (!isActive) {
            Text(text = "Connecting...", style = MaterialTheme.typography.titleSmall)
        } else {
            Text(text = "Connected", style = MaterialTheme.typography.titleSmall)
        }

    }
}

@Composable
private fun IncomingCallActions(onCallAction: (CallAction) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(26.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        FloatingActionButton(
            onClick = {
                onCallAction(
                    CallAction.Disconnect(
                        DisconnectCause(
                            DisconnectCause.REJECTED,
                        ),
                    ),
                )
            },
            containerColor = MaterialTheme.colorScheme.error,
        ) {
            Icon(
                imageVector = Icons.Rounded.Call,
                contentDescription = null,
                modifier = Modifier.rotate(90f),
            )
        }
        FloatingActionButton(
            onClick = {
                onCallAction(
                    CallAction.Answer,
                )
            },
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(imageVector = Icons.Rounded.Call, contentDescription = null)
        }
    }
}

@Composable
private fun OngoingCallActions(
    isMuted: Boolean,
    onCallAction: (CallAction) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .shadow(1.dp)
            .padding(26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CallControls(
            isMuted = isMuted,
            onCallAction = onCallAction,
        )
        FloatingActionButton(
            onClick = {
                onCallAction(
                    CallAction.Disconnect(
                        DisconnectCause(
                            DisconnectCause.LOCAL,
                        ),
                    ),
                )
            },
            containerColor = MaterialTheme.colorScheme.error,
        ) {
            Icon(
                imageVector = Icons.Rounded.Call,
                contentDescription = "Disconnect call",
                modifier = Modifier.rotate(90f),
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun CallControls(
    isMuted: Boolean,
    onCallAction: (CallAction) -> Unit,
) {
    val micPermission = rememberPermissionState(permission = Manifest.permission.RECORD_AUDIO)
    var showRationale by remember(micPermission.status) {
        mutableStateOf(false)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (micPermission.status.isGranted) {
            IconToggleButton(
                checked = isMuted,
                onCheckedChange = {
                    onCallAction(CallAction.ToggleMute(it))
                },
            ) {
                if (isMuted) {
                    Icon(painter = painterResource(R.drawable.ic_mic_off_24), contentDescription = "Mic on")
                } else {
                    Icon(painter = painterResource(R.drawable.ic_mic_24), contentDescription = "Mic off")
                }
            }
        } else {
            IconButton(
                onClick = {
                    if (micPermission.status.shouldShowRationale) {
                        showRationale = true
                    } else {
                        micPermission.launchPermissionRequest()
                    }
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_mic_off_24),
                    contentDescription = "Missing mic permission",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    if (showRationale) {
        RationaleMicDialog(
            onResult = { request ->
                if (request) {
                    micPermission.launchPermissionRequest()
                }
                showRationale = false
            },
        )
    }
}

@Composable
private fun RationaleMicDialog(onResult: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = { onResult(false) },
        confirmButton = {
            TextButton(onClick = { onResult(true) }) {
                Text(text = "Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = { onResult(false) }) {
                Text(text = "Cancel")
            }
        },
        title = {
            Text(text = "Mic permission required")
        },
        text = {
            Text(text = "In order to speak in a call we need mic permission. Please press continue and grant the permission in the next dialog.")
        },
    )
}