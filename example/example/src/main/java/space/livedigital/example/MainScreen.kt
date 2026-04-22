package space.livedigital.example

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onVisibilityChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import space.livedigital.example.ui.components.buttons.ButtonComponent
import space.livedigital.example.ui.components.containers.ContainerComponent
import space.livedigital.example.ui.extensions.gradientBackground
import space.livedigital.example.ui.theme.AppTheme
import space.livedigital.sdk.data.entities.MediaLabel
import space.livedigital.sdk.data.entities.Peer
import space.livedigital.sdk.view.PeerView
import kotlin.time.Duration

@Composable
fun MainScreen(
    state: ScreenState,
    onRestartClicked: () -> Unit,
    onCameraClicked: () -> Unit,
    onMicrophoneClicked: () -> Unit,
    onFlipCameraClick: () -> Unit,
    onAudioDeviceClick: () -> Unit,
    onAudioDeviceSelected: (String) -> Unit,
    onAudioDeviceDismiss: () -> Unit
) {
    val peers = remember(state.remotePeers) {
        buildPeersList(state)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .gradientBackground(
                listOf(
                    AppTheme.colorSystem.accent02,
                    AppTheme.colorSystem.accent01
                )
            )
    ) {
        val pagerState = rememberPagerState {
            peers.size
        }
        HorizontalPager(
            state = pagerState,
            key = { index ->
                "${peers[index].peerWithUpdateTime.peer.id.value}:${peers[index].videoContentType}"
            }) { pageIndex ->
            RemotePeerComponent(peers[pageIndex])
        }

        if (state.roomName != null) {
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .safeDrawingPadding()
                    .padding(top = 46.dp, start = 16.dp, end = 16.dp)
            ) {
                ContainerComponent(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    val peer = peers.getOrNull(pagerState.currentPage)?.peerWithUpdateTime?.peer
                    val text = peer?.let {
                        "${state.roomName}\n" +
                                "${peer.appData.optString("name", "User")}\n" +
                                "ID: ${peer.id.value}"
                    } ?: state.roomName

                    Text(text = text, textAlign = TextAlign.Center)
                }

                when {
                    !state.isSessionStarted -> {
                        ContainerComponent(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text(
                                text = "Connecting.."
                            )
                        }
                    }

                    state.callDuration != null -> {
                        ContainerComponent(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text(
                                text = formatDuration(state.callDuration)
                            )
                        }
                    }
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .safeDrawingPadding()
        ) {
            if (state.isLocalVideoOn) {
                Box(modifier = Modifier.align(Alignment.End)) {
                    AndroidView(
                        factory = { context ->
                            PeerView(context).apply {
                                scaleType = PeerView.ScaleType.CENTER_CROP
                            }
                        },
                        update = { view ->
                            if (state.localVideoSource != null && state.isLocalVideoOn) {
                                view.renderVideoSource(state.localVideoSource)
                            }
                        },
                        onRelease = { view ->
                            if (state.localVideoSource != null) {
                                view.stopRenderingVideoSource(state.localVideoSource)
                            }
                            view.release()
                        },
                        modifier = Modifier
                            .size(120.dp, 180.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                2.dp,
                                shape = RoundedCornerShape(20.dp),
                                color = AppTheme.colorSystem.contrast
                            )
                    )

                    ButtonComponent(
                        onClick = onFlipCameraClick,
                        style = AppTheme.buttonSystem.tertiaryButtonStyle.copy(
                            normalContentColor = AppTheme.colorSystem.contrast
                        ),
                        icon = ImageVector.vectorResource(R.drawable.ic_swap_camera),
                        contentPaddingValues = PaddingValues(all = 8.dp),
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
            }

            ContainerComponent(contentPadding = PaddingValues(all = 12.dp)) {
                Row {
                    ButtonComponent(
                        onClick = onAudioDeviceClick,
                        style = if (!state.isAudioDevicePickerShown) {
                            AppTheme.buttonSystem.tertiaryButtonStyle.copy(
                                normalContentColor = AppTheme.colorSystem.contrast
                            )
                        } else {
                            AppTheme.buttonSystem.primaryButtonStyle
                        },
                        icon = ImageVector.vectorResource(R.drawable.ic_sound)
                    )

                    Spacer(modifier = Modifier.weight(1.0f))

                    ButtonComponent(
                        onClick = onCameraClicked,
                        style = if (!state.isLocalVideoOn) {
                            AppTheme.buttonSystem.tertiaryButtonStyle
                        } else {
                            AppTheme.buttonSystem.primaryButtonStyle
                        },
                        icon = ImageVector.vectorResource(
                            if (!state.isLocalVideoOn) {
                                R.drawable.ic_camera_off
                            } else {
                                R.drawable.ic_camera_on
                            }
                        )
                    )

                    Spacer(modifier = Modifier.weight(1.0f))

                    ButtonComponent(
                        onClick = onMicrophoneClicked,
                        style = if (!state.isLocalAudioOn) {
                            AppTheme.buttonSystem.tertiaryButtonStyle
                        } else {
                            AppTheme.buttonSystem.primaryButtonStyle
                        },
                        icon = ImageVector.vectorResource(
                            if (!state.isLocalAudioOn) {
                                R.drawable.ic_microphone_off
                            } else {
                                R.drawable.ic_microphone_on
                            }
                        )
                    )

                    Spacer(modifier = Modifier.weight(1.0f))

                    ButtonComponent(
                        onClick = onRestartClicked,
                        style = AppTheme.buttonSystem.tertiaryButtonStyle.copy(
                            normalContentColor = AppTheme.colorSystem.contrast
                        ),
                        icon = ImageVector.vectorResource(R.drawable.ic_refresh)
                    )
                }
            }
        }
    }

    if (state.isAudioDevicePickerShown) {
        AudioDevicePickerComponent(
            devices = state.availableAudioDeviceNames,
            selectedIndex = state.audioDeviceIndex,
            onDeviceSelected = onAudioDeviceSelected,
            onDismiss = onAudioDeviceDismiss
        )
    }
}

@Composable
private fun AudioDevicePickerComponent(
    devices: List<String>,
    selectedIndex: Int,
    onDeviceSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose audio device") },
        text = {
            Column(Modifier.selectableGroup()) {
                devices.forEachIndexed { index, deviceName ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (index == selectedIndex),
                                onClick = { onDeviceSelected(deviceName) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (index == selectedIndex), onClick = null)
                        Text(
                            text = deviceName,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun RemotePeerComponent(
    peerWithMedia: PeerWithMediaContentType,
    modifier: Modifier = Modifier
) {
    val peer = peerWithMedia.peerWithUpdateTime.peer
    val contentType = peerWithMedia.videoContentType

    val isVisibleState = remember { mutableStateOf(false) }

    val isConsumingAudio = remember(peerWithMedia) {
        when (contentType) {
            PeerVideoContentType.CUSTOM_VIDEO -> peer.isConsumingAudio(MediaLabel.CUSTOM_AUDIO)
            PeerVideoContentType.SCREEN_VIDEO -> peer.isConsumingAudio(MediaLabel.SCREEN_AUDIO)
            PeerVideoContentType.CAMERA -> peer.isConsumingAudio(MediaLabel.MICROPHONE)
        }
    }

    val isConsumingVideo = remember(peerWithMedia) {
        when (contentType) {
            PeerVideoContentType.CUSTOM_VIDEO -> peer.isConsumingVideo(MediaLabel.CUSTOM_VIDEO)
            PeerVideoContentType.SCREEN_VIDEO -> peer.isConsumingVideo(MediaLabel.SCREEN_VIDEO)
            PeerVideoContentType.CAMERA -> peer.isConsumingVideo(MediaLabel.CAMERA)
        }
    }

    Box(
        modifier = modifier
    ) {
        AndroidView(
            factory = { context ->
                PeerView(context).apply {
                    scaleType = when (contentType) {
                        PeerVideoContentType.CUSTOM_VIDEO -> PeerView.ScaleType.FIT_CENTER
                        PeerVideoContentType.SCREEN_VIDEO -> PeerView.ScaleType.FIT_CENTER
                        PeerVideoContentType.CAMERA -> PeerView.ScaleType.CROP_FIT_FIT_BOUNDED
                    }
                }
            },
            update = { view ->
                if (isConsumingVideo && isVisibleState.value) {
                    view.renderPeerVideo(peer, contentType.videoMediaLabel)
                } else {
                    view.stopRenderingPeerVideo(peer, contentType.videoMediaLabel)
                }
            },
            onRelease = { view ->
                view.stopRenderingPeerVideo(peer, contentType.videoMediaLabel)
                view.release()
            },
            modifier = Modifier
                .fillMaxSize()
                .onVisibilityChanged(minFractionVisible = 0.1f, minDurationMs = 300) { isVisible ->
                    isVisibleState.value = isVisible
                }
        )

        Row(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(12.dp)
                .align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ContainerComponent(contentPadding = PaddingValues(6.dp)) {
                Icon(
                    painter = painterResource(
                        id = if (isConsumingVideo) R.drawable.ic_camera_on else R.drawable.ic_camera_off
                    ),
                    contentDescription = null,
                    tint = if (isConsumingVideo) AppTheme.colorSystem.contrast else AppTheme.colorSystem.errorBase,
                    modifier = Modifier.size(16.dp)
                )
            }
            ContainerComponent(contentPadding = PaddingValues(6.dp)) {
                Icon(
                    painter = painterResource(
                        id = if (isConsumingAudio) R.drawable.ic_microphone_on else R.drawable.ic_microphone_off
                    ),
                    contentDescription = null,
                    tint = if (isConsumingAudio) AppTheme.colorSystem.contrast else AppTheme.colorSystem.errorBase,
                    modifier = Modifier.size(16.dp)
                )
            }
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