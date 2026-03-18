package space.livedigital.example.ui.screens

import android.Manifest
import android.telecom.DisconnectCause
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import space.livedigital.example.R
import space.livedigital.example.calls.ScreenState
import space.livedigital.example.calls.entities.CallAction
import space.livedigital.example.calls.entities.CallState
import space.livedigital.example.ui.components.buttons.ButtonComponent
import space.livedigital.example.ui.components.containers.ContainerComponent
import space.livedigital.example.ui.extensions.gradientBackground
import space.livedigital.example.ui.theme.AppTheme
import space.livedigital.sdk.channel.ChannelSessionStatus
import kotlin.time.Duration

@Composable
internal fun CallScreen(
    state: State<ScreenState>,
    onCallFinished: () -> Unit,
    onCallAction: (CallAction) -> Unit
) {
    val state = state.value

    when (val callState = state.callState) {
        is CallState.Active -> {
            ActiveCallContentComponent(
                callState = callState,
                sessionStatus = state.sessionStatus,
                callDuration = state.callDuration,
                onCallAction = onCallAction
            )
        }

        is CallState.Answered -> {
            AnsweredCallContentComponent(callState = callState)
        }

        is CallState.Outgoing -> {
            OutgoingCallContentComponent(callState = callState, onCallAction = onCallAction)
        }

        is CallState.Ended -> {
            EndedCallContentComponent(
                callState = callState,
                onCallAction = onCallAction,
                onCallFinished = onCallFinished
            )
        }

        is CallState.Incoming -> {
            IncomingCallContentComponent(callState = callState, onCallAction = onCallAction)
        }

        is CallState.Missed -> {
            MissedCallContentComponent(
                callState = callState,
                onCallAction = onCallAction,
                onCallFinished = onCallFinished
            )
        }


        CallState.Idle -> {
            return
        }
    }
}

@Composable
private fun ActiveCallContentComponent(
    callState: CallState.Active,
    sessionStatus: ChannelSessionStatus,
    callDuration: Duration,
    onCallAction: (CallAction) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .gradientBackground(
                listOf(
                    AppTheme.colorSystem.accent02,
                    AppTheme.colorSystem.accent01
                )
            )
            .safeDrawingPadding()
            .padding(vertical = 24.dp, horizontal = 16.dp)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp)
        )

        ContainerComponent(
            contentPadding = PaddingValues(
                horizontal = 32.dp,
                vertical = 16.dp
            )
        ) {
            Text(text = callState.call.displayName)
        }

        if (sessionStatus != ChannelSessionStatus.STARTED) {
            ContainerComponent(
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 8.dp)
            ) {
                Text(stringResource(R.string.label_connection))
            }
        } else if (callDuration != Duration.ZERO) {
            ContainerComponent(
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 8.dp)
            ) {
                Text(formatDuration(callDuration))
            }
        }

        Spacer(modifier = Modifier.weight(1.0f))

        ContainerComponent(contentPadding = PaddingValues(all = 12.dp)) {
            Row {
                val microphonePermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    if (isGranted) {
                        onCallAction(CallAction.ToggleMute(isMute = !callState.isMuted))
                    } else {
                        // Разрешение отклонено, можно показать Toast / SnackBar
                    }
                }

                ButtonComponent(
                    onClick = {
                        microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    style = if (callState.isMuted) {
                        AppTheme.buttonSystem.tertiaryButtonStyle
                    } else {
                        AppTheme.buttonSystem.primaryButtonStyle
                    },
                    icon = ImageVector.vectorResource(
                        if (callState.isMuted) {
                            R.drawable.ic_microphone_off
                        } else {
                            R.drawable.ic_microphone_on
                        }
                    )
                )

                Spacer(modifier = Modifier.weight(1.0f))

                ButtonComponent(
                    onClick = {
                        onCallAction(
                            CallAction.Disconnect(
                                displayName = callState.call.displayName,
                                phone = callState.call.phone,
                                roomAlias = callState.call.roomAlias,
                                cause = DisconnectCause(DisconnectCause.LOCAL)
                            )
                        )
                    },
                    style = AppTheme.buttonSystem.rejectButtonStyle,
                    icon = ImageVector.vectorResource(R.drawable.ic_close_mini)
                )
            }
        }
    }
}

