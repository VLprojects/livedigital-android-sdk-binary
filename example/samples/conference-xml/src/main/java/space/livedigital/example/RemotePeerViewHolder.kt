package space.livedigital.example

import android.annotation.SuppressLint
import android.view.View
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import space.livedigital.example.databinding.ItemRemotePeerBinding
import space.livedigital.sdk.data.entities.MediaLabel
import space.livedigital.sdk.view.PeerView
import space.livedigital.sdk.view.VideoRenderer

internal class RemotePeerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val binding = ItemRemotePeerBinding.bind(itemView)

    @SuppressLint("SetTextI18n")
    fun bind(peerWithMediaContentType: PeerWithMediaContentType) {
        val peer = peerWithMediaContentType.peerWithUpdateTime.peer
        val peerVideoContentType = peerWithMediaContentType.videoContentType
        binding.peerIdText.text = peer.id.value
        binding.peerNameText.text = peer.appData.optString("name")

        val isConsumingAudio: Boolean
        val isConsumingVideo: Boolean
        val scale: PeerView.ScaleType

        when (peerVideoContentType) {
            PeerVideoContentType.CUSTOM_VIDEO -> {
                isConsumingAudio = peer.isConsumingAudio(MediaLabel.CUSTOM_AUDIO)
                isConsumingVideo = peer.isConsumingVideo(MediaLabel.CUSTOM_VIDEO)
                scale = PeerView.ScaleType.FIT_CENTER
            }

            PeerVideoContentType.SCREEN_VIDEO -> {
                isConsumingAudio = peer.isConsumingAudio(MediaLabel.SCREEN_AUDIO)
                isConsumingVideo = peer.isConsumingVideo(MediaLabel.SCREEN_VIDEO)
                scale = PeerView.ScaleType.FIT_CENTER
            }

            PeerVideoContentType.CAMERA -> {
                isConsumingAudio = peer.isConsumingAudio(MediaLabel.MICROPHONE)
                isConsumingVideo = peer.isConsumingVideo(MediaLabel.CAMERA)
                scale = PeerView.ScaleType.CROP_FIT_FIT_BOUNDED
            }
        }

        binding.remotePeerView.isInvisible = isConsumingVideo.not()
        binding.audioStatusText.text = "audio is ${if (isConsumingAudio) "on" else "off"}"
        binding.videoStatusText.text = "video is ${if (isConsumingVideo) "on" else "off"}"
        binding.remotePeerView.renderPeerVideo(peer, peerVideoContentType.videoMediaLabel)
        binding.remotePeerView.scaleType = scale

        binding.remotePeerView.setViewScreenVisibilityListener(
            object : PeerView.ViewScreenVisibilityListener {
                override fun onViewBecomeVisibleOnScreen(videoRenderer: VideoRenderer) {
                    binding.remotePeerView.renderPeerVideo(peer, peerVideoContentType.videoMediaLabel)
                }

                override fun onViewBecomeInvisibleOnScreen(videoRenderer: VideoRenderer) {
                    binding.remotePeerView.stopRenderingPeerVideo(
                        peer,
                        peerVideoContentType.videoMediaLabel
                    )
                }
            })
    }

    fun unbind() {
        binding.remotePeerView.release()
    }
}