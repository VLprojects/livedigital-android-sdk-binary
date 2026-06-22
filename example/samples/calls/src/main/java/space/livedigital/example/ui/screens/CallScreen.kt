package space.livedigital.example.ui.screens

import android.Manifest
import android.os.Build
import android.telecom.DisconnectCause
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import space.livedigital.example.R
import space.livedigital.example.calls.PeerWithVersion
import space.livedigital.example.calls.ScreenState
import space.livedigital.example.calls.constants.CallEndpointType
import space.livedigital.example.calls.entities.CallAction
import space.livedigital.example.calls.entities.CallAction.Answer
import space.livedigital.example.calls.entities.CallAction.Disconnect
import space.livedigital.example.calls.entities.CallAction.PlaceOutgoingCall
import space.livedigital.example.calls.entities.CallAction.ToggleCamera
import space.livedigital.example.calls.entities.CallAction.ToggleMute
import space.livedigital.example.calls.entities.CallState
import space.livedigital.example.calls.entities.CallType
import space.livedigital.example.calls.entities.GeneralCallEndpoint
import space.livedigital.example.calls.utils.initialIsCameraOn
import space.livedigital.example.calls.utils.initialIsMuted
import space.livedigital.example.ui.components.buttons.ButtonComponent
import space.livedigital.example.ui.components.containers.ContainerComponent
import space.livedigital.example.ui.components.peer.RemotePeerComponent
import space.livedigital.example.ui.extensions.crop
import space.livedigital.example.ui.extensions.gradientBackground
import space.livedigital.example.ui.theme.AppTheme
import space.livedigital.sdk.channel.ChannelSessionStatus
import space.livedigital.sdk.data.entities.MediaLabel
import space.livedigital.sdk.data.entities.Peer
import space.livedigital.sdk.media.video.VideoSource
import space.livedigital.sdk.view.PeerView
import kotlin.time.Duration

