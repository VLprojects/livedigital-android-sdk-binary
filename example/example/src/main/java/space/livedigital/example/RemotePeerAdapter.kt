package space.livedigital.example

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import space.livedigital.sdk.entities.Peer
import space.livedigital.sdk.entities.PeerId

internal class RemotePeerAdapter(
    private val layoutInflater: LayoutInflater
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val list = mutableListOf<Peer>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return RemotePeerViewHolder(
            layoutInflater.inflate(
                R.layout.item_remote_peer,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as? RemotePeerViewHolder)?.bind(list[position])
    }

    override fun getItemCount() = list.size

    fun addPeer(peer: Peer) {
        list.add(peer)
        notifyItemInserted(list.size - 1)
    }

    fun removePeer(peerId: PeerId) {
        val peer = list.find { it.id == peerId } ?: return
        val index = list.indexOf(peer)
        list.removeAt(index)
        notifyItemRemoved(index)
    }

    fun updatePeer(peer: Peer) {
        val index = list.indexOfFirst { it.id == peer.id }
        if (index == -1) {
            addPeer(peer)
            return
        }

        list[index] = peer
        notifyItemChanged(index, index)
    }

    fun clear() {
        list.clear()
        notifyDataSetChanged()
    }
}