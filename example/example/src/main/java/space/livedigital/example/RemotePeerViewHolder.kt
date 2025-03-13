package space.livedigital.example

import android.annotation.SuppressLint
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import space.livedigital.example.databinding.ItemRemotePeerBinding
import space.livedigital.sdk.entities.MediaLabel
import space.livedigital.sdk.entities.Peer

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

        if (isConsumingVideo) {
            binding.remotePeerView.renderPeerVideo(peer, MediaLabel.CAMERA)
        } else {
            binding.remotePeerView.stopRenderingPeerVideo(peer, MediaLabel.CAMERA)
        }
    }
}