@Composable
internal fun CallScreen(
    state: State<ScreenState>,
    onCallFinished: () -> Unit,
    onCallAction: (CallAction) -> Unit,
    onFlipCameraClicked: () -> Unit,
    onChangeLocalPreviewFullscreenStateClicked: () -> Unit,
    onAudioDevicePickerClicked: () -> Unit,
    onAudioDevicePickerDismissed: () -> Unit,
    onAudioDevicePicked: (GeneralCallEndpoint) -> Unit
) {
    val state = state.value
    val peers = remember(state.remotePeers) {
        buildPeersList(state)
    }

    var isLocalPreviewFullscreen by remember(state.callState, state.isLocalPreviewFullscreen) {
        mutableStateOf(state.callState is CallState.Outgoing || state.isLocalPreviewFullscreen)
    }

    val targetColors =
        if (state.callState is CallState.Missed || state.callState is CallState.Ended) {
            listOf(AppTheme.colorSystem.secondary03, AppTheme.colorSystem.secondary02)
        } else {
            listOf(AppTheme.colorSystem.accent02, AppTheme.colorSystem.accent01)
        }

    val firstColor by animateColorAsState(
        targetValue = targetColors[0],
        animationSpec = tween(durationMillis = 300),
        label = "GradientColor1"
    )
    val secondColor by animateColorAsState(
        targetValue = targetColors[1],
        animationSpec = tween(durationMillis = 300),
        label = "GradientColor2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .gradientBackground(listOf(firstColor, secondColor))
    ) {
        if (state.callState is CallState.Active &&
            state.callState.call.callType == CallType.VIDEO
        ) {
            val pagerState = rememberPagerState { peers.size }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { index -> "${peers[index].peer.peer.id.value}:${peers[index].videoContentType}" }
            ) { pageIndex ->
                RemotePeerComponent(peers[pageIndex], modifier = Modifier.fillMaxSize())
            }
        }

        if (isLocalPreviewFullscreen &&
            state.callState.call.callType == CallType.VIDEO
        ) {
            LocalVideoComponent(
                isLocalVideoOn = state.isLocalVideoOn,
                localVideoSource = state.localVideoSource,
                isOutgoingCall = state.callState is CallState.Outgoing,
                isLocalPreviewFullscreen = true,
                onFlipCameraClick = onFlipCameraClicked,
                onChangeLocalPreviewFullscreenStateClicked =
                    onChangeLocalPreviewFullscreenStateClicked,
                modifier = Modifier
                    .fillMaxSize()
            )
        }

        CallInfoComponent(state)

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (!isLocalPreviewFullscreen && state.callState !is CallState.Outgoing &&
                state.callState !is CallState.Idle &&
                state.callState.call.callType == CallType.VIDEO
            ) {
                LocalVideoComponent(
                    isLocalVideoOn = state.isLocalVideoOn,
                    localVideoSource = state.localVideoSource,
                    isLocalPreviewFullscreen = false,
                    isOutgoingCall = false,
                    onFlipCameraClick = onFlipCameraClicked,
                    onChangeLocalPreviewFullscreenStateClicked =
                        onChangeLocalPreviewFullscreenStateClicked,
                    modifier = Modifier
                        .size(width = 120.dp, height = 180.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            2.dp,
                            AppTheme.colorSystem.contrast,
                            RoundedCornerShape(20.dp)
                        )
                )
                Spacer(modifier = Modifier.size(16.dp))
            }

            CallControlPanel(
                screenState = state,
                onCallFinished = onCallFinished,
                onCallAction = onCallAction,
                onAudioDevicePickerClicked = onAudioDevicePickerClicked,
                onAudioDevicePickerDismissed = onAudioDevicePickerDismissed,
                onAudioDevicePicked = onAudioDevicePicked,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BoxScope.CallInfoComponent(
    state: ScreenState,
) {
    val callState = state.callState
    if (callState is CallState.Idle) return

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .safeDrawingPadding()
            .padding(top = if (callState is CallState.Active) 46.dp else 24.dp)
    ) {
        if (callState is CallState.Outgoing || callState is CallState.Incoming ||
            callState is CallState.Ended || callState is CallState.Missed
        ) {
            Spacer(modifier = Modifier.weight(1.0f))
        }

        ContainerComponent(
            contentPadding = PaddingValues(
                horizontal = 32.dp,
                vertical = 16.dp
            )
        ) {
            if (callState is CallState.Outgoing || callState is CallState.Incoming ||
                callState is CallState.Ended || callState is CallState.Missed
            ) {
                Image(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_logo),
                    contentDescription = null
                )
            }

            Text(text = callState.call.displayName)
        }

        when (callState) {
            is CallState.Active -> {
                val label = when {
                    state.sessionStatus != ChannelSessionStatus.STARTED -> stringResource(R.string.label_connection)
                    state.callDuration != Duration.ZERO -> formatDuration(state.callDuration)
                    else -> null
                }
                label?.let {
                    ContainerComponent(
                        contentPadding = PaddingValues(
                            horizontal = 32.dp,
                            vertical = 8.dp
                        )
                    ) {
                        Text(it)
                    }
                }
            }

            is CallState.Outgoing -> {
                ContainerComponent(
                    contentPadding = PaddingValues(
                        horizontal = 32.dp,
                        vertical = 11.5.dp
                    )
                ) {
                    Text(stringResource(R.string.label_call))
                }
            }

            is CallState.Incoming -> {
                ContainerComponent(
                    contentPadding = PaddingValues(
                        horizontal = 32.dp,
                        vertical = 11.5.dp
                    )
                ) {
                    Text(
                        stringResource(
                            if (callState.call.callType == CallType.VIDEO) {
                                R.string.label_incoming_video_call
                            } else {
                                R.string.label_incoming_call
                            }
                        )
                    )
                }
            }

            is CallState.Ended -> {
                ContainerComponent(
                    contentPadding = PaddingValues(
                        horizontal = 32.dp,
                        vertical = 11.5.dp
                    )
                ) {
                    Text(
                        stringResource(
                            if (callState.wasActive) {
                                R.string.label_call_finished
                            } else {
                                R.string.label_call_rejected
                            }
                        )
                    )
                }
            }

            is CallState.Missed -> {
                ContainerComponent(
                    contentPadding = PaddingValues(
                        horizontal = 32.dp,
                        vertical = 11.5.dp
                    )
                ) {
                    Text(
                        stringResource(
                            if (callState.call.callType == CallType.VIDEO) {
                                R.string.label_missed_video_call
                            } else {
                                R.string.label_missed_call
                            }
                        )
                    )
                }
            }

            else -> Unit
        }

        if (callState is CallState.Outgoing || callState is CallState.Incoming ||
            callState is CallState.Ended || callState is CallState.Missed
        ) {
            Spacer(modifier = Modifier.weight(1.0f))
        }
    }
}

