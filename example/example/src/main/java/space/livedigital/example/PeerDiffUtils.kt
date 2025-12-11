package space.livedigital.example

import androidx.recyclerview.widget.DiffUtil

class PeerDiffUtils(
    private val oldList: List<PeerWithUpdateTime>,
    private val newList: List<PeerWithUpdateTime>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(
        oldItemPosition: Int,
        newItemPosition: Int
    ): Boolean {
        return oldList[oldItemPosition].peer.id == newList[newItemPosition].peer.id
    }

    override fun areContentsTheSame(
        oldItemPosition: Int,
        newItemPosition: Int
    ): Boolean {
        val oldPeerUpdateTime = oldList[oldItemPosition].updateTimeMark
        val newPeerUpdateTime = newList[newItemPosition].updateTimeMark

        return oldPeerUpdateTime == newPeerUpdateTime
    }
}
