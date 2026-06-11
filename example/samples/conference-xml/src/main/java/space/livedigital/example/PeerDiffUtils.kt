package space.livedigital.example

import androidx.recyclerview.widget.DiffUtil

class PeerDiffUtils(
    private val oldList: List<PeerWithMediaContentType>,
    private val newList: List<PeerWithMediaContentType>
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
        return oldList[oldItemPosition].peerWithUpdateTime.peer.id ==
                newList[newItemPosition].peerWithUpdateTime.peer.id
    }

    override fun areContentsTheSame(
        oldItemPosition: Int,
        newItemPosition: Int
    ): Boolean {
        val oldPeerUpdateTime = oldList[oldItemPosition].peerWithUpdateTime.updateTimeMark
        val newPeerUpdateTime = newList[newItemPosition].peerWithUpdateTime.updateTimeMark

        return oldPeerUpdateTime == newPeerUpdateTime
    }
}