@Composable
private fun CallControlPanel(
    screenState: ScreenState,
    onCallAction: (CallAction) -> Unit,
    onCallFinished: () -> Unit,
    onAudioDevicePickerClicked: () -> Unit,
    onAudioDevicePickerDismissed: () -> Unit,
    onAudioDevicePicked: (GeneralCallEndpoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val callState = screenState.callState

    if (callState is CallState.Idle) return

    ContainerComponent(
        contentPadding = PaddingValues(all = 12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (callState) {
                is CallState.Active,
                is CallState.Outgoing,
                is CallState.Activated,
                is CallState.Answered -> {
                    val isMuted = (callState as? CallState.Active)?.isMuted
                        ?: (callState as? CallState.Outgoing)?.isMuted
                        ?: (callState as? CallState.Answered)?.isMuted
                        ?: (callState as? CallState.Activated)?.isMuted ?: false
                    val isCameraOn = (callState as? CallState.Active)?.isCameraOn
                        ?: (callState as? CallState.Outgoing)?.isCameraOn
                        ?: (callState as? CallState.Answered)?.isCameraOn
                        ?: (callState as? CallState.Activated)?.isCameraOn ?: false

                    val microphonePermissionLauncher = rememberLauncherForActivityResult(
                        contract = RequestPermission()
                    ) { isGranted -> if (isGranted) onCallAction(ToggleMute(!isMuted)) }

                    val cameraPermissionLauncher = rememberLauncherForActivityResult(
                        contract = RequestPermission()
                    ) { isGranted -> if (isGranted) onCallAction(ToggleCamera(!isCameraOn)) }

                    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
                        contract = RequestPermission()
                    ) {
                        onAudioDevicePickerClicked()
                    }


                    Box {
                        DropdownMenu(
                            expanded = screenState.isAudioDevicePickerShown,
                            offset = DpOffset(x = (-12).dp, y = (-16).dp),
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            containerColor = AppTheme.colorSystem.audioPickerContainer,
                            onDismissRequest = onAudioDevicePickerDismissed,
                            modifier = Modifier
                                .widthIn(max = 244.dp)
                                .fillMaxWidth(0.9f)
                                .crop(vertical = 8.dp)
                        ) {
                            for (audioDevice in screenState.audioDevices) {
                                val audioDeviceUi = audioDevice.type.toUi() ?: break
                                val label = stringResource(audioDeviceUi.labelRes)
                                val isCurrent = audioDevice == screenState.currentAudioDevice
                                val targetContentColor = if (isCurrent) {
                                    AppTheme.colorSystem.bg
                                } else {
                                    AppTheme.colorSystem.contrast
                                }
                                val targetBackgroundColor = if (isCurrent) {
                                    AppTheme.colorSystem.contrast
                                } else {
                                    AppTheme.colorSystem.audioPickerContainer
                                }
                                val contentColor by animateColorAsState(
                                    targetValue = targetContentColor,
                                    animationSpec = tween(300),
                                    label = "device_picker_content_color"
                                )
                                val backgroundColor by animateColorAsState(
                                    targetValue = targetBackgroundColor,
                                    animationSpec = tween(300),
                                    label = "device_picker_background_color"
                                )

                                DropdownMenuItem(
                                    colors = MenuDefaults.itemColors(
                                        textColor = contentColor,
                                        trailingIconColor = contentColor
                                    ),
                                    text = {
                                        Text(
                                            text = label,
                                            style = AppTheme.typographySystem.mainTextMedium
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(
                                                audioDeviceUi.iconRes
                                            ),
                                            contentDescription = label
                                        )
                                    },
                                    onClick = { onAudioDevicePicked(audioDevice) },
                                    modifier = Modifier.background(color = backgroundColor)
                                )
                            }
                        }

                        ButtonComponent(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    bluetoothPermissionLauncher.launch(
                                        Manifest.permission.BLUETOOTH_CONNECT
                                    )
                                } else {
                                    onAudioDevicePickerClicked()
                                }
                            },
                            style = if (screenState.isAudioDevicePickerShown) {
                                AppTheme.buttonSystem.primaryButtonStyle
                            } else {
                                AppTheme.buttonSystem.tertiaryButtonStyle.copy(
                                    normalContentColor = AppTheme.colorSystem.contrast
                                )
                            },
                            icon = ImageVector.vectorResource(R.drawable.ic_sound)
                        )
                    }

                    if (callState.call.callType == CallType.VIDEO) {
                        ButtonComponent(
                            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                            style = if (isCameraOn) {
                                AppTheme.buttonSystem.primaryButtonStyle
                            } else {
                                AppTheme.buttonSystem.tertiaryButtonStyle
                            },
                            icon = ImageVector.vectorResource(
                                if (isCameraOn) {
                                    R.drawable.ic_camera_on
                                } else {
                                    R.drawable.ic_camera_off
                                }
                            )
                        )
                    }

                    ButtonComponent(
                        onClick =
                            { microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        style = if (isMuted) {
                            AppTheme.buttonSystem.tertiaryButtonStyle
                        } else {
                            AppTheme.buttonSystem.primaryButtonStyle
                        },
                        icon = ImageVector.vectorResource(
                            if (isMuted) {
                                R.drawable.ic_microphone_off
                            } else {
                                R.drawable.ic_microphone_on
                            }
                        )
                    )

                    ButtonComponent(
                        onClick = {
                            val call = callState.call
                            onCallAction(
                                Disconnect(
                                    call.displayName,
                                    call.phone,
                                    call.roomAlias,
                                    call.callType,
                                    DisconnectCause(DisconnectCause.LOCAL)
                                )
                            )
                        },
                        style = AppTheme.buttonSystem.rejectButtonStyle,
                        icon = ImageVector.vectorResource(R.drawable.ic_close_mini)
                    )
                }

                is CallState.Incoming -> {
                    val context = LocalContext.current
                    ButtonComponent(
                        onClick = {
                            onCallAction(
                                Answer(
                                    callState.call.displayName,
                                    callState.call.phone,
                                    callState.call.roomAlias,
                                    callState.call.callType,
                                    isMuted = context.initialIsMuted(),
                                    isCameraOn = context.initialIsCameraOn(callState.call.callType)
                                )
                            )
                        },
                        style = AppTheme.buttonSystem.acceptButtonStyle,
                        icon = ImageVector.vectorResource(R.drawable.ic_checkbox)
                    )

                    ButtonComponent(
                        onClick = {
                            onCallAction(
                                Disconnect(
                                    callState.call.displayName,
                                    callState.call.phone,
                                    callState.call.roomAlias,
                                    callState.call.callType,
                                    DisconnectCause(DisconnectCause.LOCAL)
                                )
                            )
                        },
                        style = AppTheme.buttonSystem.rejectButtonStyle,
                        icon = ImageVector.vectorResource(R.drawable.ic_close_mini)
                    )
                }

                is CallState.Ended, is CallState.Missed -> {
                    val context = LocalContext.current
                    val call = (callState as? CallState.Ended)?.call
                        ?: (callState as? CallState.Missed)?.call
                    ButtonComponent(
                        onClick = {
                            call?.let {
                                onCallAction(
                                    PlaceOutgoingCall(
                                        it.displayName,
                                        it.phone,
                                        it.roomAlias,
                                        it.callType,
                                        isMuted = context.initialIsMuted(),
                                        isCameraOn = context.initialIsCameraOn(it.callType)
                                    )
                                )
                            }
                        },
                        style = AppTheme.buttonSystem.primaryButtonStyle,
                        text = stringResource(
                            if (callState is CallState.Missed) {
                                R.string.button_call_back
                            } else {
                                R.string.button_call_redial
                            }
                        ),
                        icon = ImageVector.vectorResource(R.drawable.ic_refresh)
                    )
                    ButtonComponent(
                        onClick = onCallFinished,
                        style = AppTheme.buttonSystem.primaryButtonStyle,
                        icon = ImageVector.vectorResource(R.drawable.ic_close_mini)
                    )
                }

                else -> Unit
            }
        }
    }
}