@Composable
private fun AnsweredCallContentComponent(callState: CallState.Answered) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .gradientBackground(
                listOf(
                    AppTheme.colorSystem.accent02,
                    AppTheme.colorSystem.accent01
                )
            )
            .safeDrawingPadding()
            .padding(vertical = 24.dp, horizontal = 16.dp)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp)
        )

        ContainerComponent(
            contentPadding = PaddingValues(
                horizontal = 32.dp,
                vertical = 16.dp
            )
        ) {
            Text(text = callState.call.displayName)
        }

        Spacer(modifier = Modifier.weight(1.0f))
    }
}

@Composable
private fun OutgoingCallContentComponent(
    callState: CallState.Outgoing,
    onCallAction: (CallAction) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .gradientBackground(
                listOf(
                    AppTheme.colorSystem.accent02,
                    AppTheme.colorSystem.accent01
                )
            )
            .safeDrawingPadding()
            .padding(vertical = 24.dp, horizontal = 16.dp)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp)
        )

        ContainerComponent(
            contentPadding = PaddingValues(
                horizontal = 32.dp,
                vertical = 16.dp
            )
        ) {
            Image(
                imageVector = ImageVector.vectorResource(R.drawable.ic_logo),
                contentDescription = null
            )

            Text(text = callState.call.displayName)
        }

        ContainerComponent(
            contentPadding = PaddingValues(
                horizontal = 32.dp,
                vertical = 11.5.dp
            )
        ) {
            Text(stringResource(R.string.label_call))
        }

        Spacer(modifier = Modifier.weight(1.0f))

        ContainerComponent(contentPadding = PaddingValues(all = 12.dp)) {
            Row {
                val microphonePermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    if (isGranted) {
                        onCallAction(
                            CallAction.ToggleMute(isMute = !callState.isMuted)
                        )
                    }
                }

                ButtonComponent(
                    onClick = {
                        microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    style = if (callState.isMuted) {
                        AppTheme.buttonSystem.tertiaryButtonStyle
                    } else {
                        AppTheme.buttonSystem.primaryButtonStyle
                    },
                    icon = if (callState.isMuted) {
                        ImageVector.vectorResource(R.drawable.ic_microphone_off)
                    } else {
                        ImageVector.vectorResource(R.drawable.ic_microphone_on)
                    }
                )

                Spacer(modifier = Modifier.weight(1.0f))

                ButtonComponent(
                    onClick = {
                        onCallAction(
                            CallAction.Disconnect(
                                displayName = callState.call.displayName,
                                phone = callState.call.phone,
                                roomAlias = callState.call.roomAlias,
                                cause = DisconnectCause(DisconnectCause.LOCAL)
                            )
                        )
                    },
                    style = AppTheme.buttonSystem.rejectButtonStyle,
                    icon = ImageVector.vectorResource(R.drawable.ic_close_mini)
                )
            }
        }
    }
}

@Composable
private fun EndedCallContentComponent(
    callState: CallState.Ended,
    onCallAction: (CallAction) -> Unit,
    onCallFinished: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .gradientBackground(
                listOf(
                    AppTheme.colorSystem.secondary03,
                    AppTheme.colorSystem.secondary02
                )
            )
            .safeDrawingPadding()
            .padding(vertical = 24.dp, horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.weight(1.0f))

        ContainerComponent(
            contentPadding = PaddingValues(
                horizontal = 32.dp,
                vertical = 16.dp
            )
        ) {
            Image(
                imageVector = ImageVector.vectorResource(R.drawable.ic_logo),
                contentDescription = null
            )

            Text(text = callState.call.displayName)
        }

        ContainerComponent(
            contentPadding = PaddingValues(
                horizontal = 32.dp,
                vertical = 11.5.dp
            )
        ) {
            if (callState.wasActive) {
                Text(stringResource(R.string.label_call_finished))
            } else {
                Text(stringResource(R.string.label_call_rejected))
            }
        }

        Spacer(modifier = Modifier.weight(1.0f))

        ContainerComponent(contentPadding = PaddingValues(all = 12.dp)) {
            Row {
                ButtonComponent(
                    onClick = {
                        onCallAction(
                            CallAction.PlaceOutgoingCall(
                                displayName = callState.call.displayName,
                                phone = callState.call.phone,
                                roomAlias = callState.call.roomAlias
                            )
                        )
                    },
                    style = AppTheme.buttonSystem.primaryButtonStyle,
                    text = stringResource(R.string.button_call_redial),
                    icon = ImageVector.vectorResource(R.drawable.ic_refresh)
                )
                Spacer(modifier = Modifier.weight(1.0f))

                ButtonComponent(
                    onClick = onCallFinished,
                    icon = ImageVector.vectorResource(R.drawable.ic_close_mini),
                    style = AppTheme.buttonSystem.primaryButtonStyle
                )
            }
        }
    }
}

