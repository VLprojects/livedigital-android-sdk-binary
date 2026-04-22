package space.livedigital.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import space.livedigital.sdk.data.entities.MediaLabel
import space.livedigital.sdk.data.entities.Peer
import space.livedigital.sdk.media.video.CameraPosition
import space.livedigital.sdk.view.PeerView

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
    val listState = rememberLazyListState()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
        ) {
            val peers = remember(state.remotePeers) {
                buildPeersList(state)
            }

            val visiblePeerIndices by remember {
                derivedStateOf {
                    val layoutInfo = listState.layoutInfo
                    val viewportStart = layoutInfo.viewportStartOffset
                    val viewportEnd = layoutInfo.viewportEndOffset

                    layoutInfo.visibleItemsInfo.filter { item ->
                        val itemStart = item.offset
                        val itemEnd = item.offset + item.size

                        val visibleWidth =
                            minOf(itemEnd, viewportEnd) - maxOf(itemStart, viewportStart)
                        val visibilityRatio = visibleWidth.toFloat() / item.size

                        visibilityRatio > 0.1f
                    }.map { it.index }
                }
            }

            LazyRow(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = peers,
                    key = { _, peer ->
                        "${peer.peerWithUpdateTime.peer.id}:${peer.videoContentType}"
                    }) { index, peer ->
                    RemotePeerItem(
                        peerWithMedia = peer,
                        isVisibleOnScreen = index in visiblePeerIndices,
                        modifier = Modifier.fillParentMaxSize()
                    )
                }
            }

            AndroidView(
                factory = { context ->
                    PeerView(context)
                },
                update = { view ->
                    state.localVideoSource?.let { videoSource ->
                        if (state.isLocalVideoOn) {
                            view.renderVideoSource(videoSource)
                        } else {
                            view.stopRenderingVideoSource(videoSource)
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .size(110.dp, 200.dp),
                onRelease = { it.release() }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFA8072))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControlButton(
                    iconRes = R.drawable.ic_outline_autorenew_24,
                    text = "Restart",
                    onClick = onRestartClicked
                )

                val cameraIcon = if (state.isLocalVideoOn)
                    R.drawable.ic_baseline_videocam_24 else R.drawable.ic_baseline_videocam_off_24
                ControlButton(
                    iconRes = cameraIcon,
                    text = "Camera",
                    onClick = onCameraClicked
                )

                val micIcon = if (state.isLocalAudioOn)
                    R.drawable.ic_baseline_mic_24 else R.drawable.ic_baseline_mic_off_24
                ControlButton(
                    iconRes = micIcon,
                    text = "Mic",
                    onClick = onMicrophoneClicked
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControlButton(
                    iconRes = R.drawable.ic_outline_audiotrack_24,
                    text = "Switch audio",
                    textSize = 15.sp,
                    onClick = onAudioDeviceClick
                )

                val flipIcon = when (state.localCameraPosition) {
                    CameraPosition.BACK -> R.drawable.ic_outline_photo_camera_back_24
                    CameraPosition.FRONT -> R.drawable.ic_outline_photo_camera_front_24
                }
                ControlButton(
                    iconRes = flipIcon,
                    text = "Switch camera",
                    textSize = 15.sp,
                    onClick = onFlipCameraClick
                )
            }
        }
    }

    if (state.isAudioDevicePickerShown) {
        AudioDevicePicker(
            devices = state.availableAudioDeviceNames,
            selectedIndex = state.audioDeviceIndex,
            onDeviceSelected = onAudioDeviceSelected,
            onDismiss = onAudioDeviceDismiss
        )
    }
}

@Composable
fun AudioDevicePicker(
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
fun ControlButton(
    iconRes: Int,
    text: String,
    onClick: () -> Unit,
    textSize: TextUnit = 20.sp
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = text,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = textSize
        )
    }
}

@Composable
fun RemotePeerItem(
    peerWithMedia: PeerWithMediaContentType,
    isVisibleOnScreen: Boolean,
    modifier: Modifier = Modifier
) {
    val peer = peerWithMedia.peerWithUpdateTime.peer
    val contentType = peerWithMedia.videoContentType

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
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                if (isConsumingVideo && isVisibleOnScreen) {
                    view.renderPeerVideo(peer, contentType.videoMediaLabel)
                } else {
                    view.stopRenderingPeerVideo(peer, contentType.videoMediaLabel)
                }
            },
            onRelease = { view ->
                view.stopRenderingPeerVideo(peer, contentType.videoMediaLabel)
                view.release()
            }
        )

        Text(
            text = peer.id.value,
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(Color(0xFF035397))
                .padding(4.dp),
            color = Color(0xFFFFAA4C),
            fontSize = 12.sp
        )

        Text(
            text = peer.appData.optString("name"),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(Color(0xFF035397))
                .padding(4.dp),
            color = Color(0xFFFFAA4C),
            fontSize = 12.sp
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(Color(0xFF035397))
                .padding(4.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "video is ${if (isConsumingVideo) "on" else "off"}",
                color = Color(0xFFFFAA4C),
                fontSize = 12.sp
            )
            Text(
                text = "audio is ${if (isConsumingAudio) "on" else "off"}",
                color = Color(0xFFFFAA4C),
                fontSize = 12.sp
            )
        }
    }
}

private fun buildPeersList(state: ScreenState): List<PeerWithMediaContentType> = buildList {
    state.remotePeers.forEach { peer ->
        if (peer.peer.hasMedia(MediaLabel.CUSTOM_VIDEO)) {
            add(PeerWithMediaContentType(peer, PeerVideoContentType.CUSTOM_VIDEO))
        }
        if (peer.peer.hasMedia(MediaLabel.SCREEN_VIDEO)) {
            add(PeerWithMediaContentType(peer, PeerVideoContentType.SCREEN_VIDEO))
        }
        add(PeerWithMediaContentType(peer, PeerVideoContentType.CAMERA))
    }
}

private fun Peer.hasMedia(label: MediaLabel): Boolean {
    return (this.hasConsumer(label) || this.hasProducerDataInStock(label)) &&
            this.isRemoteProducerResumed(label)
}