@Composable
private fun LocalVideoComponent(
    isLocalVideoOn: Boolean,
    localVideoSource: VideoSource?,
    isOutgoingCall: Boolean,
    isLocalPreviewFullscreen: Boolean,
    onFlipCameraClick: () -> Unit,
    onChangeLocalPreviewFullscreenStateClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isLocalVideoOn) return

    val context = LocalContext.current
    var isFirstFrameRendered by remember { mutableStateOf(false) }

    val peerView = remember(context) {
        PeerView(context).apply {
            scaleType = PeerView.ScaleType.CENTER_CROP
            setRenderEventsListener(object : PeerView.PeerViewRenderEventsListener {
                override fun onFirstFrameRender() {
                    isFirstFrameRendered = true
                }
            })
        }
    }

    LaunchedEffect(localVideoSource) {
        isFirstFrameRendered = false
        if (localVideoSource != null) {
            peerView.renderVideoSource(localVideoSource)
        }
    }

    val activity = LocalActivity.current

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        if (activity?.isChangingConfigurations == false) {
            localVideoSource?.let {
                peerView.stopRenderingVideoSource(it)
            }
            peerView.release()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { peerView },
            onRelease = { view ->
                localVideoSource?.let {
                    peerView.stopRenderingVideoSource(it)
                }
                view.release()
            },
            modifier = Modifier
                .matchParentSize()
                .align(Alignment.BottomEnd)
        )

        if (!isFirstFrameRendered) {
            CircularProgressIndicator(
                color = AppTheme.colorSystem.accentBase,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Row(
            modifier = Modifier
                .matchParentSize()
                .align(Alignment.BottomEnd)
                .safeDrawingPadding()
                .padding(
                    top = if (isLocalPreviewFullscreen && isOutgoingCall) {
                        46.dp
                    } else if (isLocalPreviewFullscreen) {
                        110.dp
                    } else {
                        8.dp
                    },
                    start = if (isLocalPreviewFullscreen) {
                        16.dp
                    } else {
                        0.dp
                    },
                    end = if (isLocalPreviewFullscreen) {
                        16.dp
                    } else {
                        0.dp
                    }
                )
        ) {
            if (!isOutgoingCall) {
                ButtonComponent(
                    onClick = onChangeLocalPreviewFullscreenStateClicked,
                    style = AppTheme.buttonSystem.tertiaryButtonStyle.copy(
                        normalContentColor = AppTheme.colorSystem.contrast
                    ),
                    icon = if (isLocalPreviewFullscreen) {
                        ImageVector.vectorResource(R.drawable.ic_minimal)
                    } else {
                        ImageVector.vectorResource(R.drawable.ic_maximal)
                    },
                    contentPaddingValues = PaddingValues(all = 8.dp),
                )
            }

            Spacer(modifier = Modifier.weight(1.0f))

            ButtonComponent(
                onClick = onFlipCameraClick,
                style = AppTheme.buttonSystem.tertiaryButtonStyle.copy(
                    normalContentColor = AppTheme.colorSystem.contrast
                ),
                icon = ImageVector.vectorResource(R.drawable.ic_swap_camera),
                contentPaddingValues = PaddingValues(all = 8.dp),
            )
        }
    }
}