@Composable
private fun IncomingCallContentComponent(
    callState: CallState.Incoming,
    onCallAction: (CallAction) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .gradientBackground(
                listOf(
                    AppTheme.colorSystem.accent02,
                    AppTheme.colorSystem.accent01
                )
            )
            .safeDrawingPadding()
            .padding(vertical = 24.dp, horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.weight(1.0f))

        ContainerComponent(
            contentPadding = PaddingValues(
                horizontal = 32.dp,
                vertical = 16.dp
            )
        ) {
            Image(
                imageVector = ImageVector.vectorResource(R.drawable.ic_logo),
                contentDescription = null
            )

            Text(text = callState.call.displayName)
        }

        ContainerComponent(
            contentPadding = PaddingValues(
                horizontal = 32.dp,
                vertical = 11.5.dp
            )
        ) {
            Text(stringResource(R.string.label_incoming_call))
        }

        Spacer(modifier = Modifier.weight(1.0f))

        ContainerComponent(contentPadding = PaddingValues(all = 12.dp)) {
            Row {
                ButtonComponent(
                    onClick = {
                        onCallAction(
                            CallAction.Answer(
                                displayName = callState.call.displayName,
                                phone = callState.call.phone,
                                roomAlias = callState.call.roomAlias
                            )
                        )
                    },
                    style = AppTheme.buttonSystem.acceptButtonStyle,
                    icon = ImageVector.vectorResource(R.drawable.ic_checkbox)
                )

                Spacer(modifier = Modifier.weight(1.0f))

                ButtonComponent(
                    onClick = {
                        onCallAction(
                            CallAction.Disconnect(
                                displayName = callState.call.displayName,
                                phone = callState.call.phone,
                                roomAlias = callState.call.roomAlias,
                                cause = DisconnectCause(DisconnectCause.LOCAL)
                            )
                        )
                    },
                    style = AppTheme.buttonSystem.rejectButtonStyle,
                    icon = ImageVector.vectorResource(R.drawable.ic_close_mini)
                )
            }
        }
    }
}

@Composable
private fun MissedCallContentComponent(
    callState: CallState.Missed,
    onCallAction: (CallAction) -> Unit,
    onCallFinished: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .gradientBackground(
                listOf(
                    AppTheme.colorSystem.secondary03,
                    AppTheme.colorSystem.secondary02
                )
            )
            .safeDrawingPadding()
            .padding(vertical = 24.dp, horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.weight(1.0f))

        ContainerComponent(
            contentPadding = PaddingValues(
                horizontal = 32.dp,
                vertical = 16.dp
            )
        ) {
            Image(
                imageVector = ImageVector.vectorResource(R.drawable.ic_logo),
                contentDescription = null
            )

            Text(text = callState.call.displayName)
        }

        ContainerComponent(
            contentPadding = PaddingValues(
                horizontal = 32.dp,
                vertical = 11.5.dp
            )
        ) {
            Text(stringResource(R.string.label_missed_call))
        }

        Spacer(modifier = Modifier.weight(1.0f))

        ContainerComponent(contentPadding = PaddingValues(all = 12.dp)) {
            Row {
                ButtonComponent(
                    onClick = {
                        onCallAction(
                            CallAction.PlaceOutgoingCall(
                                displayName = callState.call.displayName,
                                phone = callState.call.phone,
                                roomAlias = callState.call.roomAlias
                            )
                        )
                    },
                    style = AppTheme.buttonSystem.primaryButtonStyle,
                    text = stringResource(R.string.button_call_back)
                )
                Spacer(modifier = Modifier.weight(1.0f))

                ButtonComponent(
                    onClick = onCallFinished,
                    icon = ImageVector.vectorResource(R.drawable.ic_close_mini),
                    style = AppTheme.buttonSystem.primaryButtonStyle
                )
            }
        }
    }
}

private fun formatDuration(duration: Duration): String {
    val totalSeconds = duration.inWholeMilliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}