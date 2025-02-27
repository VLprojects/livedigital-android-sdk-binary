package space.livedigital.example

import android.annotation.SuppressLint
import android.view.View
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import space.livedigital.example.databinding.ItemRemotePeerBinding
import space.livedigital.sdk.entities.MediaLabel
import space.livedigital.sdk.entities.Peer
import space.livedigital.sdk.view.PeerView
import space.livedigital.sdk.view.VideoRenderer

internal class RemotePeerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val binding = ItemRemotePeerBinding.bind(itemView)

    @SuppressLint("SetTextI18n")
    fun bind(peer: Peer) {
        val isConsumingAudio = peer.isConsumingAudio(MediaLabel.MICROPHONE)
        val isConsumingVideo = peer.isConsumingVideo(MediaLabel.CAMERA)

        binding.peerIdText.text = peer.id.value
        binding.peerNameText.text = peer.appData.optString("name")

        binding.audioStatusText.text = "audio is ${if (isConsumingAudio) "on" else "off"}"
        binding.videoStatusText.text = "video is ${if (isConsumingVideo) "on" else "off"}"

        binding.remotePeerView.isInvisible = isConsumingVideo.not()
        binding.remotePeerView.renderPeerVideo(peer, MediaLabel.CAMERA)

        binding.remotePeerView.setViewScreenVisibilityListener(
            object : PeerView.ViewScreenVisibilityListener {
                override fun onViewBecomeVisibleOnScreen(videoRenderer: VideoRenderer) {
                    binding.remotePeerView.renderPeerVideo(peer, MediaLabel.CAMERA)
                }

                override fun onViewBecomeInvisibleOnScreen(videoRenderer: VideoRenderer) {
                    binding.remotePeerView.stopRenderingPeerVideo(peer, MediaLabel.CAMERA)
                }
            })
    }

    fun unbind() {
        binding.remotePeerView.release()
    }
}