private fun buildPeersList(state: ScreenState): List<PeerWithMediaContentType> = buildList {
    state.remotePeers.forEach { peer ->
        add(PeerWithMediaContentType(peer, PeerVideoContentType.CAMERA))
        if (peer.peer.hasMedia(MediaLabel.SCREEN_VIDEO)) {
            add(PeerWithMediaContentType(peer, PeerVideoContentType.SCREEN_VIDEO))
        }
        if (peer.peer.hasMedia(MediaLabel.CUSTOM_VIDEO)) {
            add(PeerWithMediaContentType(peer, PeerVideoContentType.CUSTOM_VIDEO))
        }
    }
}

private fun Peer.hasMedia(label: MediaLabel): Boolean {
    return (this.hasConsumer(label) || this.hasProducerDataInStock(label)) &&
            this.isRemoteProducerResumed(label)
}

private fun formatDuration(duration: Duration): String {
    val totalSeconds = duration.inWholeMilliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

data class PeerWithMediaContentType(
    val peer: PeerWithVersion,
    val videoContentType: PeerVideoContentType,
)

enum class PeerVideoContentType(val videoMediaLabel: MediaLabel) {
    CUSTOM_VIDEO(MediaLabel.CUSTOM_VIDEO),
    SCREEN_VIDEO(MediaLabel.SCREEN_VIDEO),
    CAMERA(MediaLabel.CAMERA)
}

data class AudioDeviceUi(
    @DrawableRes val iconRes: Int,
    @StringRes val labelRes: Int
)

fun CallEndpointType.toUi(): AudioDeviceUi? = when (this) {
    CallEndpointType.BLUETOOTH -> {
        AudioDeviceUi(R.drawable.ic_bluetooth, R.string.label_bluetooth)
    }

    CallEndpointType.SPEAKER -> {
        AudioDeviceUi(R.drawable.ic_volume, R.string.label_speaker)
    }

    CallEndpointType.WIRED_HEADSET -> {
        AudioDeviceUi(R.drawable.ic_headphones, R.string.label_headset)
    }

    CallEndpointType.EARPIECE -> {
        AudioDeviceUi(R.drawable.ic_phone, R.string.label_earpiece)
    }

    else -> {
        null
    }
}