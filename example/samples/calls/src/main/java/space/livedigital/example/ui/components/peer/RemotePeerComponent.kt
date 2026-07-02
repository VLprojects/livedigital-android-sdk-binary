package space.livedigital.example.ui.components.peer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onVisibilityChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import space.livedigital.example.ui.screens.PeerVideoContentType
import space.livedigital.example.ui.screens.PeerWithMediaContentType
import space.livedigital.sdk.view.PeerView

@Composable
fun RemotePeerComponent(
    peerWithMedia: PeerWithMediaContentType,
    modifier: Modifier = Modifier
) {
    val peer = peerWithMedia.peer.peer
    val contentType = peerWithMedia.videoContentType
    val context = LocalContext.current

    var isVisible by remember { mutableStateOf(false) }
    var isFirstFrameRendered by remember { mutableStateOf(false) }

    val peerView = remember(context, peer.id, contentType) {
        PeerView(context).apply {
            scaleType = when (contentType) {
                PeerVideoContentType.CUSTOM_VIDEO,
                PeerVideoContentType.SCREEN_VIDEO -> PeerView.ScaleType.FIT_CENTER

                PeerVideoContentType.CAMERA -> PeerView.ScaleType.CROP_FIT_FIT_BOUNDED
            }
            setRenderEventsListener(object : PeerView.PeerViewRenderEventsListener {
                override fun onFirstFrameRender() {
                    isFirstFrameRendered = true
                }
            })
        }
    }

    val isConsuming = peer.isConsumingVideo(contentType.videoMediaLabel)

    LaunchedEffect(peer.id, contentType, isVisible, isConsuming) {
        val shouldRender = isVisible && isConsuming

        if (shouldRender) {
            peerView.renderPeerVideo(peer, contentType.videoMediaLabel)
        } else {
            if (isFirstFrameRendered) {
                peerView.stopRenderingPeerVideo(peer, contentType.videoMediaLabel)
            }
            isFirstFrameRendered = false
        }
    }

    AndroidView(
        factory = { peerView },
        modifier = modifier
            .fillMaxSize()
            .onVisibilityChanged(minFractionVisible = 0.1f, minDurationMs = 300) { visible ->
                isVisible = visible
            },
        onRelease = { view ->
            if (isFirstFrameRendered) {
                view.stopRenderingPeerVideo(peer, contentType.videoMediaLabel)
                view.release()
            }
            isFirstFrameRendered = false
        }
    )
}